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
import it.unipd.dei.esp2021.nottalk.databinding.ActivityItemDetailBinding
import it.unipd.dei.esp2021.nottalk.databinding.BubbleActivityBinding

class BubbleActivity : AppCompatActivity(R.layout.bubble_activity), NavigationController {

    // navController
   // private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("BUBBLE_TAG", "onCreateBubbleActivity")

        /*
        val id = intent.getIntExtra("chatId",0)


         */


        val binding = BubbleActivityBinding.inflate(layoutInflater) //ActivityItemDetailBinding is the binding class generated for activity_item_detail.xml layout, .inflate(layoutInflater) does the inflate as setContentView(R.layout.activity_item_detail)
        val view = binding.root //get a reference to the root view
        setContentView(view)  //and make it active on the screen

        /*

       if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
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
