package com.example.tasklly.ui.home.provider

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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

        adapter = TasksAdapter(items) { task ->
            Log.d("PROVIDER_JOBS", "clicked task=${task.title} status=${task.status}")
            if (task.status == "in_progress") {
                markTaskCompleted(task)
            } else {
                toast("Task status: ${task.status}")
            }
        }

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
                t.taskId = c.key ?: ""

                val assigned = t.assignedProviderId ?: ""

                Log.d(
                    "PROVIDER_JOBS_LOAD",
                    "id=${t.taskId}, title=${t.title}, status=${t.status}, assigned=$assigned, me=$uid"
                )

                if (assigned == uid && (t.status == "in_progress" || t.status == "completed")) {
                    items.add(t)
                }
            }

            items.sortByDescending { it.createdAt }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener {
            toast("Load failed: ${it.message}")
        }
    }

    private fun markTaskCompleted(task: Task) {
        val providerId = task.assignedProviderId ?: ""
        val clientId = task.clientId
        val taskId = task.taskId

        Log.d(
            "MARK_COMPLETED",
            "taskId=$taskId clientId=$clientId providerId=$providerId title=${task.title}"
        )

        if (providerId.isBlank() || clientId.isBlank() || taskId.isBlank()) {
            toast("Missing data")
            return
        }

        val updates = hashMapOf<String, Any>(
            "tasks/$taskId/status" to "completed",
            "tasks/$taskId/completedAt" to System.currentTimeMillis()
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                toast("Task marked as completed")
                Log.d("MARK_COMPLETED", "success")
                loadJobs()
            }
            .addOnFailureListener { e ->
                toast("Failed: ${e.message}")
                Log.d("MARK_COMPLETED", "failed=${e.message}")
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}