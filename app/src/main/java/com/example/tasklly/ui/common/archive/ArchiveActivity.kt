package com.example.tasklly.ui.common.archive

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.Task
import com.example.tasklly.util.BaseActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ArchiveActivity : BaseActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var adapter: ArchiveAdapter
    private val items = mutableListOf<Pair<Task, String>>() // task + otherName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive)

        val mode = intent.getStringExtra("mode") ?: "client"
        findViewById<TextView>(R.id.tvTitle).text =
            if (mode == "client") "Archive (Client)" else "Archive (Provider)"

        val rv = findViewById<RecyclerView>(R.id.rvArchive)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ArchiveAdapter(mode, items)
        rv.adapter = adapter

        val uid = auth.currentUser?.uid ?: return

        // 1) земи листа на completed taskIds
        db.child("history").child(uid).child("completed")
            .get()
            .addOnSuccessListener { snap ->
                val ids = snap.children.mapNotNull { it.key }
                if (ids.isEmpty()) return@addOnSuccessListener

                // 2) за секој id земи task, па земи "другиот корисник"
                ids.forEach { taskId ->
                    db.child("tasks").child(taskId)
                        .get()
                        .addOnSuccessListener { taskSnap ->
                            val task = taskSnap.getValue(Task::class.java) ?: return@addOnSuccessListener

                            // ✅ најважно: taskId секогаш = Firebase key
                            task.taskId = taskId

                            // ✅ друг корисник
                            val otherUid = if (mode == "client") {
                                task.assignedProviderId ?: ""
                            } else {
                                task.clientId
                            }

                            if (otherUid.isBlank()) {
                                items.add(task to "Unknown")
                                adapter.notifyItemInserted(items.size - 1)
                                return@addOnSuccessListener
                            }

                            db.child("users").child(otherUid)
                                .get()
                                .addOnSuccessListener { userSnap ->
                                    val first = userSnap.child("firstName").getValue(String::class.java) ?: ""
                                    val last = userSnap.child("lastName").getValue(String::class.java) ?: ""
                                    val otherName = (first + " " + last).trim().ifBlank { "User" }

                                    items.add(task to otherName)
                                    adapter.notifyItemInserted(items.size - 1)
                                }
                        }
                }
            }
    }
}
