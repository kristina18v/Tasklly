package com.example.tasklly.data.model

data class Task(
    var taskId: String = "",
    var clientId: String = "",
    var clientName: String = "",
    var title: String = "",
    var desc: String = "",
    var category: String = "",
    var location: String = "",
    var budget: Double = 0.0,
    var status: String = "open", // open, in_progress, completed, cancelled
    var assignedProviderId: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var completedAt: Long = 0L,
    val id: String = "",
)