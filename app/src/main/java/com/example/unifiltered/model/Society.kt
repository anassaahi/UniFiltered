package com.example.unifiltered.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Society(
    val societyId: String = "",
    val name: String = "",
    val description: String = "",
    val creatorId: String = "",

    @JvmField // THE NUCLEAR FIX: Forces Firebase to use exactly "isOfficial"
    val isOfficial: Boolean = false,

    val followerCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)