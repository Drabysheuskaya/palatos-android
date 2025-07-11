package com.example.PalaTos.Model

data class Order(
    val orderId: String = "",
    val userId: String = "",
    var tableNumber: Any? = null,
    var waiter: String = "",
    val timeOfOrder: String = "",
    val items: List<OrderItem> = listOf(),
    val subtotal: String = "",
    val service: String = "",
    val total: String = "",
    var status: String = "Pending",
    var estimatedTime: Int = 0
)

