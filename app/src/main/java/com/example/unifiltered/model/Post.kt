package com.example.unifiltered.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likedBy: List<String> = emptyList(),
    val commentsCount: Int = 0,
    val imageUrl: String = "",

    val postedAsSocietyId: String? = null,

    @JvmField // THE NUCLEAR FIX: Forces Firebase to use exactly "isOfficialSocietyPost"
    val isOfficialSocietyPost: Boolean = false
)