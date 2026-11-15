package com.example.starter

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class StarterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug only. In release, plant a tree that forwards warnings/errors to Crashlytics/Sentry.
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
