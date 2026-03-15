package com.example.tasklly.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.data.model.Task
import com.example.tasklly.databinding.ItemRecentTaskBinding

class RecentTasksAdapter(
    private val items: MutableList<Task>,
    private val onClick: (Task) -> Unit
) : RecyclerView.Adapter<RecentTasksAdapter.VH>() {

    inner class VH(val b: ItemRecentTaskBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecentTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val task = items[position]

        holder.b.tvTaskTitle.text = task.title.ifBlank { "Untitled task" }
        holder.b.tvTaskStatus.text = task.status.ifBlank { "Open" }
        holder.b.tvTaskDetails.text = "${task.category} • ${task.location}"
        holder.b.tvTaskBudget.text = "Budget: ${task.budget} "

        holder.b.root.setOnClickListener {
            onClick(task)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Task>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}