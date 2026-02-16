package com.example.tasklly.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.Task
import com.google.android.material.button.MaterialButton

class ProviderTasksAdapter(
    private var items: List<Task>,
    private val onView: (Task) -> Unit,
    private val onApply: (Task) -> Unit,
    private val isApplied: (taskId: String) -> Boolean
) : RecyclerView.Adapter<ProviderTasksAdapter.VH>() {

    fun submitList(newItems: List<Task>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_provider_task, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]

        holder.tvTitle.text = t.title
        holder.tvBudget.text = "${t.budget.toInt()} €"
        holder.tvMeta.text = "${t.category} • ${t.location}"

        holder.btnView.setOnClickListener { onView(t) }

        // ✅ користи taskId (Firebase key)
        val applied = isApplied(t.taskId)

        holder.btnApply.isEnabled = !applied
        holder.btnApply.text = if (applied) "Applied" else "Apply"
        holder.btnApply.setOnClickListener {
            if (!applied) onApply(t)
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvBudget: TextView = v.findViewById(R.id.tvBudget)
        val tvMeta: TextView = v.findViewById(R.id.tvMeta)
        val btnView: MaterialButton = v.findViewById(R.id.btnView)
        val btnApply: MaterialButton = v.findViewById(R.id.btnApply)
    }
}
