package com.example.tasklly.data.model

data class Review(
    var clientId: String = "",
    var providerId: String = "",
    var taskId: String = "",
    var orderId: String = "",
    var rating: Int = 0,
    var comment: String = "",
    var createdAt: Long = 0L
)