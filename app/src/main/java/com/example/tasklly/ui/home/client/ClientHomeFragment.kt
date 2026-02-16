package com.example.tasklly.ui.home.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tasklly.R
import com.example.tasklly.ui.home.ClientHomeActivity
import com.example.tasklly.ui.home.client.post.PostTaskActivity
import com.example.tasklly.ui.home.client.task.ClientMyTasksFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ClientHomeFragment : Fragment(R.layout.fragment_client_home) {

    private lateinit var btnPost: Button
    private lateinit var btnMyTasks: Button

    private lateinit var tvOpenCount: TextView
    private lateinit var tvInProgressCount: TextView
    private lateinit var tvCompletedCount: TextView

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }

    private var tasksListener: ValueEventListener? = null
    private var tasksQuery: Query? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnPost = view.findViewById(R.id.btnPostTask)
        btnMyTasks = view.findViewById(R.id.btnMyTasks)

        tvOpenCount = view.findViewById(R.id.tvOpenCount)
        tvInProgressCount = view.findViewById(R.id.tvInProgressCount)
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount)

        // disable theme tint ако ти прави лилаво
        btnPost.backgroundTintList = null
        btnMyTasks.backgroundTintList = null

        btnPost.setOnClickListener {
            startActivity(Intent(requireContext(), PostTaskActivity::class.java))
        }

        btnMyTasks.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.clientContainer, ClientMyTasksFragment())
                .commit()

            (activity as? ClientHomeActivity)?.setSelectedBottomTab(R.id.nav_tasks)
        }
    }

    override fun onStart() {
        super.onStart()
        attachCountsListener()
    }

    override fun onStop() {
        super.onStop()
        detachCountsListener()
    }

    private fun attachCountsListener() {
        val uid = auth.currentUser?.uid ?: return

        // слушаме задачи на овој client
        tasksQuery = db.child("tasks").orderByChild("clientId").equalTo(uid)

        tasksListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var open = 0
                var inProgress = 0
                var completed = 0

                for (taskSnap in snapshot.children) {
                    val status = taskSnap.child("status").getValue(String::class.java) ?: "open"

                    when (status.lowercase()) {
                        "open" -> open++
                        "in_progress", "inprogress", "in progress" -> inProgress++
                        "completed", "done" -> completed++
                        else -> {
                            // ако имаш други статуси, одлучи каде да ги броиш
                            // пример: default во open
                            open++
                        }
                    }
                }

                tvOpenCount.text = open.toString()
                tvInProgressCount.text = inProgress.toString()
                tvCompletedCount.text = completed.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                // ако сакаш, можеш да ставиш Toast
            }
        }

        tasksQuery?.addValueEventListener(tasksListener as ValueEventListener)
    }

    private fun detachCountsListener() {
        val listener = tasksListener ?: return
        tasksQuery?.removeEventListener(listener)
        tasksListener = null
        tasksQuery = null
    }
}
