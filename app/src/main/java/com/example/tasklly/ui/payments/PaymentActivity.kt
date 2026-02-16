package com.example.tasklly.ui.payments

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.example.tasklly.databinding.ActivityPaymentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PaymentActivity : AppCompatActivity() {

    private lateinit var b: ActivityPaymentBinding
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var orderId: String
    private lateinit var paymentUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(b.root)

        orderId = intent.getStringExtra("orderId") ?: run { finish(); return }
        paymentUrl = intent.getStringExtra("paymentUrl") ?: run { finish(); return }

        // Ако сакаш: прочитај amount од order и прикажи
        db.child("orders").child(orderId).child("amount").get().addOnSuccessListener {
            val amount = it.getValue(Double::class.java) ?: 0.0
            b.tvAmount.text = "Amount: €%.2f".format(amount)
        }

        b.btnPayNow.setOnClickListener {
            val first = b.etFirstName.text.toString().trim()
            val last = b.etLastName.text.toString().trim()

            if (first.isBlank() || last.isBlank()) {
                Toast.makeText(this, "Внеси име и презиме", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // запиши ги во order (не е картичка, безбедно е)
            val updates = mapOf(
                "payerFirstName" to first,
                "payerLastName" to last
            )

            db.child("orders").child(orderId).updateChildren(updates)
                .addOnSuccessListener { openStripe(paymentUrl) }
                .addOnFailureListener { Toast.makeText(this, it.message ?: "Error", Toast.LENGTH_SHORT).show() }
        }

        b.btnIHavePaid.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val updates = mapOf<String, Any>(
                "/orders/$orderId/status" to "paid",
                "/orders/$orderId/paidAt" to System.currentTimeMillis(),
                "/orders/$orderId/paidBy" to uid
            )
            db.updateChildren(updates)
                .addOnSuccessListener { finish() }
                .addOnFailureListener { Toast.makeText(this, it.message ?: "Error", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun openStripe(url: String) {
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(this, Uri.parse(url))
    }
}
