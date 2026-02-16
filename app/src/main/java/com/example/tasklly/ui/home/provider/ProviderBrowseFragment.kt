package com.example.tasklly.ui.home.provider

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasklly.R
import com.example.tasklly.data.model.Task
import com.example.tasklly.databinding.FragmentListBinding
import com.example.tasklly.ui.adapter.TasksAdapter
import com.example.tasklly.ui.home.provider.task.ProviderTaskDetailsActivity
import com.google.firebase.database.FirebaseDatabase

class ProviderBrowseFragment : Fragment(R.layout.fragment_list) {

    private var _b: FragmentListBinding? = null
    private val b get() = _b!!
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private val items = mutableListOf<Task>()
    private lateinit var adapter: TasksAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentListBinding.bind(view)

        adapter = TasksAdapter(items) { task ->
            startActivity(Intent(requireContext(), ProviderTaskDetailsActivity::class.java).apply {
                putExtra("taskId", task.taskId)
            })
        }

        b.rv.layoutManager = LinearLayoutManager(requireContext())
        b.rv.adapter = adapter

        loadOpenTasks()
    }

    private fun loadOpenTasks() {
        db.child("tasks").get().addOnSuccessListener { snap ->
            items.clear()
            for (c in snap.children) {
                val t = c.getValue(Task::class.java) ?: continue
                if (t.status == "open") items.add(t)
            }
            items.sortByDescending { it.createdAt }
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}