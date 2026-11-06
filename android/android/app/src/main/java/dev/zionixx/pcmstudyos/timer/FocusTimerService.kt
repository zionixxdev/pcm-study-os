package dev.zionixx.pcmstudyos.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.zionixx.pcmstudyos.MainActivity
import dev.zionixx.pcmstudyos.R
import dev.zionixx.pcmstudyos.data.repo.SettingsRepository
import dev.zionixx.pcmstudyos.data.repo.StudyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the focus timer running while the app is in the background.
 *
 * The countdown is derived from the monotonic clock, so pausing, resuming or a
 * configuration change never drifts. When the planned block finishes the service
 * saves the session and stops itself.
 */
@AndroidEntryPoint
class FocusTimerService : Service() {

    @Inject lateinit var controller: TimerController
    @Inject lateinit var studyRepository: StudyRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch {
                commitAndStop()
            }
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification("Starting…"))
        if (ticker == null) {
            ticker = scope.launch {
                while (true) {
                    val state = controller.state.value
                    if (!state.running) {
                        if (state.accumulatedMs == 0L) break
                        updateNotification("Paused · ${format(state.accumulatedMs)}")
                    } else {
                        updateNotification(format(state.remainingMs(), state.mode))
                        val finished = state.mode != TimerMode.STOPWATCH && state.remainingMs() <= 0L
                        if (finished) {
                            val settings = settingsRepository.settings.first()
                            controller.advancePhase(settings.pomodoroRounds)
                            commitAndStop()
                            break
                        }
                    }
                    delay(1000)
                }
            }
        }
        return START_STICKY
    }

    private suspend fun commitAndStop() {
        controller.commit(studyRepository, settingsRepository)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        ticker = null
    }

    private fun format(remainingMs: Long, mode: TimerMode = TimerMode.POMODORO): String {
        if (mode == TimerMode.STOPWATCH) return "Stopwatch running"
        val totalSeconds = remainingMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        ensureChannel()
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).putExtra("route", "timer"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FocusTimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_TIMER)
            .setContentTitle(getString(R.string.timer_running))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .addAction(0, "Stop & save", stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_TIMER) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_TIMER,
                        getString(R.string.notif_channel_timer),
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = getString(R.string.notif_channel_timer_desc)
                        setShowBadge(false)
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        ticker?.cancel()
        ticker = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_TIMER = "focus_timer"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "dev.zionixx.pcmstudyos.STOP_TIMER"

        fun start(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, FocusTimerService::class.java).setAction(ACTION_STOP))
        }
    }
}

/** Notification action receiver (kept for completeness of the manifest entry). */
class TimerNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        FocusTimerService.stop(context)
    }
}
