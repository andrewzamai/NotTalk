package it.unipd.dei.esp2021.nottalk

import android.util.Log
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
    val userListLiveData = notTalkRepository.getAllUsers() // LiveData list of all users

    // call insertUser function on NotTalkRepository reference,
    // which not only updates the list in ViewModel but adds it directly in the database
    // so changes will also be then notified userListLiveData list of user
    fun insertUser(user: User) {
        notTalkRepository.insertUser(user)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "UserListViewModel about to be destroyed")
    }
}