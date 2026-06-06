package com.example.unifiltered.model

data class Comment(
    val commentId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    // NEW: Points to the comment this is replying to (null means it's a root comment)
    val parentId: String? = null
)

// NEW: A wrapper class just for the UI so the adapter knows how far to indent!
data class ThreadedComment(
    val comment: Comment,
    val depth: Int
)