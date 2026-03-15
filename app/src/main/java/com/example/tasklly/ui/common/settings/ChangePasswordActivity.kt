package com.example.tasklly.ui.common.settings
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.example.tasklly.R
import com.example.tasklly.util.BaseActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : BaseActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val etCurrentPassword = findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)

        btnBack.setOnClickListener { finish() }

        btnChangePassword.setOnClickListener {
            val currentPassword = etCurrentPassword.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (currentPassword.isEmpty()) {
                etCurrentPassword.error = "Enter current password"
                etCurrentPassword.requestFocus()
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                etNewPassword.error = "Enter new password"
                etNewPassword.requestFocus()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                etNewPassword.error = "Password must be at least 6 characters"
                etNewPassword.requestFocus()
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                etConfirmPassword.error = "Confirm your new password"
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (currentPassword == newPassword) {
                etNewPassword.error = "New password must be different"
                etNewPassword.requestFocus()
                return@setOnClickListener
            }

            changePassword(currentPassword, newPassword)
        }
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser

        if (user == null) {
            toast("User not logged in")
            return
        }

        val email = user.email
        if (email.isNullOrBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Only email/password accounts can change password here")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        toast("Password changed successfully")
                        finish()
                    }
                    .addOnFailureListener { e ->
                        toast(e.message ?: "Failed to change password")
                    }
            }
            .addOnFailureListener {
                toast("Current password is incorrect")
            }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}