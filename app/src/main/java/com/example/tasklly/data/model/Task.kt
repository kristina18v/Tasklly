package com.example.tasklly.data.model

data class Task(
    var taskId: String = "",          // Firebase key
    var clientId: String = "",

    var title: String = "",
    var desc: String = "",

    var category: String = "",
    var location: String = "",
    var budget: Double = 0.0,

    // status: open, in_progress, completed
    var status: String = "open",

    // кога клиент ќе прифати провајдер
    var assignedProviderId: String? = null,

    var createdAt: Long = System.currentTimeMillis()
)
