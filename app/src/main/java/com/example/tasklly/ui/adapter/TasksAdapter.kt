package com.example.tasklly.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.data.model.Task
import com.example.tasklly.databinding.ItemTaskBinding

class TasksAdapter(
    private val items: MutableList<Task>,
    private val onClick: (Task) -> Unit
) : RecyclerView.Adapter<TasksAdapter.VH>() {

    inner class VH(val b: ItemTaskBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.b.tvTitle.text = t.title
        holder.b.tvMeta.text = "${t.category} • ${t.location} • ${t.status}"
        holder.b.root.setOnClickListener { onClick(t) }
    }

    override fun getItemCount() = items.size
}
