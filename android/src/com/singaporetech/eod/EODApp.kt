package com.singaporetech.eod

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Represents a non-UI global place to contain the global App Context.
 * Many things should not be owned individually in the components and should be here.
 *
 * Note on context:
 *  - load resources (strings, assets, etc)
 *  - starting Activities and Services
 *  - send broadcasts, register receivers
 *  - Activity context can inflate layouts
 */
class EODApp: Application() {
    // 1. lazy init the Room DB
    // 2. lazy init the player repo with the DAO from the DB
    private val db by lazy { PlayerDB.getDatabase(this) }
    val playerRepo by lazy { PlayerRepo(db.playerDAO()) }

    // 1. lazy init the volley network request queue using the same patten as repo
    private val networkRequestQueue by lazy { NetworkRequestQueue.getInstance(this) }
    val weatherRepo by lazy { WeatherRepo(networkRequestQueue) }

    /**
     * Init the stuff that needs context to be initialized.
     * - create a periodic reminder work to be thrown to the WorkManager
     */
    override fun onCreate() {
        super.onCreate()

        val workConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
                .setConstraints(workConstraints)
                .build()

        WorkManager.getInstance(this).enqueue(periodicWorkRequest)
    }
}
