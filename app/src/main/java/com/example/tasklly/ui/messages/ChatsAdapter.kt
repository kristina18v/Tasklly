package com.example.tasklly.ui.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.ChatPreview

class ChatsAdapter(
    private val onOpen: (ChatPreview) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.VH>() {

    private val items = mutableListOf<ChatPreview>()

    fun submit(list: List<ChatPreview>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_preview, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val c = items[position]

        h.tvName.text = c.otherName.ifBlank { "Chat" }
        h.tvLast.text = c.lastMessage.ifBlank { "No messages yet" }

        h.itemView.setOnClickListener {
            onOpen(c)
        }
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvLast: TextView = v.findViewById(R.id.tvLastMessage)
    }
}

