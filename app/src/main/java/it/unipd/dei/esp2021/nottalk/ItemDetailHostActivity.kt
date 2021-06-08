package it.unipd.dei.esp2021.nottalk

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import it.unipd.dei.esp2021.nottalk.databinding.ActivityItemDetailBinding
import it.unipd.dei.esp2021.nottalk.remote.ServerAdapter
import it.unipd.dei.esp2021.nottalk.remote.SyncService
import it.unipd.dei.esp2021.nottalk.util.AppNotificationManager
import it.unipd.dei.esp2021.nottalk.util.PlayerService
import java.util.concurrent.Executors
import kotlin.system.exitProcess


/**
 * This activity will hosts the two fragments (ItemListFragment/ItemDetailFragment):
 * simultaneously or separately depending on device size.
 */
class ItemDetailHostActivity : AppCompatActivity(){

    companion object{
        const val REQUEST_LOGIN = 10
        const val REQUEST_MUST_LOGIN = 11
        const val SERVICE_STOP = 20
        const val REQUEST_PERMISSION = 21

        val currentUsername = MutableLiveData<String>()
    }


    // reference to sharedPreferences
    private lateinit var sharedPref: SharedPreferences

    // toolbar
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    // userIcon in toolbar
    private lateinit var userIcon: ImageView

    // navController
    private lateinit var navController: NavController

    // hashMap containing "username-draftMessage" elements to not lose editText content through configuration changes, activity stop and fragment navigation
    private var userMessagesMap: HashMap<String, String>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View binding Android Jetpack feature: binding will get a reference to the layout, using dot notation is possible to get a reference to all contained ID widgets
        val binding = ActivityItemDetailBinding.inflate(layoutInflater) //ActivityItemDetailBinding is the binding class generated for activity_item_detail.xml layout, .inflate(layoutInflater) does the inflate as setContentView(R.layout.activity_item_detail)
        userIcon = binding.userIcon
        val view = binding.root //get a reference to the root view
        setContentView(view)  //and make it active on the screen

        // saves in sharedPreferences this user username
        sharedPref = getSharedPreferences("notTalkPref", MODE_PRIVATE)
        if (sharedPref.getString("thisUsername", "") == "") {
            applicationContext.stopService(Intent(this, SyncService::class.java))
        }
        when {
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                checkAuth()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                requestSettings()
            } else -> {
                requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }


        // when re-creating the activity after destroy the useMessageMap is retrieved from savedInstanceState bundle
        userMessagesMap = savedInstanceState?.getSerializable("userMessagesMap") as? HashMap<String, String>


        // initialize a navigation host fragment by retrieving Fragment Container View ID declared in hosting activity xml file activity_item_detail.xml
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_item_detail) as NavHostFragment
        // initialize a navigation controller in the hosting activity
        navController = navHostFragment.navController

        // set up ToolBar, use toolbar instead of action bar (action bar will display difference behaviours depending on device)
        // toolbar does not need overriding onNavigateUp method
        toolbar = binding.toolbar // gets reference
        val appBarConfiguration: AppBarConfiguration = AppBarConfiguration(navController.graph)
        toolbar.setupWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if(destination.id == R.id.fragment_item_detail || destination.id == R.id.item_detail_fragment){
                //clear the user menu if not in the correct fragment
                toolbar.menu.clear()
            }
            else{
                //inflate and update the user menu
                onCreateOptionsMenu(toolbar.menu)
                userIcon.setBackgroundColor(0x00000000)
                val userItem = toolbar.menu.findItem(R.id.user_item)
                userItem.title= currentUsername.value
                userItem.icon.setTint(getColor(R.color.NT_purple2))
                currentUsername.observe(this){
                    userItem.title= it
                }
            }
        }
        toolbar.title = getString(R.string.toolbar_chatLists)
        /**
         *  Sets a listener for menu items:
         *  Login/Register: starts LoginActivity with back press available
         *  Logout: starts LoginActivity without coming back
         *  Delete: user removes all data in database relative to current user and perform logout
         */
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.login_item -> {
                    applicationContext.stopService(Intent(this, SyncService::class.java))
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("requestCode",REQUEST_LOGIN)
                    startActivityForResult(intent, REQUEST_LOGIN)
                    true
                }
                R.id.delete_item -> {
                    applicationContext.stopService(Intent(this, SyncService::class.java))
                    val backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
                    val sp1 = getSharedPreferences("notTalkPref", MODE_PRIVATE)
                    val username = sp1.getString("thisUsername", "")
                    val uuid = sp1.getString("uuid", "")
                    val alert = AlertDialog.Builder(this)
                    alert.setTitle("Delete user")
                    alert.setMessage("Are you sure to delete $username")
                    alert.setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, whichButton ->
                        // Do something with value!
                        backgroundExecutor.execute{
                            val repo = NotTalkRepository.get()
                            val sa = ServerAdapter()
                            val result = sa.deleteUser(username!!,uuid!!)
                            if(result=="ok") {
                                applicationContext.stopService(Intent(this, SyncService::class.java))
                                repo.deleteUser(username)
                                repo.deleteByUserTo(username)
                                repo.deleteByUserFrom(username)
                                repo.deleteRelationsByOtherUser(username)
                                repo.deleteRelationsByThisUser(username)
                                val ep = sp1.edit()
                                ep.putString("thisUsername", "")
                                ep.putString("uuid", "")
                                ep.commit()
                                mainExecutor.execute {
                                    Toast.makeText(applicationContext,"User deleted successfully",Toast.LENGTH_LONG).show()
                                }
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.putExtra("requestCode", REQUEST_MUST_LOGIN)
                                startActivityForResult(intent, REQUEST_MUST_LOGIN)
                                true
                            }
                            else{
                                mainExecutor.execute {
                                    Toast.makeText(applicationContext,"Failed to delete user",Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    })
                    alert.setNegativeButton("No") { _, _ -> }
                    alert.show()
                    true
                }
                R.id.logout_item -> {
                    applicationContext.stopService(Intent(this, SyncService::class.java))
                    val sp1 = getSharedPreferences("notTalkPref", MODE_PRIVATE)
                    val ep = sp1.edit()
                    ep.putString("thisUsername", "")
                    ep.putString("uuid", "")
                    ep.commit()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("requestCode", REQUEST_MUST_LOGIN)
                    startActivityForResult(intent, REQUEST_MUST_LOGIN)
                    true
                }
                else -> false
            }
        }

        // to open ItemDetailFragment chat of user from notification
        intent?.let(::handleIntent)

    }

    //Solution to add icon and group to toolbar menu
    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
            menu.isGroupDividerEnabled=true
            menu.setGroupEnabled(R.id.group1,true)
        }
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && (requestCode == REQUEST_LOGIN || requestCode == REQUEST_MUST_LOGIN)){
            val username = data?.getStringExtra("username")
            val uuid = data?.getStringExtra("uuid")
            currentUsername.value=username!!
            sharedPref = getSharedPreferences("notTalkPref", MODE_PRIVATE)
            with (sharedPref.edit()){
                putString("thisUsername", username)
                putString("uuid", uuid)
            }.commit()
            val userItem = toolbar.menu.findItem(R.id.user_item)
            userItem.title= currentUsername.value
            applicationContext.startForegroundService(Intent(this, SyncService::class.java))
        }
        if(resultCode == Activity.RESULT_CANCELED && requestCode == REQUEST_MUST_LOGIN){
            finish()
        }
        if(requestCode == REQUEST_PERMISSION){
            if(ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED) {
                checkAuth()
            } else{
                requestSettings()
            }
        }
    }

    // saves userMessagesMap of draft messages
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!userMessagesMap.isNullOrEmpty()) {
            outState.putSerializable("userMessagesMap", userMessagesMap)
        }
    }

    //To handle an intent received with the activity already open
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if(intent.action == SyncService.STOP_SERVICE) {
            //Stops the application and service if the foreground notification is closed
            applicationContext.stopService(Intent(this, SyncService::class.java))
            finishAndRemoveTask()
            exitProcess(0)
        }
    }




/*----------------------------------------------- HELPER FUNCTIONS -----------------------------------------------------------*/

    private fun checkAuth() {
        if (sharedPref.getString("thisUsername", "") == "") {
            applicationContext.stopService(Intent(this, SyncService::class.java))
            Toast.makeText(applicationContext, R.string.ServiceRequestLogin, Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("requestCode", REQUEST_MUST_LOGIN)
            startActivityForResult(intent, REQUEST_MUST_LOGIN)
        }
        else{
            currentUsername.value= sharedPref.getString("thisUsername","")
            applicationContext.startForegroundService(Intent(this, SyncService::class.java))
        }
    }

    private fun requestPermission(permission: String){
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                checkAuth()
            } else {
                requestSettings()
            }
        }.launch(permission)
    }

    private fun requestSettings(){
        AlertDialog.Builder(this)
            .setTitle("Attention")
            .setMessage("Storage permission is needed, please allow it in the settings")
            .setPositiveButton("Ok") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:" + applicationContext.packageName)
                startActivityForResult(intent,REQUEST_PERMISSION)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finishAndRemoveTask()
                exitProcess(0)
            }.show()
    }

    // to set toolbar title
    fun setToolBarTitle(title: String) {
        toolbar.title = title
    }

    // to set userIcon in toolbar
    fun setUserIconToolBar(icon: Bitmap?) {
        if (icon == null) {
            userIcon.setImageDrawable(getDrawable(R.drawable.default_user_icon))
        } else {
            userIcon.setImageBitmap(icon)
        }
    }

    // get message draft given username
    fun getMessageDraft(username: String): String? {
        return userMessagesMap?.get(username)
    }

    // save message draft in userMessagesMap
    fun saveMessageDraft(username: String, textMessage: String) {
        if (userMessagesMap == null) {
            userMessagesMap = HashMap()
        }
        userMessagesMap!!.put(username, textMessage)
    }

    // starts PlayerService given audio message sender and uriString of the content to play
    fun startPlayerService(uriString: String, username: String) {
        val intent = Intent(this, PlayerService::class.java)
        intent.putExtra(PlayerService.PLAYER_START, true)
        intent.putExtra(PlayerService.URI_PATH, uriString)
        intent.putExtra(PlayerService.USERNAME, username)
        applicationContext.startService(intent)
    }

    // navigates to correct username chat fragment when opening activity from notification
    private fun handleIntent(intent: Intent) {
        when (intent.action) {

            Intent.ACTION_VIEW -> {
                val username = intent.data?.lastPathSegment
                if (username != null) {
                    Log.d("ItemDetailHostActivity started from notification intent", username)
                    val bundle = Bundle()
                    bundle.putString(
                        ItemDetailFragment.ARG_ITEM_ID,
                        username
                    )
                    navController.navigate(R.id.show_item_detail, bundle)

                }


            }
            //If close button is pressed on foreground notification
            SyncService.STOP_SERVICE -> {
                //Stops the application and service
                applicationContext.stopService(Intent(this, SyncService::class.java))
                finishAndRemoveTask()
                exitProcess(0)
            }

        }
    }

}