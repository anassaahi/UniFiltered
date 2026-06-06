package com.example.unifiltered.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "student",
    val followedSocieties: List<String> = emptyList(),
    val profileImageUrl: String = "",

    // NEW PROFILE FEATURES
    val coverImageUrl: String = "",
    val bio: String = "",
    val department: String = "BS Computer Science",
    val batch: String = "Class of '26",
    val campusCred: Int = 0 // Our version of Reddit Karma!
)