package com.gustafah.android.mockinterceptor.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.gustafah.android.mockinterceptor.MockConfig
import com.gustafah.android.mockinterceptor.MockUtils

class DefaultMockNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        println("autoMock: ${MockUtils.autoMock}")
        MockUtils.autoMock = !MockUtils.autoMock
        context?.let {
            with(NotificationManagerCompat.from(it)) {
                this.cancel(MockNotification.NOTIFICATION_ID)
            }
            MockNotification.showMockNotification(it)
        }
    }
}
