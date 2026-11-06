package dev.zionixx.pcmstudyos.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.zionixx.pcmstudyos.data.local.PcmDatabase
import dev.zionixx.pcmstudyos.data.repo.SettingsRepository
import dev.zionixx.pcmstudyos.domain.RevisionSchedule
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * One lightweight daily job that raises a single in-app notification when
 * revisions or tasks are due. No network, no polling, no wake locks.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: PcmDatabase,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.notificationsEnabled) return Result.success()

        val yearId = settings.currentYearId ?: return Result.success()
        val today = LocalDate.now()

        val revisions = database.revisionDao().forYear(yearId)
            .count { RevisionSchedule.statusOf(it, today) == "due" || RevisionSchedule.statusOf(it, today) == "overdue" }
        val tasks = database.taskDao().forYear(yearId)
            .count { it.status != "completed" && it.dueDate != null && it.dueDate <= today.toString() }

        if (revisions == 0 && tasks == 0) return Result.success()

        val parts = buildList {
            if (revisions > 0) add("$revisions revision${if (revisions == 1) "" else "s"} due")
            if (tasks > 0) add("$tasks task${if (tasks == 1) "" else "s"} due today")
        }
        Notifications.showReminder(applicationContext, parts.joinToString(" · "))
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "pcm-daily-reminder"
    }
}

object ReminderScheduler {
    const val WORK_NAME = "pcm-daily-reminder"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.NONE)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
