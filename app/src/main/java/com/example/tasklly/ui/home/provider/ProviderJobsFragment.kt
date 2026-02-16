package com.example.tasklly.ui.home.provider

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasklly.R
import com.example.tasklly.data.model.Task
import com.example.tasklly.databinding.FragmentListBinding
import com.example.tasklly.ui.adapter.TasksAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProviderJobsFragment : Fragment(R.layout.fragment_list) {

    private var _b: FragmentListBinding? = null
    private val b get() = _b!!
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private val items = mutableListOf<Task>()
    private lateinit var adapter: TasksAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _b = FragmentListBinding.bind(view)

        adapter = TasksAdapter(items) { /* later: details */ }
        b.rv.layoutManager = LinearLayoutManager(requireContext())
        b.rv.adapter = adapter

        loadJobs()
    }

    private fun loadJobs() {
        val uid = auth.currentUser?.uid ?: return

        db.child("tasks").get().addOnSuccessListener { snap ->
            items.clear()

            for (c in snap.children) {
                val t = c.getValue(Task::class.java) ?: continue

                // ✅ секогаш сетирај taskId од key
                t.taskId = c.key ?: ""
                if (t.taskId.isBlank()) continue

                val assigned = t.assignedProviderId ?: ""

                if (assigned == uid && (t.status == "in_progress" || t.status == "completed")) {
                    items.add(t)
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
