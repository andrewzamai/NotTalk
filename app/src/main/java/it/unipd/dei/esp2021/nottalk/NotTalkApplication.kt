package it.unipd.dei.esp2021.nottalk

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import it.unipd.dei.esp2021.nottalk.util.AppNotificationManager

class NotTalkApplication : Application() {

    // on application creation initializes (only one time) NotTalkRepository
    // then singleton pattern in NotTalkRepository will provide the instance when asked via get function
    override fun onCreate() {
        super.onCreate()

        NotTalkRepository.initialize(this)
        AppNotificationManager.initialize(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "DefaultNotificationChannel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("notTalk", name, importance)
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val sharedPref = getSharedPreferences("notTalkPref", MODE_PRIVATE)

        if(sharedPref.getString("thisUsername","")!=""){
            val intent = Intent(this,ItemDetailHostActivity::class.java)
            intent.flags=(FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }else{
            val intent = Intent(this,LoginActivity::class.java)
            intent.flags=(FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}