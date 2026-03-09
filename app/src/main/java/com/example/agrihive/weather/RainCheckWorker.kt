package com.example.agrihive.weather

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Background worker that checks for rain and sends notifications
 * This runs periodically to check weather conditions and notify beekeepers
 */
class RainCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val weatherService = WeatherService()

    override suspend fun doWork(): Result {
        return try {
            // Check weather for Cebu (or configured location)
            val weather = weatherService.getWeatherByCity("Cebu City, PH")

            weather?.let {
                // Show notification based on warning level
                if (it.rainfallWarning.level > RainfallWarning.NONE.level) {
                    RainAlertNotification.showRainNotification(applicationContext, it.rainfallWarning)
                }
            }

            // Also check forecast
            val forecast = weatherService.getForecast(10.0, 123.9) // Cebu coordinates
            for ((index, day) in forecast.withIndex()) {
                if (index == 0) continue // Skip today
                if (day.rainfallProbability >= 50) {
                    RainAlertNotification.showRainForecastNotification(
                        applicationContext,
                        day.rainfallProbability,
                        day.date
                    )
                    break
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "rain_check_worker"

        /**
         * Schedule periodic rain check
         * Runs every 6 hours to check weather conditions
         */
        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<RainCheckWorker>(
                6, TimeUnit.HOURS, // Run every 6 hours
                30, TimeUnit.MINUTES // Flex period
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }

        /**
         * Run a one-time rain check
         */
        fun runNow(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<RainCheckWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

        /**
         * Cancel scheduled rain checks
         */
        fun cancelScheduledCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
