package com.example.jobtown.data

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "seeker", // "seeker" or "company"
    val phone: String = "",
    val industry: String? = null,
    val skills: String? = null,
    val bio: String = ""
)