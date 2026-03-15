package com.example.tasklly.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.data.model.Review
import com.example.tasklly.databinding.ItemReviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewsAdapter(
    private val items: MutableList<Review>
) : RecyclerView.Adapter<ReviewsAdapter.VH>() {

    inner class VH(val b: ItemReviewBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]

        holder.b.tvStars.text = stars(r.rating)
        holder.b.tvComment.text = if (r.comment.isBlank()) "No comment." else r.comment
        holder.b.tvDate.text = formatDate(r.createdAt)
    }

    override fun getItemCount(): Int = items.size

    private fun stars(rating: Int): String {
        val clamped = rating.coerceIn(0, 5)
        return "★".repeat(clamped) + "☆".repeat(5 - clamped)
    }

    @SuppressLint("SimpleDateFormat")
    private fun formatDate(ts: Long): String {
        if (ts <= 0L) return ""
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(ts))
    }
}