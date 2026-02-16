package com.example.tasklly.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.tasklly.MainActivity
import com.example.tasklly.R
import com.example.tasklly.databinding.ActivityLoginBinding
import com.example.tasklly.ui.home.ClientHomeActivity
import com.example.tasklly.ui.home.ProviderHomeActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit
import com.example.tasklly.util.BaseActivity


class LoginActivity : BaseActivity() {

    private lateinit var b: ActivityLoginBinding

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    // Phone OTP state
    private var storedVerificationId: String? = null

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            runCatching {
                val account = task.result
                val idToken = account.idToken
                if (idToken == null) {
                    toast("Google token missing (check default_web_client_id)")
                    return@registerForActivityResult
                }
                firebaseAuthWithGoogle(idToken)
            }.onFailure {
                toast(it.message ?: "Google sign-in failed")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Email/Password
        b.btnLogin.setOnClickListener { loginWithEmail() }

        // Google
        b.btnGoogle.setOnClickListener { loginWithGoogle() }

        // Anonymous
        b.btnAnon.setOnClickListener { loginAnonymously() }

        // Phone icon click -> show/hide phone card
        b.btnPhone.setOnClickListener {
            b.phoneCard.visibility =
                if (b.phoneCard.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Phone OTP send/verify
        b.btnPhoneSend.setOnClickListener { sendOtp() }
        b.btnVerifyOtp.setOnClickListener { verifyOtp() }

        // Go register
        b.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginWithEmail() {
        val email = b.etEmail.text.toString().trim()
        val pass = b.etPassword.text.toString().trim()

        if (email.isEmpty() || pass.isEmpty()) {
            toast("Fill email & password")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Invalid email")
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { routeAfterAuth() }
            .addOnFailureListener { e -> toast(e.message ?: "Login failed") }
    }

    private fun loginWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(this, gso)
        client.signOut()
        googleLauncher.launch(client.signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { routeAfterAuth() }
            .addOnFailureListener { e -> toast(e.message ?: "Google auth failed") }
    }

    private fun loginAnonymously() {
        auth.signInAnonymously()
            .addOnSuccessListener { routeAfterAuth() }
            .addOnFailureListener { e -> toast(e.message ?: "Guest login failed") }
    }

    private fun sendOtp() {
        val phone = b.etPhone.text.toString().trim()

        if (phone.isEmpty()) {
            toast("Enter phone: +3897xxxxxxx")
            return
        }
        if (!phone.startsWith("+")) {
            toast("Phone мора да почнува со + (пример +3897xxxxxxx)")
            return
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(phoneCallbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
        toast("Sending OTP...")
    }

    private fun verifyOtp() {
        val code = b.etOtp.text.toString().trim()
        val verificationId = storedVerificationId

        if (verificationId.isNullOrEmpty()) {
            toast("First click Send OTP")
            return
        }
        if (code.length < 4) {
            toast("Enter valid OTP")
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { routeAfterAuth() }
            .addOnFailureListener { e -> toast(e.message ?: "OTP verify failed") }
    }

    private val phoneCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            auth.signInWithCredential(credential)
                .addOnSuccessListener { routeAfterAuth() }
                .addOnFailureListener { e -> toast(e.message ?: "Phone auth failed") }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            toast(e.message ?: "Phone verification failed")
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            storedVerificationId = verificationId
            toast("OTP sent ✅ Enter the code")
        }
    }

    /**
     * После било кој login (email/google/phone/anon) -> овде решаваме каде оди корисникот
     */
    private fun routeAfterAuth() {
        val user = auth.currentUser ?: return

        // Guest -> оди каде што сакаш (јас те праќам на MainActivity)
        if (user.isAnonymous) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val uid = user.uid

        // Читаме role од: users/{uid}/role
        db.child("users").child(uid).child("role").get()
            .addOnSuccessListener { snap ->
                val role = snap.getValue(String::class.java)?.lowercase()

                when (role) {
                    "client" -> {
                        startActivity(Intent(this, ClientHomeActivity::class.java))
                        finish()
                    }
                    "provider" -> {
                        startActivity(Intent(this, ProviderHomeActivity::class.java))
                        finish()
                    }
                    else -> {
                        // Нема role -> креирај basic user record и стави default role
                        createUserIfMissingThenGo(uid, user.email, defaultRole = "client")
                    }
                }
            }
            .addOnFailureListener {
                toast("Не можам да го прочитам role од DB")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }

    private fun createUserIfMissingThenGo(uid: String, email: String?, defaultRole: String) {
        val userMap = hashMapOf<String, Any?>(
            "uid" to uid,
            "email" to (email ?: ""),
            "name" to (email?.substringBefore("@") ?: "User"),
            "phone" to "",
            "role" to defaultRole,
            "createdAt" to System.currentTimeMillis()
        )

        db.child("users").child(uid).setValue(userMap)
            .addOnSuccessListener {
                if (defaultRole == "provider") {
                    startActivity(Intent(this, ProviderHomeActivity::class.java))
                } else {
                    startActivity(Intent(this, ClientHomeActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                toast("Не можам да креирам user во DB")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
