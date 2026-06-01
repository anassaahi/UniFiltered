package com.example.unifiltered.model

data class VerificationRequest(
    val requestId: String = "",
    val societyId: String = "",
    val societyName: String = "",
    val requesterId: String = "",
    val contactInfo: String = "",
    val proofOrReason: String = "",
    val status: String = "pending", // Can be "pending", "approved", or "rejected"
    val timestamp: Long = System.currentTimeMillis()
)