package com.example.tasklly.ui.home.client.task

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.Application
import com.example.tasklly.data.model.Task
import com.example.tasklly.ui.adapter.ApplicationsAdapter
import com.example.tasklly.ui.messages.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ClientTaskDetailsActivity : AppCompatActivity() {

    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var tvTitle: TextView
    private lateinit var tvDesc: TextView
    private lateinit var tvStatus: TextView
    private lateinit var rv: RecyclerView

    private val apps = mutableListOf<Application>()
    private lateinit var adapter: ApplicationsAdapter

    private var taskId: String = ""
    private var currentTask: Task? = null

    // 🔗 Stripe Payment Link (ист за сите, најлесно)
    private val paymentUrl = "https://buy.stripe.com/test_14A6oH2734jMa3ib918N200"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_task_details)

        taskId = intent.getStringExtra("taskId") ?: ""
        if (taskId.isBlank()) { finish(); return }

        tvTitle = findViewById(R.id.tvTitle)
        tvDesc = findViewById(R.id.tvDesc)
        tvStatus = findViewById(R.id.tvStatus)
        rv = findViewById(R.id.rvApps)

        adapter = ApplicationsAdapter(
            items = apps,
            showActions = true,
            onAccept = { acceptAndCreateOrder(it) },
            onDecline = { decline(it) },
            onMessage = { app ->
                val otherUid = app.providerId
                if (otherUid.isBlank()) return@ApplicationsAdapter

                val name = app.providerName.ifBlank { "Provider" }
                val auto = "Hi $name, I’m interested. Let’s discuss the task: ${tvTitle.text}."

                startActivity(
                    Intent(this, ChatActivity::class.java)
                        .putExtra("otherUid", otherUid)
                        .putExtra("otherName", name)
                        .putExtra("autoMessage", auto)
                )
            }
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        loadTask()
        loadApplications()
    }

    private fun loadTask() {
        db.child("tasks").child(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val t = snap.getValue(Task::class.java) ?: return
                    // ✅ важно: taskId како Firebase key
                    t.taskId = snap.key ?: taskId

                    currentTask = t
                    tvTitle.text = t.title
                    tvDesc.text = t.desc
                    tvStatus.text = "Status: ${t.status}"
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ✅ ЧИТАЊЕ од taskApplications/{taskId}
    private fun loadApplications() {
        db.child("taskApplications").child(taskId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    apps.clear()

                    for (c in snap.children) {
                        val a = c.getValue(Application::class.java) ?: continue

                        // ако providerId не се мапира, земи го од key
                        if (a.providerId.isBlank()) a.providerId = c.key ?: ""

                        apps.add(a)
                    }

                    apps.sortByDescending { it.createdAt }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ✅ ACCEPT + CREATE ORDER (NO OPEN PAYMENT)
    private fun acceptAndCreateOrder(app: Application) {
        val clientId = auth.currentUser?.uid
        if (clientId.isNullOrBlank()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_LONG).show()
            return
        }

        val task = currentTask
        if (task == null) {
            Toast.makeText(this, "Task not loaded yet. Try again.", Toast.LENGTH_LONG).show()
            return
        }

        if (app.providerId.isBlank()) {
            Toast.makeText(this, "Provider missing", Toast.LENGTH_LONG).show()
            return
        }

        val orderId = db.child("orders").push().key
        if (orderId.isNullOrBlank()) {
            Toast.makeText(this, "❌ Could not create orderId", Toast.LENGTH_LONG).show()
            return
        }

        val now = System.currentTimeMillis()

        val updates = hashMapOf<String, Any>(
            // accept во taskApplications + providerApplications
            "taskApplications/$taskId/${app.providerId}/status" to "accepted",
            "providerApplications/${app.providerId}/$taskId/status" to "accepted",

            // task -> in progress + assigned provider
            "tasks/$taskId/status" to "in_progress",
            "tasks/$taskId/assignedProviderId" to app.providerId,

            // orders -> pending (ќе се појави во Payments таб)
            "orders/$orderId/orderId" to orderId,
            "orders/$orderId/taskId" to (task.taskId.ifBlank { taskId }),
            "orders/$orderId/clientId" to clientId,
            "orders/$orderId/providerId" to app.providerId,
            "orders/$orderId/providerName" to app.providerName,
            "orders/$orderId/taskTitle" to task.title,
            "orders/$orderId/amount" to app.price,
            "orders/$orderId/currency" to "EUR",
            "orders/$orderId/status" to "pending",
            "orders/$orderId/paymentUrl" to paymentUrl,
            "orders/$orderId/createdAt" to now
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Added to Payments", Toast.LENGTH_SHORT).show()
                rejectOthers(app.providerId)
                loadTask()
                loadApplications()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Accept/order failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ✅ reject others
    private fun rejectOthers(acceptedProviderId: String) {
        db.child("taskApplications").child(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val updates = hashMapOf<String, Any>()
                    for (c in snap.children) {
                        val pid = c.key ?: continue
                        if (pid != acceptedProviderId) {
                            updates["taskApplications/$taskId/$pid/status"] = "rejected"
                            updates["providerApplications/$pid/$taskId/status"] = "rejected"
                        }
                    }
                    if (updates.isNotEmpty()) db.updateChildren(updates)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun decline(app: Application) {
        if (app.providerId.isBlank()) return

        val updates = hashMapOf<String, Any>(
            "taskApplications/$taskId/${app.providerId}/status" to "rejected",
            "providerApplications/${app.providerId}/$taskId/status" to "rejected"
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Declined", Toast.LENGTH_SHORT).show()
                loadApplications()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Decline failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
