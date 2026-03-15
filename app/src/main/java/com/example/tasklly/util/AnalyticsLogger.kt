package com.example.tasklly.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsLogger {
    fun log(analytics: FirebaseAnalytics, name: String, params: Bundle? = null) {
        analytics.logEvent(name, params)
    }
}