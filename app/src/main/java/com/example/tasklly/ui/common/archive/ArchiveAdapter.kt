package com.example.tasklly.ui.common.archive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.Task

class ArchiveAdapter(
    private val mode: String, // "client" или "provider"
    private val items: MutableList<Pair<Task, String>> // task + otherUserName
) : RecyclerView.Adapter<ArchiveAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvTaskTitle)
        val tvOther: TextView = v.findViewById(R.id.tvOtherUser)
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_archive_task, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (task, otherName) = items[position]
        holder.tvTitle.text = task.title
        holder.tvOther.text = if (mode == "client") "Worked with: $otherName" else "Client: $otherName"
        holder.tvPrice.text = "Price: ${task.budget.toInt()} €"

    }

    override fun getItemCount() = items.size
}