package com.example.unifiltered.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "student" // Default is student, can also be "admin"
)