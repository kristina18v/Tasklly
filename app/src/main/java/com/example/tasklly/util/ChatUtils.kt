package com.example.tasklly.util

fun chatIdFor(a: String, b: String): String {
    return if (a < b) "${a}_$b" else "${b}_$a"
}
