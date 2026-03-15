package com.example.tasklly.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.data.model.Application
import com.example.tasklly.databinding.ItemApplicationBinding
import com.google.firebase.database.*

class ApplicationsAdapter(
    private val items: MutableList<Application>,
    private val showActions: Boolean,
    private val onAccept: (Application) -> Unit,
    private val onDecline: (Application) -> Unit,
    private val onMessage: (Application) -> Unit,
    private val onViewReviews: (providerId: String) -> Unit   // ✅ НОВО
) : RecyclerView.Adapter<ApplicationsAdapter.VH>() {

    private val db: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }

    // ✅ КЕШ: providerId -> (avg, count)
    private val ratingCache = mutableMapOf<String, Pair<Double, Long>>()

    inner class VH(val b: ItemApplicationBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemApplicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, @SuppressLint("RecyclerView") position: Int) {
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
        holder.b.btnMessage.setOnClickListener { onMessage(a) }

        // ✅ REVIEWS BUTTON
        holder.b.btnViewReviews.setOnClickListener {
            if (a.providerId.isNotBlank()) onViewReviews(a.providerId)
        }

        // ✅ RATING UI DEFAULT
        holder.b.rbProvider.rating = 0f
        holder.b.tvRatingInfo.text = "No reviews yet"

        val providerId = a.providerId
        if (providerId.isBlank()) return

        // ✅ ако го имаме во кеш – стави и не читај Firebase пак
        ratingCache[providerId]?.let { (avg, count) ->
            bindRating(holder, avg, count)
            return
        }


        db.child("providerRatings").child(providerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val avg = s.child("avg").getValue(Double::class.java) ?: 0.0
                    val count = s.child("count").getValue(Long::class.java) ?: 0L

                    ratingCache[providerId] = avg to count


                    if (holder.bindingAdapterPosition == position) {
                        bindRating(holder, avg, count)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun bindRating(holder: VH, avg: Double, count: Long) {
        holder.b.rbProvider.rating = avg.toFloat()
        holder.b.tvRatingInfo.text =
            if (count <= 0L) "No reviews yet" else String.format("%.1f (%d)", avg, count)
    }

    override fun getItemCount(): Int = items.size

    fun setData(newItems: List<Application>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}