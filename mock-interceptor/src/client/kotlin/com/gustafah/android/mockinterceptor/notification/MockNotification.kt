package com.gustafah.android.mockinterceptor.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.gustafah.android.mockinterceptor.MockUtils
import com.gustafah.android.mockinterceptor.R

object MockNotification {

    private const val NOTIFICATION_CHANNEL_ID = "Channel_ID"
    private const val NOTIFICATION_CHANNEL_NAME = "Channel_NAME"
    const val NOTIFICATION_ID = 1

    @RequiresApi(Build.VERSION_CODES.M)
    fun showMockNotification(context: Context) {
        val intent = Intent(context, DefaultMockNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val collapsedNotification = RemoteViews(
            context.packageName,
            R.layout.mock_notification_option
        )
        collapsedNotification.setOnClickPendingIntent(
            R.id.switchDefaultMock,
            pendingIntent
        )

        val bodyMessage: String
        val buttonText: String
        val buttonTextColor: Int
        val notificationIcon: Int

        if (MockUtils.autoMock) {
            bodyMessage = context.getString(R.string.mock_notification_default_message)
            buttonText = context.getString(R.string.mock_notification_button_disable_mock)
            buttonTextColor = context.getColor(R.color.mock_disable_text_color)
            notificationIcon = R.drawable.ic_rule_black_24dp
        } else {
            bodyMessage = context.getString(R.string.mock_notification_select_message)
            buttonText = context.getString(R.string.mock_notification_button_enable_mock)
            buttonTextColor = context.getColor(R.color.mock_enable_text_color)
            notificationIcon = R.drawable.ic_rule_black_24dp
        }

        collapsedNotification.setTextViewText(R.id.switchDefaultMock, buttonText)
        collapsedNotification.setTextColor(R.id.switchDefaultMock, buttonTextColor)
        collapsedNotification.setTextViewText(R.id.bodyMessage, bodyMessage)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notifyChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(notifyChannel)
        }

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(notificationIcon)
            .setContent(collapsedNotification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}