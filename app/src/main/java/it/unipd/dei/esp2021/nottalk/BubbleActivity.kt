package it.unipd.dei.esp2021.nottalk

import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import it.unipd.dei.esp2021.nottalk.databinding.ActivityItemDetailBinding
import it.unipd.dei.esp2021.nottalk.databinding.BubbleActivityBinding

class BubbleActivity : AppCompatActivity(R.layout.bubble_activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val otherUsername = intent.data?.lastPathSegment

        Log.d("BUBBLE_TAG", "onCreateBubbleActivity $otherUsername")

        /*
        val binding = BubbleActivityBinding.inflate(layoutInflater) //ActivityItemDetailBinding is the binding class generated for activity_item_detail.xml layout, .inflate(layoutInflater) does the inflate as setContentView(R.layout.activity_item_detail)
        val view = binding.root //get a reference to the root view
        setContentView(view)  //and make it active on the screen
        */

        // initialize a navigation host fragment by retrieving Fragment Container View ID declared in hosting activity xml file activity_item_detail.xml
        //val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_bubble_activity) as NavHostFragment
        // initialize a navigation controller in the hosting activity
        // navController = navHostFragment.navController

        if (otherUsername != null) {
            Log.d("BUBBLE_TAG", "Instantiating fragment with otherUsername -> $otherUsername")

            val bundle = Bundle()
            bundle.putString(
                ItemDetailFragment.ARG_ITEM_ID,
                otherUsername
            )
            bundle.putBoolean(
                ItemDetailFragment.ARG_ITEM_IS_IN_HOST_ACTIVITY,
                false
            )
            val fragment = ItemDetailFragment()
            fragment.arguments = bundle

            /*
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_bubble_activity)

            if (currentFragment == null) {
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container_bubble_activity, fragment)
                    .commit()
            }
            */

            if (savedInstanceState == null) {
                supportFragmentManager.commitNow {
                    replace(R.id.fragment_container_bubble_activity, fragment)
                }
            }


            //navController.navigate(R.id.show_item_detail, bundle)
            /*
            supportFragmentManager.commitNow {
                val fragment = ItemDetailFragment()
                fragment.arguments = bundle
                replace(R.id.fragment_container_bubble_activity, fragment)
            }

             */

        }

        /*

       if (savedInstanceState == null)
       {
            supportFragmentManager.commitNow
            {
                replace(R.id.container, ItemDetailFragment.newInstanceText(id, false))
            }
        }


         */

    }

/*
    override fun openChat(id: Int, prepopulateText: String?) {
        throw UnsupportedOperationException("BubbleActivity always shows a single chat thread.")
    }



    override fun openPhoto(photo: Uri) {
        supportFragmentManager.commit {
            addToBackStack(null)
            replace(R.id.container, ItemDetailFragment.newInstancePhoto(photo))
        }
    }


    override fun updateAppBar(
        showContact: Boolean,
        hidden: Boolean,
        body: (name: TextView, icon: ImageView) -> Unit
    ) { }


 */
}
