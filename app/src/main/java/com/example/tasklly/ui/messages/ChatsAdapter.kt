package com.example.tasklly.ui.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.ChatPreview
import java.text.SimpleDateFormat
import java.util.*

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
        h.tvTime.text = formatTime(c.lastTimestamp)
        h.dotUnread.visibility = if (c.unread) View.VISIBLE else View.GONE

        h.itemView.setOnClickListener { onOpen(c) }
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvLast: TextView = v.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
        val dotUnread: View = v.findViewById(R.id.dotUnread)
    }

    private fun formatTime(ts: Long): String {
        if (ts <= 0L) return ""
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        cal.timeInMillis = ts
        val day = cal.get(Calendar.DAY_OF_YEAR)

        return if (day == today) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
        } else {
            SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(ts))
        }
    }
}