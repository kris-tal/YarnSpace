package com.yarnspace.app.feature.notifs.notification

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yarnspace.app.MainActivity
import com.yarnspace.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotifsNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @SuppressLint("MissingPermission")
    fun showUnreadCountNotification(unreadCount: Int) {
        if (unreadCount <= 0) return

        // Android 13+ runtime permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        ensureChannel()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = context.getString(R.string.notifs_unread_notification_title)
        val text = context.getString(R.string.notifs_unread_notification_body, unreadCount)

        val notification = NotificationCompat.Builder(context, NotifsNotificationChannels.UNREAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_star_filled)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setNumber(unreadCount)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(UNREAD_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val name = context.getString(R.string.notifs_unread_channel_name)
        val descriptionText = context.getString(R.string.notifs_unread_channel_description)

        val channel = NotificationChannel(
            NotifsNotificationChannels.UNREAD_CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = descriptionText
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val UNREAD_NOTIFICATION_ID = 1001
    }
}


