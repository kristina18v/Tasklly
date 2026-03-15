package com.example.tasklly.data.model

data class ChatPreview(
    var chatId: String = "",
    var otherUserId: String = "",
    var otherName: String = "",
    var lastMessage: String = "",
    var lastTimestamp: Long = 0L,
    var unread: Boolean = false
)