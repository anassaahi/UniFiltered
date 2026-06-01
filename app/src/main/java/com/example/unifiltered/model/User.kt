package com.example.unifiltered.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "student",
    val followedSocieties: List<String> = emptyList(),
    val profileImageUrl: String = ""
)