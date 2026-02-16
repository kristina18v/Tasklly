package com.example.tasklly.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tasklly.databinding.ActivityPhoneAuthBinding
import com.example.tasklly.util.BaseActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class PhoneAuthActivity :  BaseActivity() {

    private lateinit var b: ActivityPhoneAuthBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityPhoneAuthBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.groupCode.visibility = View.GONE

        b.btnSendCode.setOnClickListener { sendCode() }
        b.btnVerify.setOnClickListener { verifyCode() }
    }

    private fun sendCode() {
        val phone = b.etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            toast("Внеси телефон со +код (пример +389...)")
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                toast(e.message ?: "Verification failed")
            }

            override fun onCodeSent(
                vid: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = vid
                toast("Кодот е испратен ✅")
                b.groupCode.visibility = View.VISIBLE
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode() {
        val code = b.etCode.text.toString().trim()
        val vid = verificationId

        if (vid == null) {
            toast("Прво прати код")
            return
        }
        if (code.length < 4) {
            toast("Внеси валиден код")
            return
        }

        val credential = PhoneAuthProvider.getCredential(vid, code)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                toast("Телефон логин ✅")
                finish()
            }
            .addOnFailureListener { e ->
                toast(e.message ?: "Phone sign-in failed")
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
