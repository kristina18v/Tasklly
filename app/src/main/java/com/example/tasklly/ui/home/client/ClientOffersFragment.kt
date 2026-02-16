package com.example.tasklly.ui.home.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasklly.R
import com.example.tasklly.data.model.Application
import com.example.tasklly.data.model.Task
import com.example.tasklly.databinding.FragmentListBinding
import com.example.tasklly.ui.adapter.ClientOffersAdapter
import com.example.tasklly.ui.payments.PaymentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ClientOffersFragment : Fragment(R.layout.fragment_list) {

    private var _b: FragmentListBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private val items = mutableListOf<Pair<Task, Application>>()
    private lateinit var adapter: ClientOffersAdapter

    // ✅ твој Stripe payment link
    private val paymentUrl = "https://buy.stripe.com/test_14A6oH2734jMa3ib918N200"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentListBinding.bind(view)

        b.etSearch.hint = "Search offers..."

        adapter = ClientOffersAdapter(
            items = items,
            onAccept = { task, app -> acceptAndCreateOrder(task, app) },
            onDecline = { task, app -> declineOffer(task, app) }
        )

        b.rv.layoutManager = LinearLayoutManager(requireContext())
        b.rv.adapter = adapter

        loadOffers()
    }

    private fun loadOffers() {
        val uid = auth.currentUser?.uid ?: return

        db.child("tasks")
            .orderByChild("clientId")
            .equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(tasksSnap: DataSnapshot) {
                    items.clear()

                    val tasks = tasksSnap.children.mapNotNull { it.getValue(Task::class.java) }

                    for (t in tasks) {
                        db.child("taskApplications").child(t.taskId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(appsSnap: DataSnapshot) {
                                    for (a in appsSnap.children) {
                                        val app = a.getValue(Application::class.java) ?: continue

                                        // ✅ Прикажи pending + accepted (ако сакаш само pending -> тргни accepted)
                                        if (app.status == "pending" || app.status == "accepted") {
                                            items.add(t to app)
                                        }
                                    }
                                    adapter.notifyDataSetChanged()
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    /**
     * ✅ Accept offer + create Order (so da se pojavi vo Payments tab)
     * ✅ Ne pravi duplikat order ako veke postoi za taskId + providerId
     */
    private fun acceptAndCreateOrder(task: Task, app: Application) {
        val clientId = auth.currentUser?.uid ?: return

        // 1) accept updates
        val updates = hashMapOf<String, Any>(
            "taskApplications/${task.taskId}/${app.providerId}/status" to "accepted",
            "tasks/${task.taskId}/status" to "in_progress",
            "tasks/${task.taskId}/assignedProviderId" to app.providerId,
            "providerApplications/${app.providerId}/${task.taskId}/status" to "accepted"
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                // 2) Before creating order, check if already exists (avoid duplicates)
                checkExistingOrderAndCreate(task, app, clientId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "❌ Accept failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkExistingOrderAndCreate(task: Task, app: Application, clientId: String) {
        // бараме дали веќе има order за овој client + task + provider
        db.child("orders")
            .orderByChild("taskId")
            .equalTo(task.taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    // ако најдеме ист providerId и clientId -> не креирај нов, само отвори PaymentActivity
                    for (s in snapshot.children) {
                        val providerId = s.child("providerId").getValue(String::class.java) ?: ""
                        val cId = s.child("clientId").getValue(String::class.java) ?: ""
                        val existingOrderId = s.key ?: ""

                        if (providerId == app.providerId && cId == clientId && existingOrderId.isNotBlank()) {
                            Toast.makeText(requireContext(), "Order already exists", Toast.LENGTH_SHORT).show()
                            openPayment(existingOrderId)
                            return
                        }
                    }

                    // 3) ако не постои, креирај
                    createOrder(task, app, clientId)
                }

                override fun onCancelled(error: DatabaseError) {
                    // ако check падне, сепак пробај да креираш
                    createOrder(task, app, clientId)
                }
            })
    }

    private fun createOrder(task: Task, app: Application, clientId: String) {
        val orderId = db.child("orders").push().key
        if (orderId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Could not create orderId", Toast.LENGTH_LONG).show()
            return
        }

        val order = hashMapOf(
            "orderId" to orderId,
            "taskId" to task.taskId,
            "clientId" to clientId,
            "providerId" to app.providerId,
            "providerName" to app.providerName,
            "taskTitle" to task.title,
            "amount" to app.price,
            "currency" to "EUR",
            "status" to "pending",
            "paymentUrl" to paymentUrl,
            "createdAt" to System.currentTimeMillis()
        )

        db.child("orders").child(orderId).setValue(order)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✅ Order created", Toast.LENGTH_SHORT).show()
                openPayment(orderId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "❌ Order failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun openPayment(orderId: String) {
        startActivity(
            Intent(requireContext(), PaymentActivity::class.java)
                .putExtra("orderId", orderId)
                .putExtra("paymentUrl", paymentUrl)
        )
    }

    private fun declineOffer(task: Task, app: Application) {
        val updates = hashMapOf<String, Any>(
            "taskApplications/${task.taskId}/${app.providerId}/status" to "rejected",
            "providerApplications/${app.providerId}/${task.taskId}/status" to "rejected"
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Offer rejected", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "❌ Decline failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
