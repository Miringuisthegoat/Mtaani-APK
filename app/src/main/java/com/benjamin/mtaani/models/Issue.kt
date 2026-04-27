package com.benjamin.mtaani.models

data class Issue(
    val id: String = "",
    val uid: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val status: String = "Reported",
    val upvotes: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val location: String = "",
    val photoUrl: String = "",
    val severity: String = "Medium",
    val emailSent: Boolean = false
)
