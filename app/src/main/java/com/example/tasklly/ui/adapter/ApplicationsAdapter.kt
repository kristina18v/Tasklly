package com.example.tasklly.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.data.model.Application
import com.example.tasklly.databinding.ItemApplicationBinding

class ApplicationsAdapter(
    private val items: MutableList<Application>,
    private val showActions: Boolean,
    private val onAccept: (Application) -> Unit,
    private val onDecline: (Application) -> Unit,
    private val onMessage: (Application) -> Unit   // ✅ ДОДАДЕНО
) : RecyclerView.Adapter<ApplicationsAdapter.VH>() {

    inner class VH(val b: ItemApplicationBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemApplicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = items[position]

        // Provider name
        val name = a.providerName.takeIf { it.isNotBlank() } ?: a.providerId
        holder.b.tvProvider.text = name

        // Price + status
        val priceText = "€${a.price}"
        val statusText = a.status.ifBlank { "pending" }
        holder.b.tvInfo.text = "$priceText • ${statusText.replaceFirstChar { it.uppercase() }}"

        holder.b.tvMsg.text = a.message.ifBlank { "No message." }
        holder.b.tvBadge.text = statusText.uppercase()

        // Actions
        val canAct = showActions && statusText.lowercase() == "pending"
        holder.b.actions.visibility = if (canAct) View.VISIBLE else View.GONE

        holder.b.btnAccept.setOnClickListener { onAccept(a) }
        holder.b.btnDecline.setOnClickListener { onDecline(a) }

        // ✅ MESSAGE BUTTON
        holder.b.btnMessage.setOnClickListener {
            onMessage(a)
        }
    }

    override fun getItemCount(): Int = items.size

    fun setData(newItems: List<Application>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
