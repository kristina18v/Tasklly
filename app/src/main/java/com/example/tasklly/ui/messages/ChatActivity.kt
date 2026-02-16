package com.example.tasklly.ui.messages

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.Message
import com.example.tasklly.util.chatIdFor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ChatActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var rv: RecyclerView
    private lateinit var et: EditText
    private lateinit var btn: Button
    private lateinit var tvTitle: TextView

    private val adapter by lazy { MessagesAdapter(auth.currentUser?.uid ?: "") }

    private var otherUid: String = ""
    private var otherName: String = "Chat"
    private var myUid: String = ""
    private var chatId: String = ""

    // ✅ auto-message
    private var autoMessage: String = ""
    private var autoSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        myUid = auth.currentUser?.uid ?: run { finish(); return }

        otherUid = intent.getStringExtra("otherUid") ?: run { finish(); return }
        otherName = intent.getStringExtra("otherName")?.ifBlank { "Chat" } ?: "Chat"

        autoMessage = intent.getStringExtra("autoMessage")?.trim().orEmpty()

        chatId = chatIdFor(myUid, otherUid)

        tvTitle = findViewById(R.id.tvChatTitle)
        rv = findViewById(R.id.rvMessages)
        et = findViewById(R.id.etMessage)
        btn = findViewById(R.id.btnSend)

        tvTitle.text = otherName

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        // 1) ensure chat exists + participants BEFORE reading/writing messages
        ensureChatParticipants {
            // 2) start listening
            listenMessages()

            // 3) ✅ send autoMessage once (if provided)
            if (!autoSent && autoMessage.isNotBlank()) {
                autoSent = true
                sendMessage(autoMessage)
            }
        }

        btn.setOnClickListener {
            val text = et.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            sendMessage(text)
            et.setText("")
        }
    }

    private fun ensureChatParticipants(onDone: () -> Unit) {
        val updates = hashMapOf<String, Any>(
            "/chats/$chatId/participants/$myUid" to true,
            "/chats/$chatId/participants/$otherUid" to true
        )

        db.updateChildren(updates)
            .addOnSuccessListener { onDone() }
            .addOnFailureListener {
                Toast.makeText(this, "Chat init failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun listenMessages() {
        db.child("messages").child(chatId)
            .orderByChild("timestamp")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val list = mutableListOf<Message>()
                    for (s in snapshot.children) {
                        s.getValue(Message::class.java)?.let { list.add(it) }
                    }
                    adapter.submit(list)
                    if (list.isNotEmpty()) rv.scrollToPosition(list.size - 1)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Listen failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun sendMessage(text: String) {
        val ts = System.currentTimeMillis()
        val msgKey = db.child("messages").child(chatId).push().key ?: return

        val msg = Message(
            senderId = myUid,
            text = text,
            timestamp = ts
        )

        val updates = hashMapOf<String, Any>(
            // message
            "/messages/$chatId/$msgKey" to msg,

            // chat last message
            "/chats/$chatId/lastMessage" to text,
            "/chats/$chatId/lastTimestamp" to ts,

            // userChats for ME (show other)
            "/userChats/$myUid/$chatId/otherUserId" to otherUid,
            "/userChats/$myUid/$chatId/otherName" to otherName,
            "/userChats/$myUid/$chatId/lastMessage" to text,
            "/userChats/$myUid/$chatId/lastTimestamp" to ts,

            // userChats for OTHER (show me) - ако немаш мое име, оставаме “User”
            "/userChats/$otherUid/$chatId/otherUserId" to myUid,
            "/userChats/$otherUid/$chatId/otherName" to "User",
            "/userChats/$otherUid/$chatId/lastMessage" to text,
            "/userChats/$otherUid/$chatId/lastTimestamp" to ts
        )

        db.updateChildren(updates)
            .addOnFailureListener {
                Toast.makeText(this, "Send failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
