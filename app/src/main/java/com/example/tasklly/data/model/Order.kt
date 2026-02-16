package com.example.tasklly.data.model

data class Order(
    var orderId: String = "",
    var offerId: String = "",
    var taskId: String = "",
    var clientId: String = "",
    var providerId: String = "",
    var providerName: String = "",
    var taskTitle: String = "",
    var amount: Double = 0.0,
    var currency: String = "EUR",
    var status: String = "pending",
    var payerFirstName: String = "",
    var payerLastName: String = "",
    var paymentUrl: String = "",
    var createdAt: Long = 0L,
    var paidAt: Long? = null
)
