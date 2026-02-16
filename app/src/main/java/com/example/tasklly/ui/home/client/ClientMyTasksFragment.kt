package com.example.tasklly.ui.home.client.task

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasklly.R
import com.example.tasklly.databinding.FragmentListBinding
import com.example.tasklly.ui.adapter.TasksAdapter
import com.example.tasklly.data.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ClientMyTasksFragment : Fragment(R.layout.fragment_list) {

    private var _b: FragmentListBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private val items = mutableListOf<Task>()
    private lateinit var adapter: TasksAdapter
    private var listener: ValueEventListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentListBinding.bind(view)

        adapter = TasksAdapter(items) { task ->
            startActivity(
                Intent(requireContext(), ClientTaskDetailsActivity::class.java)
                    .putExtra("taskId", task.taskId)
            )
        }

        b.rv.layoutManager = LinearLayoutManager(requireContext())
        b.rv.adapter = adapter

        b.etSearch.hint = "Search my tasks..."

        loadMyTasks()
    }

    private fun loadMyTasks() {
        val uid = auth.currentUser?.uid ?: return

        val q = db.child("tasks")
            .orderByChild("clientId")
            .equalTo(uid)

        listener = q.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                items.clear()
                for (s in snapshot.children) {
                    val t = s.getValue(Task::class.java)
                    if (t != null) items.add(t)
                }
                // newest first
                items.sortByDescending { it.createdAt }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
