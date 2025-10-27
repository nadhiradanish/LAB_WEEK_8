package com.example.lab_week_8

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationService : Service() {

    // In order to make the required notification, a service is required
    // to do the job for us in the foreground process
    // Create the notification builder that'll be called later on
    private lateinit var notificationBuilder: NotificationCompat.Builder

    // Create a system handler which controls what thread the process is being executed on
    private lateinit var serviceHandler: Handler

    // This is used to bind a two-way communication
    // In this tutorial, we will only be using a one-way communication
    // therefore, the return can be set to null
    override fun onBind(intent: Intent): IBinder? = null

    // this is a callback and part of the life cycle
    // the onCreate callback will be called when this service is created for the first time
    override fun onCreate() {
        super.onCreate()
        // Create the notification with all of its contents and configurations
        // in the startForegroundService() custom function
        notificationBuilder = startForegroundService()

        // Create the handler to control which thread the notification will be executed on.
        // 'HandlerThread' provides the different thread for the process to be executed on,
        // while on the other hand, 'Handler' enqueues the process to HandlerThread to be executed.
        val handlerThread = HandlerThread("SecondThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    // Create the notification with all of its contents and configurations all set up
    private fun startForegroundService(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val notificationBuilder = getNotificationBuilder(pendingIntent, channelId)

        // Start the foreground service
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    // A pending Intent is the Intent used to be executed when the user clicks the notification
    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            flag
        )
    }

    // To make a notification, a channel is required to set up the required configurations
    private fun createNotificationChannel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "001"
            val channelName = "001 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, channelPriority)

            val service = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )
            service.createNotificationChannel(channel)
            channelId
        } else {
            ""
        }

    // Build the notification with all of its contents and configurations
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second worker process is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Second worker process is done, check it out!")
            .setOngoing(true)

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        serviceHandler.post {
            countDownFromTenToZero(notificationBuilder)
            notifyCompletion(Id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return returnValue
    }

    // A function to update the notification to display a count down from 10 to 0
    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    // Update the LiveData with the returned channel id through the Main Thread
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }
}
