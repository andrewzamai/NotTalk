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

        // retrieves username from intent's data
        val otherUsername = intent.data?.lastPathSegment

        Log.d("BUBBLE_TAG", "onCreateBubbleActivity $otherUsername")

        if (otherUsername != null) {
            Log.d("BUBBLE_TAG", "Instantiating fragment with otherUsername -> $otherUsername")

            val bundle = Bundle()
            bundle.putString(
                ItemDetailFragment.ARG_ITEM_ID,
                otherUsername
            )
            // false tells the fragment that it is in BubbleActivity Fragment container and not in ItemDetailHostActivity (the first one does not have a toolbar)
            bundle.putBoolean(
                ItemDetailFragment.ARG_ITEM_IS_IN_HOST_ACTIVITY,
                false
            )
            val fragment = ItemDetailFragment()
            fragment.arguments = bundle

            if (savedInstanceState == null) {
                supportFragmentManager.commitNow {
                    replace(R.id.fragment_container_bubble_activity, fragment)
                }
            }

        }

    }

}
