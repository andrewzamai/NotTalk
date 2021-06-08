package it.unipd.dei.esp2021.nottalk

import android.util.Log
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import it.unipd.dei.esp2021.nottalk.database.ChatDatabase
import it.unipd.dei.esp2021.nottalk.database.User

private const val TAG = "UserListViewModel"

/* A ViewModel survives configuration changes as rotation and is destroyed only when its associated activity/fragment is finished
 * Does not survive at OS stops!
 * Using a ViewModel also enables to let the activity to be responsible for only handling what appears on screen, not the logic behind determining the data to display.
 * Instead of retrieving each time the list of users, retrieve it only one time and store it in this ViewModel. LiveData will notify any observer of changes.
 */
class UserListViewModel : ViewModel() {

    init{
        Log.d(TAG, "UserListViewModel instance created")
    }

    private val notTalkRepository = NotTalkRepository.get() // reference to NotTalkRepository instance
    val userListLiveData = Transformations.switchMap(ItemDetailHostActivity.currentUsername) { param->
        notTalkRepository.getAllUsers(param)
    } // LiveData list of all users


    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "UserListViewModel about to be destroyed")
    }
}