package com.example.tasklly.util

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)  // ✅ секогаш пред super.onCreate
        super.onCreate(savedInstanceState)
    }
}
