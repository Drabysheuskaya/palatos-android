package com.example.waveoffoodadmin.model

data class Tip(
    val userName: String = "Unknown",
    val orderId: String = "",
    val totalPrice: String = "",
    val tableNumber: String = "",
    val waiterName: String = "",
    val timeOfOrder: String = "",
    val amount: Int = 0
)
