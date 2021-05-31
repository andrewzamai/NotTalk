package it.unipd.dei.esp2021.nottalk

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import it.unipd.dei.esp2021.nottalk.database.Message
import it.unipd.dei.esp2021.nottalk.util.AppNotificationManager

class ChatViewModel(thisUser: String, otherUser: String) : ViewModel() {

    private val thisUser: String = thisUser
    private val otherUser: String = otherUser

    val notTalkRepository = NotTalkRepository.get() // reference to NotTalkRepository instance
    val chatListLiveData: LiveData<List<Message>> = notTalkRepository.getConvo(thisUser, otherUser)

    init{
        Log.d("ChatViewModel", "ChatViewModel instantiated with thisUser=${thisUser} and otherUser=${otherUser}")
    }

}