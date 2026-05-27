package com.yarnspace.app.feature.notifs.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerNotifsWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotifsWorkScheduler {

    override fun scheduleUnreadPolling() {
        // We use APPEND_OR_REPLACE so this can be called even while the current work is RUNNING
        // (e.g., from inside the worker itself to schedule the next run).
        enqueue(delayMinutes = POLL_INTERVAL_MINUTES, policy = ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    override fun triggerUnreadCheckNow() {
        enqueue(delayMinutes = 0, policy = ExistingWorkPolicy.REPLACE)
    }

    override fun cancelUnreadPolling() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun enqueue(delayMinutes: Long, policy: ExistingWorkPolicy) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val requestBuilder = OneTimeWorkRequestBuilder<UnreadNotifsPollingWorker>()
            .setConstraints(constraints)

        if (delayMinutes > 0) {
            requestBuilder.setInitialDelay(delayMinutes, TimeUnit.MINUTES)
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            policy,
            requestBuilder.build(),
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "unread_notifs_polling"
        private const val POLL_INTERVAL_MINUTES = 10L
    }
}




