package com.example.tasklly.ui.payments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.Order
import com.google.android.material.button.MaterialButton

class PaymentsAdapter(
    private val items: List<Order>,
    private val onPay: (Order) -> Unit
) : RecyclerView.Adapter<PaymentsAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvProvider: TextView = v.findViewById(R.id.tvProvider)
        val tvTask: TextView = v.findViewById(R.id.tvTask)
        val tvAmount: TextView = v.findViewById(R.id.tvAmount)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
        val btnPay: MaterialButton = v.findViewById(R.id.btnPay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_payment, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val o = items[position]

        h.tvProvider.text = o.providerName.ifBlank { "Provider" }
        h.tvTask.text = o.taskTitle.ifBlank { "Task" }
        h.tvAmount.text = "€%.2f".format(o.amount)
        h.tvStatus.text = "Status: ${o.status}"

        // Ако е paid, disable Pay
        val isPaid = o.status == "paid"
        h.btnPay.isEnabled = !isPaid
        h.btnPay.text = if (isPaid) "Paid" else "Pay"

        h.btnPay.setOnClickListener { onPay(o) }
    }
}
