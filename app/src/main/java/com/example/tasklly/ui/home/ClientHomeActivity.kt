package com.example.tasklly.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tasklly.R
import com.example.tasklly.databinding.ActivityClientHomeBinding
import com.example.tasklly.ui.home.client.ClientHomeFragment
import com.example.tasklly.ui.home.client.ClientOffersFragment
import com.example.tasklly.ui.home.client.task.ClientMyTasksFragment
import com.example.tasklly.ui.profile.ClientProfileFragment
import com.example.tasklly.util.BaseActivity
import com.example.tasklly.util.ThemeManager
import com.example.tasklly.ui.messages.MessagesFragment
import com.example.tasklly.ui.payments.PaymentActivity
import com.example.tasklly.ui.payments.PaymentsFragment


class ClientHomeActivity :  BaseActivity() {

    private lateinit var b: ActivityClientHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ apply dark/light theme
        ThemeManager.apply(this)

        super.onCreate(savedInstanceState)
        b = ActivityClientHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        // ✅ Bottom navigation listener (МНОГУ ВАЖНО да е прво)
        b.clientBottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> openFragment(ClientHomeFragment())
                R.id.nav_tasks -> openFragment(ClientMyTasksFragment())
                R.id.nav_payments -> openFragment(f = PaymentsFragment())
                R.id.nav_profile -> openFragment(ClientProfileFragment())
                R.id.nav_messages -> openFragment(MessagesFragment())

            }
            true
        }

        // ✅ Провери дали треба директно да отвори Profile
        val openProfile = intent.getBooleanExtra("openProfile", false)

        if (savedInstanceState == null) {
            if (openProfile) {
                // ➜ после Register
                b.clientBottomNav.selectedItemId = R.id.nav_profile
            } else {
                // ➜ нормално после Login
                b.clientBottomNav.selectedItemId = R.id.nav_home
            }
        }
    }

    // ✅ ако сакаш од fragment да менуваш tab
    fun setSelectedBottomTab(itemId: Int) {
        b.clientBottomNav.selectedItemId = itemId
    }

    private fun openFragment(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.clientContainer, f)
            .commit()
    }
}
