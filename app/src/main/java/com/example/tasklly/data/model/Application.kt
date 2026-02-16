package com.example.tasklly.data.model

data class Application(
    var taskId: String = "",
    var clientId: String = "",
    var clientName: String = "",
    var providerId: String = "",
    var providerName: String = "",
    var message: String = "",
    var price: Double = 0.0,
    var status: String = "pending",   // pending / accepted / rejected
    var createdAt: Long = 0L,
    var id: String = ""

)
