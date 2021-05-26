package it.unipd.dei.esp2021.nottalk

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow

class BubbleActivity : AppCompatActivity(R.layout.bubble_activity), NavigationController {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.data?.lastPathSegment?.toLongOrNull() ?: return
        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, ChatFragment.newInstance(id, false))
            }
        }
    }

    override fun openChat(id: Long, prepopulateText: String?) {
        throw UnsupportedOperationException("BubbleActivity always shows a single chat thread.")
    }

    override fun openPhoto(photo: Uri) {
        // In an expanded Bubble, you can navigate between Fragments just like you would normally
        // do in a normal Activity. Just make sure you don't block onBackPressed().
        supportFragmentManager.commit {
            addToBackStack(null)
            replace(R.id.container, PhotoFragment.newInstance(photo))
        }
    }

    override fun updateAppBar(
        showContact: Boolean,
        hidden: Boolean,
        body: (name: TextView, icon: ImageView) -> Unit
    ) {
        // The expanded bubble does not have an app bar. Ignore.
    }
}
