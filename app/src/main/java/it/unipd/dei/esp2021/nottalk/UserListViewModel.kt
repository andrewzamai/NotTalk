package it.unipd.dei.esp2021.nottalk

import android.util.Log
import androidx.lifecycle.ViewModel
import it.unipd.dei.esp2021.nottalk.database.ChatDatabase

private const val TAG = "UserListViewModel"

class UserListViewModel : ViewModel() {

    init{
        Log.d(TAG, "UserListViewModel instance created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "UserListViewModel about to be destroyed")
    }
}