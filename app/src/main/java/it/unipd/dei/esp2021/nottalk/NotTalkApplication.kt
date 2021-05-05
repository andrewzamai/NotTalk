package it.unipd.dei.esp2021.nottalk

import android.app.Application

class NotTalkApplication : Application() {

    // on application creation initializes (only one time) NotTalkRepository
    // then singleton pattern in NotTalkRepository will provide the instance when asked via get function
    override fun onCreate() {
        super.onCreate()
        NotTalkRepository.initialize(this)
    }
}