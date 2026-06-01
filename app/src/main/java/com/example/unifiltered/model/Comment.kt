package com.example.unifiltered.model

data class Comment(
    val commentId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)