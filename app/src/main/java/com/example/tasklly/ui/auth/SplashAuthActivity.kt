package com.example.tasklly.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.tasklly.R
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class SplashAuthActivity : ComponentActivity() {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_auth)

        // ✅ Track screen open (custom)
        analytics.logEvent("screen_open", Bundle().apply {
            putString("screen_name", "SplashAuth")
        })

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignUp = findViewById<Button>(R.id.btnGoRegister)

        btnLogin.setOnClickListener {
            analytics.logEvent("tap_login", null)
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnSignUp.setOnClickListener {
            analytics.logEvent("tap_signup", null)
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}