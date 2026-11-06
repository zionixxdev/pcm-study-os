package dev.zionixx.pcmstudyos.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.zionixx.pcmstudyos.MainActivity
import dev.zionixx.pcmstudyos.R

object Notifications {

    const val CHANNEL_REMINDERS = "study_reminders"
    private const val REMINDER_ID = 2001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_REMINDERS) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    context.getString(R.string.notif_channel_reminders),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notif_channel_reminders_desc)
                },
            )
        }
    }

    fun showReminder(context: Context, text: String) {
        ensureChannels(context)
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Today in PCM Study OS")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(REMINDER_ID, notification)
    }
}
