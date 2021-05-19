package it.unipd.dei.esp2021.nottalk

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import it.unipd.dei.esp2021.nottalk.database.FileManager
import it.unipd.dei.esp2021.nottalk.database.User
import it.unipd.dei.esp2021.nottalk.databinding.ActivityItemDetailBinding
import it.unipd.dei.esp2021.nottalk.remote.ServerAdapter
import it.unipd.dei.esp2021.nottalk.remote.SyncService
import java.util.concurrent.Executors

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // saves in sharedPreferences this user's username
        // TODO: change from hardcoded username admin in username specified from user when registering
        sharedPref = getSharedPreferences("notTalkPref", MODE_PRIVATE)
        with (sharedPref.edit()) {
            if (sharedPref.getString("thisUsername", null) == null) {
                putString("thisUsername", "")
                putString("uuid", "")
                commit()
            }
        }
        /*
        putString("thisUsername", "admin")
        putString("uuid", "331e698e-b33e-11eb-8632-6224d93e4c38")
        */
        //}.apply()

        if(sharedPref.getString("thisUsername","")!="") {
            applicationContext.startService(Intent(this, SyncService::class.java))
        } else{
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("requestCode",REQUEST_LOGIN)
            intent.putExtra("option","MANDATORY")
            startActivityForResult(intent, REQUEST_LOGIN)
        }

        // View binding Android Jetpack feature
        // binding will get a reference to the layout, using dot notation is possible to get a reference to all contained ID widgets
        val binding = ActivityItemDetailBinding.inflate(layoutInflater) //ActivityItemDetailBinding is the binding class generated for activity_item_detail.xml layout, .inflate(layoutInflater) does the inflate as setContentView(R.layout.activity_item_detail)
        val view = binding.root //get a reference to the root view
        setContentView(view)  //and make it active on the screen



        // initialize a navigation host fragment by retrieving Fragment Container View ID declared in hosting activity xml file activity_item_detail.xml
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_item_detail) as NavHostFragment
        // initialize a navigation controller in the hosting activity
        val navController = navHostFragment.navController

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
                R.id.logout_item -> {
                    applicationContext.stopService(Intent(this, SyncService::class.java))
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("option","MANDATORY")
                    intent.putExtra("requestCode",REQUEST_CREATE)
                    val sp1 = getSharedPreferences("notTalkPref", MODE_PRIVATE)
                    val ep = sp1.edit()
                    ep.putString("thisUsername", "")
                    ep.putString("uuid", "")
                    ep.commit()
                    startActivityForResult(intent, REQUEST_LOGIN)
                    true
                }
                R.id.delete_item -> {
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
                                if(repo.checkUser(username)){
                                    repo.deleteUser(username)
                                }
                                repo.deleteByUserTo(username)
                                val ep = sp1.edit()
                                ep.putString("thisUsername", "")
                                ep.putString("uuid", "")
                                ep.commit()
                                mainExecutor.execute {
                                    Toast.makeText(applicationContext,"User deleted successfully",Toast.LENGTH_LONG).show()
                                }
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.putExtra("requestCode",REQUEST_LOGIN)
                                intent.putExtra("option","MANDATORY")
                                startActivityForResult(intent, REQUEST_LOGIN)
                            }
                            else{
                                mainExecutor.execute {
                                    Toast.makeText(applicationContext,"Failed to delete user",Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    })
                    alert.setNegativeButton("No",
                        DialogInterface.OnClickListener { dialog, whichButton -> })
                    alert.show()
                    true
                }
                else -> false
            }
        }

        // start syncService
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
        else if (resultCode == Activity.RESULT_CANCELED && (requestCode == REQUEST_LOGIN || requestCode == REQUEST_CREATE)){
            finish()
        }
    }

    // helper methods

    fun setToolBarTitle(title: String) {
        toolbar.title = title
    }

}