package com.example.tasklly.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tasklly.databinding.ActivityRegisterBinding
import com.example.tasklly.util.BaseActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity :  BaseActivity() {

    private lateinit var b: ActivityRegisterBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnRegister.setOnClickListener { register() }

        b.tvGoLogin.setOnClickListener {
            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun register() {
        val name = b.etName.text.toString().trim()
        val phone = b.etPhone.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        val pass = b.etPassword.text.toString().trim()
        val pass2 = b.etPassword2.text.toString().trim()

        val role = when (b.rgRole.checkedRadioButtonId) {
            b.rbClient.id -> "client"
            b.rbProvider.id -> "provider"
            else -> ""
        }

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty() || pass2.isEmpty()) {
            toast("Пополнете ги сите полиња")
            return
        }
        if (role.isEmpty()) {
            toast("Избери дали си Client или Provider")
            return
        }
        if (pass.length < 6) {
            toast("Password минимум 6 карактери")
            return
        }
        if (pass != pass2) {
            toast("Password не се исти")
            return
        }

        b.btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    b.btnRegister.isEnabled = true
                    toast("Нема UID, пробај пак")
                    return@addOnSuccessListener
                }

                val userMap = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "phone" to phone,
                    "email" to email,
                    "role" to role, // ✅ client / provider
                    "createdAt" to System.currentTimeMillis()
                )

                // users/{uid} во Realtime Database
                db.child("users").child(uid).setValue(userMap)
                    .addOnSuccessListener {
                        toast("Успешна регистрација ✅")
                        // ✅ директно на Login
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        b.btnRegister.isEnabled = true
                        toast(e.message ?: "DB save failed")
                    }
            }
            .addOnFailureListener { e ->
                b.btnRegister.isEnabled = true
                toast(e.message ?: "Register failed")
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
