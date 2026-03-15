package com.example.tasklly.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.tasklly.MainActivity
import com.example.tasklly.R
import com.example.tasklly.databinding.ActivityLoginBinding
import com.example.tasklly.ui.home.ClientHomeActivity
import com.example.tasklly.ui.home.ProviderHomeActivity
import com.example.tasklly.util.BaseActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class LoginActivity : BaseActivity() {

    private lateinit var b: ActivityLoginBinding

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    // ✅ Analytics
    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

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
                    analytics.logEvent("login_failed", Bundle().apply {
                        putString("method", "google")
                        putString("reason", "token_missing")
                    })
                    return@registerForActivityResult
                }
                firebaseAuthWithGoogle(idToken)
            }.onFailure {
                toast(it.message ?: "Google sign-in failed")
                analytics.logEvent("login_failed", Bundle().apply {
                    putString("method", "google")
                    putString("reason", it.message ?: "google_sign_in_failed")
                })
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        // ✅ Track screen open
        analytics.logEvent("screen_open", Bundle().apply {
            putString("screen_name", "Login")
        })

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
            analytics.logEvent("tap_go_register", null)
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginWithEmail() {
        val email = b.etEmail.text.toString().trim()
        val pass = b.etPassword.text.toString().trim()

        analytics.logEvent("login_attempt", Bundle().apply {
            putString("method", "email")
        })

        if (email.isEmpty() || pass.isEmpty()) {
            toast("Fill email & password")
            analytics.logEvent("login_failed", Bundle().apply {
                putString("method", "email")
                putString("reason", "empty_fields")
            })
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Invalid email")
            analytics.logEvent("login_failed", Bundle().apply {
                putString("method", "email")
                putString("reason", "invalid_email")
            })
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                analytics.logEvent("login_success", Bundle().apply {
                    putString("method", "email")
                })
                routeAfterAuth()
            }
            .addOnFailureListener { e ->
                toast(e.message ?: "Login failed")
                analytics.logEvent("login_failed", Bundle().apply {
                    putString("method", "email")
                    putString("reason", e.message ?: "auth_failed")
                })
            }
    }

    private fun loginWithGoogle() {
        analytics.logEvent("login_attempt", Bundle().apply {
            putString("method", "google")
        })

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
            .addOnSuccessListener {
                analytics.logEvent("login_success", Bundle().apply {
                    putString("method", "google")
                })
                routeAfterAuth()
            }
            .addOnFailureListener { e ->
                toast(e.message ?: "Google auth failed")
                analytics.logEvent("login_failed", Bundle().apply {
                    putString("method", "google")
                    putString("reason", e.message ?: "google_auth_failed")
                })
            }
    }

    private fun loginAnonymously() {
        analytics.logEvent("login_attempt", Bundle().apply {
            putString("method", "anonymous")
        })

        auth.signInAnonymously()
            .addOnSuccessListener {
                analytics.logEvent("login_success", Bundle().apply {
                    putString("method", "anonymous")
                })
                routeAfterAuth()
            }
            .addOnFailureListener { e ->
                toast(e.message ?: "Guest login failed")
                analytics.logEvent("login_failed", Bundle().apply {
                    putString("method", "anonymous")
                    putString("reason", e.message ?: "anon_failed")
                })
            }
    }

    private fun sendOtp() {
        val phone = b.etPhone.text.toString().trim()

        analytics.logEvent("otp_send_attempt", null)

        if (phone.isEmpty()) {
            toast("Enter phone: +3897xxxxxxx")
            analytics.logEvent("otp_send_failed", Bundle().apply {
                putString("reason", "empty_phone")
            })
            return
        }
        if (!phone.startsWith("+")) {
            toast("Phone мора да почнува со + (пример +3897xxxxxxx)")
            analytics.logEvent("otp_send_failed", Bundle().apply {
                putString("reason", "missing_plus")
            })
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

        analytics.logEvent("otp_verify_attempt", null)

        if (verificationId.isNullOrEmpty()) {
            toast("First click Send OTP")
            analytics.logEvent("otp_verify_failed", Bundle().apply {
                putString("reason", "no_verification_id")
            })
            return
        }
        if (code.length < 4) {
            toast("Enter valid OTP")
            analytics.logEvent("otp_verify_failed", Bundle().apply {
                putString("reason", "short_code")
            })
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                analytics.logEvent("login_success", Bundle().apply {
                    putString("method", "phone")
                })
                routeAfterAuth()
            }
            .addOnFailureListener { e ->
                toast(e.message ?: "OTP verify failed")
                analytics.logEvent("login_failed", Bundle().apply {
                    putString("method", "phone")
                    putString("reason", e.message ?: "otp_failed")
                })
            }
    }

    private val phoneCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            auth.signInWithCredential(credential)
                .addOnSuccessListener {
                    analytics.logEvent("login_success", Bundle().apply {
                        putString("method", "phone_auto")
                    })
                    routeAfterAuth()
                }
                .addOnFailureListener { e ->
                    toast(e.message ?: "Phone auth failed")
                    analytics.logEvent("login_failed", Bundle().apply {
                        putString("method", "phone_auto")
                        putString("reason", e.message ?: "phone_auth_failed")
                    })
                }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            toast(e.message ?: "Phone verification failed")
            analytics.logEvent("otp_send_failed", Bundle().apply {
                putString("reason", e.message ?: "verification_failed")
            })
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            storedVerificationId = verificationId
            toast("OTP sent ✅ Enter the code")
            analytics.logEvent("otp_sent", null)
        }
    }

    /**
     * После било кој login (email/google/phone/anon) -> овде решаваме каде оди корисникот
     */
    private fun routeAfterAuth() {
        val user = auth.currentUser ?: return

        // Guest
        if (user.isAnonymous) {
            analytics.logEvent("route_role", Bundle().apply {
                putString("role", "anonymous")
            })
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val uid = user.uid

        // users/{uid}/role
        db.child("users").child(uid).child("role").get()
            .addOnSuccessListener { snap ->
                val role = snap.getValue(String::class.java)?.lowercase()

                when (role) {
                    "client" -> {
                        analytics.logEvent("route_role", Bundle().apply { putString("role", "client") })
                        startActivity(Intent(this, ClientHomeActivity::class.java))
                        finish()
                    }
                    "provider" -> {
                        analytics.logEvent("route_role", Bundle().apply { putString("role", "provider") })
                        startActivity(Intent(this, ProviderHomeActivity::class.java))
                        finish()
                    }
                    else -> {
                        analytics.logEvent("route_role", Bundle().apply { putString("role", "unknown") })
                        createUserIfMissingThenGo(uid, user.email, defaultRole = "client")
                    }
                }
            }
            .addOnFailureListener {
                toast("Не можам да го прочитам role од DB")
                analytics.logEvent("route_role", Bundle().apply { putString("role", "read_failed") })
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
                analytics.logEvent("user_created", Bundle().apply {
                    putString("role", defaultRole)
                })

                if (defaultRole == "provider") {
                    startActivity(Intent(this, ProviderHomeActivity::class.java))
                } else {
                    startActivity(Intent(this, ClientHomeActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                toast("Не можам да креирам user во DB")
                analytics.logEvent("user_create_failed", null)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}