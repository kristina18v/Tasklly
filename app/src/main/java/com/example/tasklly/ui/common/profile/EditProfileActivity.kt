package com.example.tasklly.ui.common.profile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tasklly.R
import com.example.tasklly.util.BaseActivity
import com.example.tasklly.util.ThemeManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EditProfileActivity :  BaseActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val etName = findViewById<EditText>(R.id.etName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val swDark = findViewById<Switch>(R.id.swDark)
        val btnSave = findViewById<Button>(R.id.btnSave)

        swDark.isChecked = ThemeManager.isDark(this)

        val uid = auth.currentUser?.uid ?: run { finish(); return }

        db.child("users").child(uid).get().addOnSuccessListener { snap ->
            etName.setText(snap.child("name").getValue(String::class.java) ?: "")
            etPhone.setText(snap.child("phone").getValue(String::class.java) ?: "")
        }

        swDark.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDark(this, isChecked)
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                toast("Пополнете Name и Phone")
                return@setOnClickListener
            }

            val updates = mapOf("name" to name, "phone" to phone)
            db.child("users").child(uid).updateChildren(updates)
                .addOnSuccessListener {
                    toast("Saved ✅")
                    finish()
                }
                .addOnFailureListener { e -> toast(e.message ?: "Save failed") }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
