package com.example.agrihive.model

data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val farm: String = "",
    val location: String = "",
    val apiaries: Int = 0
)