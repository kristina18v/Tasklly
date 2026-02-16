package com.example.tasklly.ui.home.provider.task

import android.os.Bundle
import android.widget.*
import com.example.tasklly.R
import com.example.tasklly.data.model.Application
import com.example.tasklly.data.model.Task
import com.example.tasklly.util.BaseActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProviderTaskDetailsActivity : BaseActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private var taskId: String = ""

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

        db.child("tasks").child(taskId).get().addOnSuccessListener { snap ->
            val t = snap.getValue(Task::class.java) ?: return@addOnSuccessListener
            t.taskId = snap.key ?: taskId

            tvTitle.text = t.title
            tvDesc.text = t.desc  // ✅ важно (не description)

            if (t.status != "open") {
                btnApply.isEnabled = false
                btnApply.text = "Not available"
            }
        }.addOnFailureListener {
            toast("Load failed: ${it.message}")
        }

        btnApply.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener

            val price = etPrice.text.toString().trim().toDoubleOrNull()
            val msg = etMsg.text.toString().trim()

            if (price == null || price <= 0.0) { toast("Enter valid price"); return@setOnClickListener }
            if (msg.isEmpty()) { toast("Enter message"); return@setOnClickListener }

            // provider name from users/{uid}/name (fallback на email)
            db.child("users").child(uid).child("name").get().addOnSuccessListener { s ->
                val providerName = s.getValue(String::class.java)
                    ?.trim()
                    ?.ifBlank { auth.currentUser?.email ?: "Provider" }
                    ?: (auth.currentUser?.email ?: "Provider")

                val app = Application(
                    taskId = taskId,
                    providerId = uid,
                    providerName = providerName,
                    message = msg,
                    price = price,
                    status = "pending",
                    createdAt = System.currentTimeMillis()
                )

                val updates = hashMapOf<String, Any>(
                    // ✅ ОВА мора да го чита ClientTaskDetailsActivity
                    "/taskApplications/$taskId/$uid" to app,

                    // ✅ provider “My applications”
                    "/providerApplications/$uid/$taskId" to app

                    // ако сакаш да задржиш стар пат:
                    // "/applications/$taskId/$uid" to app
                )

                db.updateChildren(updates)
                    .addOnSuccessListener {
                        toast("Applied ✅")
                        finish()
                    }
                    .addOnFailureListener { e ->
                        toast(e.message ?: "Apply failed")
                    }
            }.addOnFailureListener { e ->
                toast("Cannot load provider name: ${e.message}")
            }
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
