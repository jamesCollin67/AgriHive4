package com.example.agrihive.addapiary

data class Apiary(
    val id: String = "",
    val name: String = "",
    val temperature: String = "",
    val humidity: String = "",
    val weight: String = "",
    val isActive: Boolean = true,
    val ownerId: String = ""
)