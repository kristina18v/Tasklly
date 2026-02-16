package com.example.tasklly.data.model

data class Message(
    var senderId: String? = "",
    var text: String? = "",
    var timestamp: Long? = 0L
)
