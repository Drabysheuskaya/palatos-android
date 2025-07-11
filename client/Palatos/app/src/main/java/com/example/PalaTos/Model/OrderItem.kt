package com.example.PalaTos.Model

data class OrderItem(
    val name: String = "",
    val price: String = "",
    val quantity: Int = 0,
    val estimatedTime: Int? = 0
)
