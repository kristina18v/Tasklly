package com.example.tasklly.ui.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.Message

class MessagesAdapter(private val myUid: String) :
    RecyclerView.Adapter<MessagesAdapter.VH>() {

    private val items = mutableListOf<Message>()

    companion object {
        const val TYPE_ME = 1
        const val TYPE_OTHER = 0
    }

    fun submit(list: List<Message>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].senderId == myUid) TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {

        val layout = if (viewType == TYPE_ME) {
            R.layout.item_message_me
        } else {
            R.layout.item_message_other
        }

        val v = LayoutInflater.from(parent.context)
            .inflate(layout, parent, false)

        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.tv.text = m.text ?: ""
    }

    override fun getItemCount() = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.tvMsg)
    }
}