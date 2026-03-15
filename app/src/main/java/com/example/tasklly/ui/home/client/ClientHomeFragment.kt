package com.example.tasklly.ui.home.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.Task
import com.example.tasklly.ui.adapter.RecentTasksAdapter
import com.example.tasklly.ui.home.ClientHomeActivity
import com.example.tasklly.ui.home.client.post.PostTaskActivity
import com.example.tasklly.ui.home.client.task.ClientMyTasksFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ClientHomeFragment : Fragment(R.layout.fragment_client_home) {

    private lateinit var btnPost: Button
    private lateinit var btnMyTasks: LinearLayout
    private lateinit var btnOffers: LinearLayout

    private lateinit var tvUserName: TextView
    private lateinit var tvSeeAll: TextView

    private lateinit var tvOpenCount: TextView
    private lateinit var tvInProgressCount: TextView
    private lateinit var tvCompletedCount: TextView

    private lateinit var layoutEmptyRecent: LinearLayout
    private lateinit var rvRecentTasks: RecyclerView

    private lateinit var recentTasksAdapter: RecentTasksAdapter
    private val recentTasks = mutableListOf<Task>()

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }

    private var tasksListener: ValueEventListener? = null
    private var tasksQuery: Query? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnPost = view.findViewById(R.id.btnPostTask)
        btnMyTasks = view.findViewById(R.id.btnMyTasks)
        btnOffers = view.findViewById(R.id.btnOffers)

        tvUserName = view.findViewById(R.id.tvUserName)
        tvSeeAll = view.findViewById(R.id.tvSeeAll)

        tvOpenCount = view.findViewById(R.id.tvOpenCount)
        tvInProgressCount = view.findViewById(R.id.tvInProgressCount)
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount)

        layoutEmptyRecent = view.findViewById(R.id.layoutEmptyRecent)
        rvRecentTasks = view.findViewById(R.id.rvRecentTasks)

        btnPost.backgroundTintList = null

        loadUserName()
        setupRecentTasks()
        loadRecentTasks()

        btnPost.setOnClickListener {
            startActivity(Intent(requireContext(), PostTaskActivity::class.java))
        }

        btnMyTasks.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.clientContainer, ClientMyTasksFragment())
                .addToBackStack(null)
                .commit()

            (activity as? ClientHomeActivity)?.setSelectedBottomTab(R.id.nav_tasks)
        }

        tvSeeAll.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.clientContainer, ClientMyTasksFragment())
                .addToBackStack(null)
                .commit()

            (activity as? ClientHomeActivity)?.setSelectedBottomTab(R.id.nav_tasks)
        }

        btnOffers.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.clientContainer, ClientOffersFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onStart() {
        super.onStart()
        attachCountsListener()
        loadRecentTasks()
        loadUserName()
    }

    override fun onStop() {
        super.onStop()
        detachCountsListener()
    }

    private fun loadUserName() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            tvUserName.text = "User 👋"
            return
        }

        db.child("users")
            .child(uid)
            .child("name")
            .get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getValue(String::class.java)

                tvUserName.text = if (!name.isNullOrBlank()) {
                    "$name 👋"
                } else {
                    val displayName = auth.currentUser?.displayName
                    if (!displayName.isNullOrBlank()) "$displayName 👋" else "User 👋"
                }
            }
            .addOnFailureListener {
                val displayName = auth.currentUser?.displayName
                tvUserName.text = if (!displayName.isNullOrBlank()) {
                    "$displayName 👋"
                } else {
                    "User 👋"
                }
            }
    }

    private fun setupRecentTasks() {
        recentTasksAdapter = RecentTasksAdapter(recentTasks) { task ->
            // Ако подоцна имаш details screen, отвори го тука
            // пример:
            // startActivity(Intent(requireContext(), ClientTaskDetailsActivity::class.java).apply {
            //     putExtra("taskId", task.id)
            // })
        }

        rvRecentTasks.layoutManager = LinearLayoutManager(requireContext())
        rvRecentTasks.adapter = recentTasksAdapter
    }

    private fun loadRecentTasks() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.child("tasks")
            .orderByChild("clientId")
            .equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val allTasks = mutableListOf<Task>()

                    for (child in snapshot.children) {
                        val task = child.getValue(Task::class.java)
                        if (task != null) {
                            val taskWithId = try {
                                task.copy(id = child.key ?: "")
                            } catch (_: Exception) {
                                task
                            }
                            allTasks.add(taskWithId)
                        }
                    }

                    val latestTwo = allTasks
                        .sortedByDescending { it.createdAt }
                        .take(2)

                    if (latestTwo.isEmpty()) {
                        layoutEmptyRecent.visibility = View.VISIBLE
                        rvRecentTasks.visibility = View.GONE
                    } else {
                        layoutEmptyRecent.visibility = View.GONE
                        rvRecentTasks.visibility = View.VISIBLE
                        recentTasksAdapter.updateData(latestTwo)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    layoutEmptyRecent.visibility = View.VISIBLE
                    rvRecentTasks.visibility = View.GONE
                }
            })
    }

    private fun attachCountsListener() {
        val uid = auth.currentUser?.uid ?: return

        tasksQuery = db.child("tasks")
            .orderByChild("clientId")
            .equalTo(uid)

        tasksListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var open = 0
                var inProgress = 0
                var completed = 0

                for (taskSnap in snapshot.children) {
                    val status = taskSnap.child("status").getValue(String::class.java) ?: "open"

                    when (status.lowercase()) {
                        "open" -> open++
                        "in_progress", "in progress", "inprogress" -> inProgress++
                        "completed", "done" -> completed++
                        else -> open++
                    }
                }

                tvOpenCount.text = open.toString()
                tvInProgressCount.text = inProgress.toString()
                tvCompletedCount.text = completed.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                tvOpenCount.text = "0"
                tvInProgressCount.text = "0"
                tvCompletedCount.text = "0"
            }
        }

        tasksQuery?.addValueEventListener(tasksListener!!)
    }

    private fun detachCountsListener() {
        val listener = tasksListener ?: return
        tasksQuery?.removeEventListener(listener)
        tasksListener = null
        tasksQuery = null
    }
}