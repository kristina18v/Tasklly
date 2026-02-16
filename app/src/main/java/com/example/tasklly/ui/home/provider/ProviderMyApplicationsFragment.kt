package com.example.tasklly.ui.home.provider

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasklly.R
import com.example.tasklly.data.model.Application
import com.example.tasklly.databinding.FragmentListBinding
import com.example.tasklly.ui.adapter.ApplicationsAdapter
import com.example.tasklly.ui.messages.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProviderMyApplicationsFragment : Fragment(R.layout.fragment_list) {

    private var _b: FragmentListBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private val items = mutableListOf<Application>()
    private lateinit var adapter: ApplicationsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentListBinding.bind(view)

        adapter = ApplicationsAdapter(
            items = items,
            showActions = false,     // provider само гледа, не прифаќа/одбива тука
            onAccept = {},
            onDecline = {},
            onMessage = { app ->
                // ✅ PROVIDER -> CLIENT MESSAGE
                // мора да имаш clientId во Application (ако немаш, кажи ми и ќе ти средам структура)
                val otherUid = app.clientId
                if (otherUid.isNullOrBlank()) return@ApplicationsAdapter

                val otherName = app.clientName?.ifBlank { "Client" } ?: "Client"

                startActivity(
                    Intent(requireContext(), ChatActivity::class.java)
                        .putExtra("otherUid", otherUid)
                        .putExtra("otherName", otherName)
                )
            }
        )

        b.rv.layoutManager = LinearLayoutManager(requireContext())
        b.rv.adapter = adapter

        loadMyApplications()
    }

    private fun loadMyApplications() {
        val uid = auth.currentUser?.uid ?: return

        db.child("applications").get().addOnSuccessListener { snap ->
            items.clear()
            for (taskNode in snap.children) {
                for (providerNode in taskNode.children) {
                    val a = providerNode.getValue(Application::class.java) ?: continue
                    if (a.providerId == uid) items.add(a)
                }
            }
            items.sortByDescending { it.createdAt }
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
