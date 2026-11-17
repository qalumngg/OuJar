package com.qalumn.ourjar.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.qalumn.ourjar.data.Repo
import java.util.concurrent.TimeUnit

object WidgetSync {

    /** Redraws both widgets from the locally saved copy of the data. */
    suspend fun updateAll(context: Context) {
        LoveNoteWidget().updateAll(context)
        MoodJarWidget().updateAll(context)
    }

    private val needsNetwork: Constraints
        get() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Fetch fresh data and redraw as soon as possible (used when a widget is first added). */
    fun enqueueNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(needsNetwork).build()
        WorkManager.getInstance(context).enqueueUniqueWork("ourjar-sync-now", ExistingWorkPolicy.REPLACE, request)
    }

    /** Keeps widgets fresh when the app is closed. Android runs this at most every 15 minutes. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).setConstraints(needsNetwork).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("ourjar-sync-periodic", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            Repo.fetchOnce()
            WidgetSync.updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount >= 3) Result.failure() else Result.retry()
        }
    }
}
