package com.example.tasklly.ui.home.client.post
import android.os.Bundle
import android.widget.Toast
import com.example.tasklly.data.model.Task
import com.example.tasklly.databinding.ActivityPostTaskBinding
import com.example.tasklly.util.BaseActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PostTaskActivity :  BaseActivity() {

    private lateinit var b: ActivityPostTaskBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPostTaskBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnPost.setOnClickListener { postTask() }
    }

    private fun postTask() {
        val user = auth.currentUser
        if (user == null) {
            toast("You must be logged in.")
            return
        }
        if (user.isAnonymous) {
            toast("As a guest, you are not allowed to post tasks.")
            return
        }

        val uid = user.uid

        val title = b.etTitle.text.toString().trim()
        val desc = b.etDesc.text.toString().trim()
        val category = b.etCategory.text.toString().trim()
        val location = b.etLocation.text.toString().trim()
        val budget = b.etBudget.text.toString().trim().toDoubleOrNull()

        if (title.isEmpty() || desc.isEmpty() || category.isEmpty() || location.isEmpty() || budget == null) {
            toast("Please fill in all fields.")
            return
        }

        val taskId = db.child("tasks").push().key ?: run {
            toast("Failed to generate task ID.")
            return
        }

        val task = Task(
            taskId = taskId,
            clientId = uid,
            title = title,
            desc = desc,
            category = category,
            location = location,
            budget = budget,
            status = "open",
            createdAt = System.currentTimeMillis()
        )

        db.child("tasks").child(taskId).setValue(task)
            .addOnSuccessListener {
                toast("The task has been posted successfully.")
                finish()
            }
            .addOnFailureListener { e ->
                toast(e.message ?: "Error while posting the task.")
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
