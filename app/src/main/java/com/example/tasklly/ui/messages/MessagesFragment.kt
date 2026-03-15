package com.example.tasklly.ui.messages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
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
    private lateinit var emptyContainer: LinearLayout
    private lateinit var adapter: ChatsAdapter

    private var listener: ValueEventListener? = null
    private var queryRef: Query? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvChats)
        emptyContainer = view.findViewById(R.id.emptyContainer)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(true)

        adapter = ChatsAdapter { chat ->
            val otherUid = chat.otherUserId
            if (otherUid.isBlank()) return@ChatsAdapter

            // open chat
            startActivity(
                Intent(requireContext(), ChatActivity::class.java)
                    .putExtra("otherUid", otherUid)
                    .putExtra("otherName", chat.otherName.ifBlank { "Chat" })
            )

            // mark read
            val myUid = auth.currentUser?.uid ?: return@ChatsAdapter
            if (chat.chatId.isNotBlank()) {
                db.child("userChats").child(myUid).child(chat.chatId).child("unread").setValue(false)
            }
        }

        rv.adapter = adapter
        loadMyChats()
    }

    private fun loadMyChats() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            showEmpty(true)
            return
        }

        Log.d("MSG", "uid=$uid")

        // remove old listener
        if (listener != null && queryRef != null) {
            queryRef!!.removeEventListener(listener as ValueEventListener)
        }

        queryRef = db.child("userChats").child(uid).orderByChild("lastTimestamp")

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("MSG", "children=${snapshot.childrenCount}")

                val list = mutableListOf<ChatPreview>()

                for (s in snapshot.children) {
                    val c = s.getValue(ChatPreview::class.java) ?: continue

                    if (c.chatId.isBlank()) c.chatId = s.key ?: ""
                    if (c.otherUserId.isBlank()) c.otherUserId =
                        s.child("otherUserId").getValue(String::class.java) ?: ""

                    if (c.otherName.isBlank()) c.otherName =
                        s.child("otherName").getValue(String::class.java) ?: "Chat"

                    if (c.lastMessage.isBlank()) c.lastMessage =
                        s.child("lastMessage").getValue(String::class.java) ?: ""

                    c.lastTimestamp = s.child("lastTimestamp").getValue(Long::class.java) ?: c.lastTimestamp
                    c.unread = s.child("unread").getValue(Boolean::class.java) ?: c.unread

                    list.add(c)
                }

                // newest first
                list.sortByDescending { it.lastTimestamp }

                adapter.submit(list)
                showEmpty(list.isEmpty())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MSG", "Load chats failed: ${error.message}")
                Toast.makeText(requireContext(), "Load chats failed: ${error.message}", Toast.LENGTH_SHORT).show()
                showEmpty(true)
            }
        }

        queryRef!!.addValueEventListener(listener as ValueEventListener)
    }

    private fun showEmpty(show: Boolean) {
        emptyContainer.visibility = if (show) View.VISIBLE else View.GONE
        rv.visibility = if (show) View.GONE else View.VISIBLE
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