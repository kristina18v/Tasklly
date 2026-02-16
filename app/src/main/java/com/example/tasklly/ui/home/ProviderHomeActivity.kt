package com.example.tasklly.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.tasklly.R
import com.example.tasklly.databinding.ActivityProviderHomeBinding
import com.example.tasklly.ui.home.provider.ProviderBrowseFragment
import com.example.tasklly.ui.home.provider.ProviderHomeFragment
import com.example.tasklly.ui.home.provider.ProviderJobsFragment
import com.example.tasklly.ui.home.provider.ProviderMyApplicationsFragment
import com.example.tasklly.ui.messages.MessagesFragment
import com.example.tasklly.ui.profile.ProviderProfileFragment
import com.example.tasklly.util.BaseActivity

class ProviderHomeActivity : BaseActivity() {

    private lateinit var b: ActivityProviderHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProviderHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        // ✅ DEFAULT fragment
        if (savedInstanceState == null) {
            openFragment(ProviderBrowseFragment())
            b.providerBottomNav.selectedItemId = R.id.nav_provider_browse
        }

        b.providerBottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_provider_browse -> openFragment(ProviderHomeFragment())

                R.id.nav_provider_apps -> openFragment(ProviderMyApplicationsFragment())

                R.id.nav_provider_jobs -> openFragment(ProviderJobsFragment())

                R.id.nav_messages -> openFragment(MessagesFragment())

                R.id.nav_provider_profile -> openFragment(ProviderProfileFragment())
            }
            true
        }
    }

    private fun openFragment(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.providerContainer, f)
            .commit()
    }
}
