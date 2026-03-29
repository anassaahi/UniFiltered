package com.example.unifiltered.model

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likedBy: List<String> = emptyList() // NEW: A list of User IDs who liked this post
)