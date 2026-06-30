package com.example.funlife.game.platformer.catalog

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.funlife.resource.ResourceStore
import java.util.concurrent.TimeUnit

/**
 * WorkManager 闲时 decode：WiFi + 充电时后台补齐横版全角色磁盘缓存，不阻塞首屏。
 */
class PlatformerDecodeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!ResourceStore.isAssetSourceConfigured()) return Result.success()
        if (ResourceStore.localGameResourceStatus().pendingBundleIds.isNotEmpty()) {
            return Result.retry()
        }
        if (!PlatformerResourcePrewarmCoordinator.needsDecodePrewarm(applicationContext)) {
            return Result.success()
        }
        PlatformerResourcePrewarmCoordinator.runDecodePrewarmBlocking(applicationContext)
        return if (PlatformerResourcePrewarmCoordinator.needsDecodePrewarm(applicationContext)) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "platformer_decode_idle"

        fun scheduleIdle(context: Context) {
            if (!PlatformerResourcePrewarmCoordinator.needsDecodePrewarm(context)) return
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .build()
            val request = OneTimeWorkRequestBuilder<PlatformerDecodeWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun scheduleExpedited(context: Context) {
            if (!PlatformerResourcePrewarmCoordinator.needsDecodePrewarm(context)) return
            val request = OneTimeWorkRequestBuilder<PlatformerDecodeWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
