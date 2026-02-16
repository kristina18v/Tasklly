package com.example.tasklly.ui.messages

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.ChatPreview
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var rv: RecyclerView
    private lateinit var adapter: ChatsAdapter

    private var listener: ValueEventListener? = null
    private var queryRef: Query? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvChats)
        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = ChatsAdapter { chat ->
            val otherUid = chat.otherUserId
            if (otherUid.isBlank()) return@ChatsAdapter

            startActivity(
                Intent(requireContext(), ChatActivity::class.java)
                    .putExtra("otherUid", otherUid)
                    .putExtra("otherName", chat.otherName.ifBlank { "Chat" })
            )
        }

        rv.adapter = adapter

        loadMyChats()
    }

    private fun loadMyChats() {
        val uid = auth.currentUser?.uid ?: return

        queryRef = db.child("userChats").child(uid).orderByChild("lastTimestamp")

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatPreview>()

                for (s in snapshot.children) {
                    val c = s.getValue(ChatPreview::class.java) ?: continue

                    // ако chatId не се зачувува во моделот, земи го од key
                    if (c.chatId.isBlank()) c.chatId = s.key ?: ""

                    // ако otherUserId е празен (понекогаш модел/DB mismatch)
                    if (c.otherUserId.isBlank()) {
                        // пробај да го прочиташ од node
                        c.otherUserId = s.child("otherUserId").getValue(String::class.java) ?: ""
                    }
                    if (c.otherName.isBlank()) {
                        c.otherName = s.child("otherName").getValue(String::class.java) ?: "Chat"
                    }
                    if (c.lastMessage.isBlank()) {
                        c.lastMessage = s.child("lastMessage").getValue(String::class.java) ?: ""
                    }
                    if (c.lastTimestamp == null) {
                        c.lastTimestamp = s.child("lastTimestamp").getValue(Long::class.java) ?: 0L
                    }

                    list.add(c)
                }

                // newest first (safe ако е null)
                list.sortByDescending { it.lastTimestamp ?: 0L }

                adapter.submit(list)
            }

            override fun onCancelled(error: DatabaseError) {
                // ако сакаш: Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
        }

        queryRef!!.addValueEventListener(listener as ValueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (listener != null && queryRef != null) {
            queryRef!!.removeEventListener(listener as ValueEventListener)
        }

        listener = null
        queryRef = null
    }
}
