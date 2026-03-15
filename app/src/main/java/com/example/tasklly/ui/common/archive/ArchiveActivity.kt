package com.example.tasklly.ui.common.archive

import android.os.Bundle
import android.util.Log
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
    private val items = mutableListOf<Pair<Task, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive)

        val mode = intent.getStringExtra("mode") ?: "client"
        val uid = auth.currentUser?.uid

        Log.d("ARCHIVE", "mode=$mode uid=$uid")

        findViewById<TextView>(R.id.tvTitle).text =
            if (mode == "client") "Archive (Client)" else "Archive (Provider)"

        val rv = findViewById<RecyclerView>(R.id.rvArchive)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = ArchiveAdapter(mode, items)
        rv.adapter = adapter

        loadArchive(mode)
    }

    private fun loadArchive(mode: String) {
        val uid = auth.currentUser?.uid ?: return

        db.child("tasks").get()
            .addOnSuccessListener { snap ->
                items.clear()

                Log.d("ARCHIVE", "all tasks count=${snap.childrenCount}")

                for (c in snap.children) {
                    val task = c.getValue(Task::class.java) ?: continue
                    task.taskId = c.key ?: ""

                    Log.d(
                        "ARCHIVE_TASK",
                        "id=${task.taskId}, title=${task.title}, status=${task.status}, clientId=${task.clientId}, assignedProviderId=${task.assignedProviderId}"
                    )

                    if (task.status != "completed") continue

                    val allowed = if (mode == "client") {
                        task.clientId == uid
                    } else {
                        task.assignedProviderId == uid
                    }

                    if (!allowed) continue

                    val otherUid = if (mode == "client") {
                        task.assignedProviderId ?: ""
                    } else {
                        task.clientId
                    }

                    if (otherUid.isBlank()) {
                        items.add(task to "Unknown")
                        continue
                    }

                    db.child("users").child(otherUid).get()
                        .addOnSuccessListener { userSnap ->
                            val first = userSnap.child("firstName").getValue(String::class.java) ?: ""
                            val last = userSnap.child("lastName").getValue(String::class.java) ?: ""
                            val name = (first + " " + last).trim().ifBlank {
                                userSnap.child("name").getValue(String::class.java)
                                    ?: userSnap.child("email").getValue(String::class.java)
                                    ?: "User"
                            }

                            items.add(task to name)
                            adapter.notifyDataSetChanged()

                            Log.d("ARCHIVE", "added task=${task.title} other=$name")
                        }
                        .addOnFailureListener {
                            items.add(task to "User")
                            adapter.notifyDataSetChanged()
                        }
                }

                // за случај кога otherUid е празно
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.d("ARCHIVE", "load failed=${e.message}")
            }
    }
}