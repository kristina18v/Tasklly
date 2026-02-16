package com.example.tasklly.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.Application
import com.example.tasklly.data.model.Task
import com.example.tasklly.ui.messages.ChatActivity
import com.google.android.material.button.MaterialButton

class ClientOffersAdapter(
    private val items: MutableList<Pair<Task, Application>>,
    private val onAccept: (Task, Application) -> Unit,
    private val onDecline: (Task, Application) -> Unit,
    private val onPay: ((Task, Application) -> Unit)? = null // ✅ optional
) : RecyclerView.Adapter<ClientOffersAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = v.findViewById(R.id.tvSubtitle)
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)

        val btnAccept: Button = v.findViewById(R.id.btnAccept)
        val btnDecline: Button = v.findViewById(R.id.btnDecline)
        val btnMessage: MaterialButton = v.findViewById(R.id.btnMessage)

        // ✅ ако го немаш во layout, ќе биде null
        val btnPay: MaterialButton? = v.findViewById(R.id.btnPay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client_offer, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val (task, app) = items[position]
        val ctx = h.itemView.context

        h.tvTitle.text = task.title.ifBlank { "Task" }
        h.tvSubtitle.text = app.message.ifBlank { "No message" }
        h.tvPrice.text = "Price: €%.2f".format(app.price)

        // ✅ статус логика
        val status = app.status.lowercase().trim()
        val isPending = status == "pending"
        val isAccepted = status == "accepted"
        val isRejected = status == "rejected"

        // ✅ Accept/Decline активни само ако е pending
        h.btnAccept.isEnabled = isPending
        h.btnDecline.isEnabled = isPending
        h.btnAccept.alpha = if (isPending) 1f else 0.5f
        h.btnDecline.alpha = if (isPending) 1f else 0.5f

        // ✅ Click Accept (со debug toast)
        h.btnAccept.setOnClickListener {
            Toast.makeText(ctx, "ACCEPT CLICKED: ${task.title}", Toast.LENGTH_SHORT).show()
            onAccept(task, app)
        }

        // ✅ Click Decline (со debug toast)
        h.btnDecline.setOnClickListener {
            Toast.makeText(ctx, "DECLINE CLICKED: ${task.title}", Toast.LENGTH_SHORT).show()
            onDecline(task, app)
        }

        // ✅ Pay button (само ако е accepted и ако е даден callback + ако постои во layout)
        h.btnPay?.apply {
            visibility = if (isAccepted && onPay != null) View.VISIBLE else View.GONE
            isEnabled = isAccepted && onPay != null
            text = "Pay"
            setOnClickListener {
                Toast.makeText(ctx, "OPEN PAYMENT: ${task.title}", Toast.LENGTH_SHORT).show()
                onPay?.invoke(task, app)
            }
        }

        // ✅ Chat
        h.btnMessage.setOnClickListener {
            val otherUid = app.providerId
            if (otherUid.isBlank()) return@setOnClickListener

            ctx.startActivity(
                Intent(ctx, ChatActivity::class.java)
                    .putExtra("otherUid", otherUid)
                    .putExtra("otherName", app.providerName.ifBlank { "Provider" })
            )
        }
    }
}
