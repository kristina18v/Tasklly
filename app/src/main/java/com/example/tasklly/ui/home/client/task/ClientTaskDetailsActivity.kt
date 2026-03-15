package com.example.tasklly.ui.home.client.task

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.Application
import com.example.tasklly.data.model.Review
import com.example.tasklly.data.model.Task
import com.example.tasklly.ui.adapter.ApplicationsAdapter
import com.example.tasklly.ui.adapter.ReviewsAdapter
import com.example.tasklly.ui.messages.ChatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class ClientTaskDetailsActivity : AppCompatActivity() {

    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // ✅ Analytics
    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    private lateinit var tvTitle: TextView
    private lateinit var tvDesc: TextView
    private lateinit var tvStatus: TextView
    private lateinit var rv: RecyclerView
    private lateinit var btnCompleteReview: MaterialButton

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

        // ✅ screen open event
        analytics.logEvent("screen_open", Bundle().apply {
            putString("screen_name", "ClientTaskDetails")
            putString("task_id", taskId)
        })

        tvTitle = findViewById(R.id.tvTitle)
        tvDesc = findViewById(R.id.tvDesc)
        tvStatus = findViewById(R.id.tvStatus)
        rv = findViewById(R.id.rvApps)
        btnCompleteReview = findViewById(R.id.btnCompleteReview)

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

                // ✅ track click message
                analytics.logEvent("client_tap_message", Bundle().apply {
                    putString("task_id", taskId)
                    putString("provider_id", otherUid)
                })

                startActivity(
                    Intent(this, ChatActivity::class.java)
                        .putExtra("otherUid", otherUid)
                        .putExtra("otherName", name)
                        .putExtra("autoMessage", auto)
                        .putExtra("source", "client_task_details")
                        .putExtra("taskId", taskId)
                )
            },

            onViewReviews = { providerId ->
                // ✅ track view reviews
                analytics.logEvent("client_view_reviews", Bundle().apply {
                    putString("task_id", taskId)
                    putString("provider_id", providerId)
                })
                openReviewsBottomSheet(providerId)
            }
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnCompleteReview.setOnClickListener {
            val task = currentTask ?: return@setOnClickListener
            val providerId = task.assignedProviderId.orEmpty()

            if (providerId.isBlank()) {
                Toast.makeText(this, "No assigned provider", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ track open review dialog
            analytics.logEvent("client_open_review_dialog", Bundle().apply {
                putString("task_id", taskId)
                putString("provider_id", providerId)
            })

            showLeaveReviewDialog(providerId)
        }

        loadTask()
        loadApplications()
    }

    private fun loadTask() {
        db.child("tasks").child(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val t = snap.getValue(Task::class.java) ?: return
                    t.taskId = snap.key ?: taskId

                    currentTask = t
                    tvTitle.text = t.title
                    tvDesc.text = t.desc
                    tvStatus.text = "Status: ${t.status}"

                    updateCompleteButtonVisibility()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateCompleteButtonVisibility() {
        val clientId = auth.currentUser?.uid.orEmpty()
        val task = currentTask

        if (clientId.isBlank() || task == null) {
            btnCompleteReview.visibility = android.view.View.GONE
            return
        }

        val canShow = task.status == "in_progress" && !task.assignedProviderId.isNullOrBlank()
        if (!canShow) {
            btnCompleteReview.visibility = android.view.View.GONE
            return
        }

        // ✅ show only if NOT already reviewed
        db.child("taskReviews").child(taskId).child(clientId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val already = s.getValue(Boolean::class.java) == true
                    btnCompleteReview.visibility =
                        if (already) android.view.View.GONE else android.view.View.VISIBLE
                }
                override fun onCancelled(error: DatabaseError) {
                    btnCompleteReview.visibility = android.view.View.VISIBLE
                }
            })
    }

    //  ЧИТАЊЕ од taskApplications
    private fun loadApplications() {
        db.child("taskApplications").child(taskId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    apps.clear()

                    for (c in snap.children) {
                        val a = c.getValue(Application::class.java) ?: continue
                        if (a.providerId.isBlank()) a.providerId = c.key ?: ""
                        apps.add(a)
                    }

                    apps.sortByDescending { it.createdAt }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ✅ BottomSheet за reviews
    private fun openReviewsBottomSheet(providerId: String) {
        if (providerId.isBlank()) return

        val dialog = BottomSheetDialog(this)
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_reviews, null)

        val tvTitle = v.findViewById<TextView>(R.id.tvReviewsTitle)
        val tvEmpty = v.findViewById<TextView>(R.id.tvEmptyReviews)
        val rvReviews = v.findViewById<RecyclerView>(R.id.rvReviews)

        tvTitle.text = "Reviews"
        tvEmpty.text = "Loading..."

        val list = mutableListOf<Review>()
        val reviewsAdapter = ReviewsAdapter(list)

        rvReviews.layoutManager = LinearLayoutManager(this)
        rvReviews.adapter = reviewsAdapter

        db.child("reviews").child(providerId)
            .orderByChild("createdAt")
            .limitToLast(20)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    list.clear()
                    for (c in snap.children) {
                        c.getValue(Review::class.java)?.let { list.add(it) }
                    }

                    list.sortByDescending { it.createdAt }
                    reviewsAdapter.notifyDataSetChanged()

                    tvEmpty.text = if (list.isEmpty()) "No reviews yet" else ""
                    tvEmpty.visibility =
                        if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    tvEmpty.text = "Failed to load reviews."
                    tvEmpty.visibility = android.view.View.VISIBLE
                }
            })

        dialog.setContentView(v)
        dialog.show()
    }


    private fun showLeaveReviewDialog(providerId: String) {
        val v = layoutInflater.inflate(R.layout.dialog_leave_review, null)
        val rb = v.findViewById<RatingBar>(R.id.rbLeave)
        val et = v.findViewById<TextInputEditText>(R.id.etComment)

        rb.rating = 5f

        MaterialAlertDialogBuilder(this)
            .setTitle("Complete & Review")
            .setMessage("Rate the provider and mark the task as completed.")
            .setView(v)
            .setNegativeButton("Cancel") { _, _ ->
                analytics.logEvent("client_review_cancel", Bundle().apply {
                    putString("task_id", taskId)
                    putString("provider_id", providerId)
                })
            }
            .setPositiveButton("Submit") { _, _ ->
                val rating = rb.rating.toInt().coerceIn(1, 5)
                val comment = et.text?.toString().orEmpty()


                analytics.logEvent("client_review_submit", Bundle().apply {
                    putString("task_id", taskId)
                    putString("provider_id", providerId)
                    putInt("rating", rating)
                    putInt("comment_len", comment.length)
                })

                submitReview(providerId, rating, comment)
            }
            .show()
    }

    private fun submitReview(providerId: String, rating: Int, comment: String) {
        val clientId = auth.currentUser?.uid
        if (clientId.isNullOrBlank()) return

        // ✅ prevent double review
        db.child("taskReviews").child(taskId).child(clientId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(flagSnap: DataSnapshot) {
                    val already = flagSnap.getValue(Boolean::class.java) == true
                    if (already) {
                        Toast.makeText(
                            this@ClientTaskDetailsActivity,
                            "You already reviewed this task.",
                            Toast.LENGTH_SHORT
                        ).show()
                        btnCompleteReview.visibility = android.view.View.GONE

                        analytics.logEvent("client_review_blocked", Bundle().apply {
                            putString("task_id", taskId)
                            putString("reason", "already_reviewed")
                        })
                        return
                    }

                    val now = System.currentTimeMillis()

                    val reviewRef = db.child("reviews").child(providerId).push()
                    val reviewId = reviewRef.key ?: return

                    val review = Review(
                        clientId = clientId,
                        providerId = providerId,
                        taskId = taskId,
                        orderId = "", // ако сакаш ќе го врземе со orderId подоцна
                        rating = rating,
                        comment = comment,
                        createdAt = now
                    )

                    val updates = hashMapOf<String, Any>(
                        "reviews/$providerId/$reviewId" to review,
                        "taskReviews/$taskId/$clientId" to true,
                        "tasks/$taskId/status" to "completed"
                    )

                    db.updateChildren(updates)
                        .addOnSuccessListener {
                            updateProviderRating(providerId, rating)
                            Toast.makeText(
                                this@ClientTaskDetailsActivity,
                                "✅ Completed & Review submitted",
                                Toast.LENGTH_SHORT
                            ).show()

                            analytics.logEvent("client_task_completed", Bundle().apply {
                                putString("task_id", taskId)
                                putString("provider_id", providerId)
                                putInt("rating", rating)
                            })

                            loadTask()
                            updateCompleteButtonVisibility()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this@ClientTaskDetailsActivity,
                                "❌ Failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()

                            analytics.logEvent("client_task_completed_failed", Bundle().apply {
                                putString("task_id", taskId)
                                putString("provider_id", providerId)
                                putString("reason", e.message ?: "update_failed")
                            })
                        }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateProviderRating(providerId: String, newRating: Int) {
        val ratingRef = db.child("providerRatings").child(providerId)
        ratingRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val avg = currentData.child("avg").getValue(Double::class.java) ?: 0.0
                val count = currentData.child("count").getValue(Long::class.java) ?: 0L

                val newCount = count + 1
                val newAvg = ((avg * count) + newRating.toDouble()) / newCount

                currentData.child("avg").value = newAvg
                currentData.child("count").value = newCount
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {}
        })
    }

    //  ACCEPT + CREATE ORDER
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

        // ✅ track accept click
        analytics.logEvent("client_accept_provider", Bundle().apply {
            putString("task_id", taskId)
            putString("provider_id", app.providerId)
            putString("provider_name", app.providerName)
            putDouble("price", app.price)
        })

        val orderId = db.child("orders").push().key
        if (orderId.isNullOrBlank()) {
            Toast.makeText(this, "❌ Could not create orderId", Toast.LENGTH_LONG).show()
            analytics.logEvent("client_accept_failed", Bundle().apply {
                putString("task_id", taskId)
                putString("provider_id", app.providerId)
                putString("reason", "order_id_null")
            })
            return
        }

        val now = System.currentTimeMillis()

        val updates = hashMapOf<String, Any>(
            "taskApplications/$taskId/${app.providerId}/status" to "accepted",
            "providerApplications/${app.providerId}/$taskId/status" to "accepted",

            "tasks/$taskId/status" to "in_progress",
            "tasks/$taskId/assignedProviderId" to app.providerId,

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

                analytics.logEvent("client_accept_success", Bundle().apply {
                    putString("task_id", taskId)
                    putString("provider_id", app.providerId)
                    putString("order_id", orderId)
                })

                rejectOthers(app.providerId)
                loadTask()
                loadApplications()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Accept/order failed: ${e.message}", Toast.LENGTH_LONG).show()

                analytics.logEvent("client_accept_failed", Bundle().apply {
                    putString("task_id", taskId)
                    putString("provider_id", app.providerId)
                    putString("reason", e.message ?: "update_failed")
                })
            }
    }

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

        // ✅ track decline click
        analytics.logEvent("client_decline_provider", Bundle().apply {
            putString("task_id", taskId)
            putString("provider_id", app.providerId)
        })

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

                analytics.logEvent("client_decline_failed", Bundle().apply {
                    putString("task_id", taskId)
                    putString("provider_id", app.providerId)
                    putString("reason", e.message ?: "update_failed")
                })
            }
    }
}