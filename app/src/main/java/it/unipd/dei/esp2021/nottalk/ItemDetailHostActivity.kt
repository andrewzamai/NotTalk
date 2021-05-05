package it.unipd.dei.esp2021.nottalk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import it.unipd.dei.esp2021.nottalk.databinding.ActivityItemDetailBinding

/**
 * This activity will hosts the two fragments (ItemListFragment/ItemDetailFragment):
 * simultaneously or separately depending on device size.
 */
class ItemDetailHostActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View binding Android Jetpack feature
        // binding will get a reference to the layout, using dot notation is possible to get a reference to all contained ID widgets
        val binding = ActivityItemDetailBinding.inflate(layoutInflater) //ActivityItemDetailBinding is the binding class generated for activity_item_detail.xml layout, .inflate(layoutInflater) does the inflate as setContentView(R.layout.activity_item_detail)
        val view = binding.root //get a reference to the root view
        setContentView(view)  //and make it active on the screen

        // initialize a navigation host fragment by retrieving Fragment Container View ID declared in hosting activity xml file activity_item_detail.xml
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_item_detail) as NavHostFragment
        // initialize a navigation controller in the hosting activity
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_item_detail)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}