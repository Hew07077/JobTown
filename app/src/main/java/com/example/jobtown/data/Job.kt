package com.example.jobtown.data

data class Job(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val salary: String,
    val salaryRange: String = salary, // Alias for compatibility
    val type: String,
    val description: String,
    val requirements: List<String> = emptyList(),
    val skills: List<String> = emptyList()
)