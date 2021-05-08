package it.unipd.dei.esp2021.nottalk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.Serializable

class ChatViewModelFactory(private val thisUser: String, private val otherUser: String) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ChatViewModel(thisUser, otherUser) as T
    }
}