package com.example.tasklly.ui.home.provider.task

import android.os.Bundle
import android.widget.*
import com.example.tasklly.R
import com.example.tasklly.data.model.Application
import com.example.tasklly.data.model.Message
import com.example.tasklly.data.model.Task
import com.example.tasklly.util.BaseActivity
import com.example.tasklly.util.chatIdFor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProviderTaskDetailsActivity : BaseActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private var taskId: String = ""
    private var currentTask: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider_task_details)

        taskId = intent.getStringExtra("taskId") ?: ""
        if (taskId.isBlank()) { finish(); return }

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvDesc = findViewById<TextView>(R.id.tvDesc)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val etMsg = findViewById<EditText>(R.id.etMsg)
        val btnApply = findViewById<Button>(R.id.btnApply)

        // Load task
        db.child("tasks").child(taskId).get()
            .addOnSuccessListener { snap ->
                val t = snap.getValue(Task::class.java) ?: return@addOnSuccessListener
                t.taskId = snap.key ?: taskId
                currentTask = t

                tvTitle.text = t.title
                tvDesc.text = t.desc

                if (t.status != "open") {
                    btnApply.isEnabled = false
                    btnApply.text = "Not available"
                }
            }
            .addOnFailureListener {
                toast("Load failed: ${it.message}")
            }

        btnApply.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val task = currentTask ?: run { toast("Task not loaded yet"); return@setOnClickListener }

            val price = etPrice.text.toString().trim().toDoubleOrNull()
            val msg = etMsg.text.toString().trim()

            if (price == null || price <= 0.0) { toast("Enter valid price"); return@setOnClickListener }
            if (msg.isEmpty()) { toast("Enter message"); return@setOnClickListener }

            // provider name from users/{uid}/name (fallback на email)
            db.child("users").child(uid).child("name").get()
                .addOnSuccessListener { s ->
                    val providerName = s.getValue(String::class.java)
                        ?.trim()
                        ?.ifBlank { auth.currentUser?.email ?: "Provider" }
                        ?: (auth.currentUser?.email ?: "Provider")

                    // ✅ Client info (мора Task да има clientId; ако немаш, кажи ќе ти кажам како да го запишуваш при post)
                    val clientId = task.clientId
                    val clientName = task.clientName.ifBlank { "Client" }

                    if (clientId.isBlank()) {
                        toast("Task missing clientId")
                        return@addOnSuccessListener
                    }

                    val now = System.currentTimeMillis()

                    val app = Application(
                        taskId = taskId,
                        clientId = clientId,
                        clientName = clientName,
                        providerId = uid,
                        providerName = providerName,
                        message = msg,
                        price = price,
                        status = "pending",
                        createdAt = now
                    )

                    val updates = hashMapOf<String, Any>(
                        "/taskApplications/$taskId/$uid" to app,
                        "/providerApplications/$uid/$taskId" to app
                    )

                    db.updateChildren(updates)
                        .addOnSuccessListener {
                            // ✅ Auto-send message so it appears in Messages tab for client
                            val autoText = "Hi $clientName! I applied for \"${
                                task.title
                            }\". My price is €${price.toInt()}.\n$msg"

                            sendApplicationChatMessage(
                                providerId = uid,
                                providerName = providerName,
                                clientId = clientId,
                                clientName = clientName,
                                text = autoText
                            )

                            toast("Applied ✅")
                            finish()
                        }
                        .addOnFailureListener { e ->
                            toast(e.message ?: "Apply failed")
                        }
                }
                .addOnFailureListener { e ->
                    toast("Cannot load provider name: ${e.message}")
                }
        }
    }

    private fun sendApplicationChatMessage(
        providerId: String,
        providerName: String,
        clientId: String,
        clientName: String,
        text: String
    ) {
        val chatId = chatIdFor(providerId, clientId)
        val ts = System.currentTimeMillis()
        val msgKey = db.child("messages").child(chatId).push().key ?: return

        val msg = Message(
            senderId = providerId,
            text = text,
            timestamp = ts
        )

        val updates = hashMapOf<String, Any>(
            // ensure chat participants
            "/chats/$chatId/participants/$providerId" to true,
            "/chats/$chatId/participants/$clientId" to true,

            // message
            "/messages/$chatId/$msgKey" to msg,

            // chat last
            "/chats/$chatId/lastMessage" to text,
            "/chats/$chatId/lastTimestamp" to ts,

            // inbox for client (unread true)
            "/userChats/$clientId/$chatId/otherUserId" to providerId,
            "/userChats/$clientId/$chatId/otherName" to providerName,
            "/userChats/$clientId/$chatId/lastMessage" to text,
            "/userChats/$clientId/$chatId/lastTimestamp" to ts,
            "/userChats/$clientId/$chatId/unread" to true,

            // inbox for provider (unread false)
            "/userChats/$providerId/$chatId/otherUserId" to clientId,
            "/userChats/$providerId/$chatId/otherName" to clientName,
            "/userChats/$providerId/$chatId/lastMessage" to text,
            "/userChats/$providerId/$chatId/lastTimestamp" to ts,
            "/userChats/$providerId/$chatId/unread" to false
        )

        db.updateChildren(updates)
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}