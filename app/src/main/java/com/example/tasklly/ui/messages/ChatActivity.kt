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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class ChatActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    // ✅ Analytics
    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    private lateinit var rv: RecyclerView
    private lateinit var et: EditText
    private lateinit var btn: Button
    private lateinit var tvTitle: TextView

    private val adapter by lazy { MessagesAdapter(auth.currentUser?.uid ?: "") }

    private var otherUid: String = ""
    private var otherName: String = "Chat"
    private var myUid: String = ""
    private var chatId: String = ""

    private var autoMessage: String = ""
    private var autoSent = false

    private var myDisplayName: String = "User"

    // extras (optional, but useful for analytics)
    private var source: String = ""
    private var taskId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        myUid = auth.currentUser?.uid ?: run { finish(); return }

        otherUid = intent.getStringExtra("otherUid") ?: run { finish(); return }
        otherName = intent.getStringExtra("otherName")?.ifBlank { "Chat" } ?: "Chat"
        autoMessage = intent.getStringExtra("autoMessage")?.trim().orEmpty()

        // optional
        source = intent.getStringExtra("source").orEmpty()
        taskId = intent.getStringExtra("taskId").orEmpty()

        chatId = chatIdFor(myUid, otherUid)

        tvTitle = findViewById(R.id.tvChatTitle)
        rv = findViewById(R.id.rvMessages)
        et = findViewById(R.id.etMessage)
        btn = findViewById(R.id.btnSend)

        tvTitle.text = otherName

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        // ✅ chat_open event
        analytics.logEvent("chat_open", Bundle().apply {
            putString("chat_id", chatId)
            putString("my_uid", myUid)
            putString("other_uid", otherUid)
            putString("source", source)
            if (taskId.isNotBlank()) putString("task_id", taskId)
        })

        loadMyName {
            ensureChatParticipants {
                markChatReadForMe()
                listenMessages()

                if (!autoSent && autoMessage.isNotBlank()) {
                    autoSent = true
                    analytics.logEvent("chat_auto_message_trigger", Bundle().apply {
                        putString("chat_id", chatId)
                        putString("source", source)
                    })
                    sendMessage(autoMessage, isAuto = true)
                }
            }
        }

        btn.setOnClickListener {
            val text = et.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            sendMessage(text, isAuto = false)
            et.setText("")
        }
    }

    private fun loadMyName(onDone: () -> Unit) {
        db.child("users").child(myUid).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val n = s.getValue(String::class.java).orEmpty().trim()
                    if (n.isNotBlank()) myDisplayName = n
                    onDone()
                }
                override fun onCancelled(error: DatabaseError) { onDone() }
            })
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
                analytics.logEvent("chat_init_failed", Bundle().apply {
                    putString("chat_id", chatId)
                    putString("reason", it.message ?: "init_failed")
                })
            }
    }

    private fun markChatReadForMe() {
        db.child("userChats").child(myUid).child(chatId).child("unread")
            .setValue(false)
    }

    private fun listenMessages() {
        db.child("messages").child(chatId)
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Message>()
                    for (s in snapshot.children) {
                        s.getValue(Message::class.java)?.let { list.add(it) }
                    }
                    adapter.submit(list)
                    if (list.isNotEmpty()) rv.scrollToPosition(list.size - 1)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Listen failed: ${error.message}", Toast.LENGTH_LONG).show()
                    analytics.logEvent("chat_listen_failed", Bundle().apply {
                        putString("chat_id", chatId)
                        putString("reason", error.message)
                    })
                }
            })
    }

    private fun sendMessage(text: String, isAuto: Boolean) {
        val ts = System.currentTimeMillis()
        val msgKey = db.child("messages").child(chatId).push().key ?: return

        // ✅ message_send event (log before DB)
        analytics.logEvent("message_send", Bundle().apply {
            putString("chat_id", chatId)
            putString("source", source)
            putBoolean("auto", isAuto)
            putInt("len", text.length)
            if (taskId.isNotBlank()) putString("task_id", taskId)
        })

        val msg = Message(
            senderId = myUid,
            text = text,
            timestamp = ts
        )

        val updates = hashMapOf<String, Any>(
            "/messages/$chatId/$msgKey" to msg,
            "/chats/$chatId/lastMessage" to text,
            "/chats/$chatId/lastTimestamp" to ts,

            "/userChats/$myUid/$chatId/otherUserId" to otherUid,
            "/userChats/$myUid/$chatId/otherName" to otherName,
            "/userChats/$myUid/$chatId/lastMessage" to text,
            "/userChats/$myUid/$chatId/lastTimestamp" to ts,
            "/userChats/$myUid/$chatId/unread" to false,

            "/userChats/$otherUid/$chatId/otherUserId" to myUid,
            "/userChats/$otherUid/$chatId/otherName" to myDisplayName,
            "/userChats/$otherUid/$chatId/lastMessage" to text,
            "/userChats/$otherUid/$chatId/lastTimestamp" to ts,
            "/userChats/$otherUid/$chatId/unread" to true
        )

        db.updateChildren(updates)
            .addOnFailureListener {
                Toast.makeText(this, "Send failed: ${it.message}", Toast.LENGTH_LONG).show()
                analytics.logEvent("message_send_failed", Bundle().apply {
                    putString("chat_id", chatId)
                    putString("reason", it.message ?: "send_failed")
                })
            }
    }
}