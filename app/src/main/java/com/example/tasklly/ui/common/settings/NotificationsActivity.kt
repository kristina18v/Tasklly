package com.example.tasklly.ui.common.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tasklly.R
import com.example.tasklly.util.BaseActivity
import com.example.tasklly.util.ThemeManager

class NotificationsActivity :  BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
    }
}
