package com.yarnspace.app.feature.notifs.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.yarnspace.app.core.auth.TokenManager
import com.yarnspace.app.feature.notifs.domain.NotifsRepository
import com.yarnspace.app.feature.notifs.notification.NotifsNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import java.io.IOException

@HiltWorker
class UnreadNotifsPollingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val notifsRepository: NotifsRepository,
    private val notifier: NotifsNotifier,
    private val notifsWorkScheduler: NotifsWorkScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        // If we're not authenticated, don't retry.
        if (TokenManager.getToken(applicationContext).isNullOrBlank()) {
            return ListenableWorker.Result.success()
        }

        return try {
            val unreadCount = notifsRepository.getUnreadCount()
            if (unreadCount > 0) {
                notifier.showUnreadCountNotification(unreadCount)
            }

            // Schedule the next poll. (WorkManager periodic jobs have a 15m minimum,
            // so we implement a self-rescheduling one-time work instead.)
            notifsWorkScheduler.scheduleUnreadPolling()

            ListenableWorker.Result.success()
        } catch (e: HttpException) {
            when (e.code()) {
                401, 403 -> ListenableWorker.Result.success() // token invalid / logged out
                in 500..599 -> ListenableWorker.Result.retry()
                else -> ListenableWorker.Result.success()
            }
        } catch (_: IOException) {
            ListenableWorker.Result.retry()
        } catch (_: Exception) {
            ListenableWorker.Result.retry()
        }
    }
}



