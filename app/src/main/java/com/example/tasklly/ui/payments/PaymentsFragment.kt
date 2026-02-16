package com.example.tasklly.ui.payments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasklly.R
import com.example.tasklly.data.model.Order
import com.example.tasklly.databinding.FragmentPaymentsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PaymentsFragment : Fragment(R.layout.fragment_payments) {

    private var _b: FragmentPaymentsBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private val items = mutableListOf<Order>()
    private lateinit var adapter: PaymentsAdapter

    private var listener: ValueEventListener? = null
    private var queryRef: Query? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _b = FragmentPaymentsBinding.bind(view)

        adapter = PaymentsAdapter(items) { order ->
            startActivity(
                Intent(requireContext(), PaymentActivity::class.java)
                    .putExtra("orderId", order.orderId)
                    .putExtra("paymentUrl", order.paymentUrl)
            )
        }

        b.rvPayments.layoutManager = LinearLayoutManager(requireContext())
        b.rvPayments.adapter = adapter

        loadMyOrders()
    }

    private fun loadMyOrders() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ IMPORTANT: query само за мои orders (инаку rules ќе блокираат)
        queryRef = db.child("orders")
            .orderByChild("clientId")
            .equalTo(uid)

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                items.clear()

                for (s in snapshot.children) {
                    val o = s.getValue(Order::class.java) ?: continue
                    if (o.orderId.isBlank()) o.orderId = s.key ?: ""

                    val st = o.status.trim().lowercase()
                    if (st == "pending") items.add(o)
                }

                items.sortByDescending { it.createdAt }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Load failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        queryRef!!.addValueEventListener(listener as ValueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (listener != null && queryRef != null) {
            queryRef!!.removeEventListener(listener as ValueEventListener)
        }
        listener = null
        queryRef = null
        _b = null
    }
}
