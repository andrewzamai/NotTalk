package it.unipd.dei.esp2021.nottalk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import it.unipd.dei.esp2021.nottalk.database.FileManager
import it.unipd.dei.esp2021.nottalk.databinding.ActivityItemDetailBinding
import it.unipd.dei.esp2021.nottalk.remote.SyncService

/**
 * This activity will hosts the two fragments (ItemListFragment/ItemDetailFragment):
 * simultaneously or separately depending on device size.
 */
class ItemDetailHostActivity : AppCompatActivity(){

    companion object{
        val REQUEST_LOGIN = 10
        val REQUEST_CREATE = 11
    }

    // reference to sharedPreferences
    private lateinit var sharedPref: SharedPreferences

    // toolbar
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    // navController
    private lateinit var navController: NavController

    // hashMap containing "username-draftMessage" elements to not lose editText content through configuration changes, activity stop and fragment navigation
    private var userMessagesMap: HashMap<String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View binding Android Jetpack feature
        // binding will get a reference to the layout, using dot notation is possible to get a reference to all contained ID widgets
        val binding = ActivityItemDetailBinding.inflate(layoutInflater) //ActivityItemDetailBinding is the binding class generated for activity_item_detail.xml layout, .inflate(layoutInflater) does the inflate as setContentView(R.layout.activity_item_detail)
        val view = binding.root //get a reference to the root view
        setContentView(view)  //and make it active on the screen

        // saves in sharedPreferences this user's username
        // TODO: change from hardcoded username admin in username specified from user when registering
        sharedPref = getSharedPreferences("notTalkPref", MODE_PRIVATE)
        with (sharedPref.edit()) {
            if (sharedPref.getString("thisUsername", "") == "") {
                putString("thisUsername", "admin")
                putString("uuid", "331e698e-b33e-11eb-8632-6224d93e4c38")
                apply()
            }
        }
            /*
            putString("thisUsername", "admin")
            putString("uuid", "331e698e-b33e-11eb-8632-6224d93e4c38")
            */
        //}.apply()

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
        toolbar.inflateMenu(R.menu.toolbar_menu)
        toolbar.title = getString(R.string.toolbar_chatLists)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.login_item -> {
                    applicationContext.stopService(Intent(this, SyncService::class.java))
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("requestCode",REQUEST_LOGIN)
                    startActivityForResult(intent, REQUEST_LOGIN)
                    true
                }
                R.id.create_item -> {
                    applicationContext.stopService(Intent(this, SyncService::class.java))
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("requestCode",REQUEST_CREATE)
                    startActivityForResult(intent, REQUEST_CREATE)
                    true
                }
                else -> false
            }
        }

        // to open ItemDetailFragment chat of user from notification
        intent?.let(::handleIntent)

        // start syncService
        applicationContext.startService(Intent(this, SyncService::class.java))
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && (requestCode == REQUEST_LOGIN || requestCode == REQUEST_CREATE)){
            val username = data?.getStringExtra("username")
            val uuid = data?.getStringExtra("uuid")
            sharedPref = getSharedPreferences("notTalkPref", MODE_PRIVATE)
            with (sharedPref.edit()){
                putString("thisUsername", username)
                putString("uuid", uuid)
            }.commit()
            applicationContext.startService(Intent(this, SyncService::class.java))
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!userMessagesMap.isNullOrEmpty()) {
            outState.putSerializable("userMessagesMap", userMessagesMap)
        }
    }





/*----------------------------------------------- HELPER FUNCTIONS -----------------------------------------------------------*/



    fun setToolBarTitle(title: String) {
        toolbar.title = title
    }

    fun getMessageDraft(username: String): String? {
        return userMessagesMap?.get(username)
    }

    fun saveMessageDraft(username: String, textMessage: String) {
        if (userMessagesMap == null) {
            userMessagesMap = HashMap()
        }
        userMessagesMap!!.put(username, textMessage)
    }


    private fun handleIntent(intent: Intent) {
        when (intent.action) {

            Intent.ACTION_VIEW -> {
                val username = intent.data?.lastPathSegment
                if (username != null) {
                    Log.d("Activity started by intent", username)
                    val bundle = Bundle()
                    bundle.putString(
                        ItemDetailFragment.ARG_ITEM_ID,
                        username
                    )
                    navController.navigate(R.id.show_item_detail, bundle)
                }
            }
            /*
            // Invoked when a text is shared through Direct Share.
            Intent.ACTION_SEND -> {
                val shortcutId = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                val contact = Contact.CONTACTS.find { it.shortcutId == shortcutId }
                if (contact != null) {
                    openChat(contact.id, text)
                }
            }
            */
        }
    }

}