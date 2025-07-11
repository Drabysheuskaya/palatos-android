package com.example.waveoffoodadmin.model

data class OrderModel(
    var orderId: String = "",               // Unique ID for the order
    var userId: String = "",                // ID of the user who placed the order
    var tableNumber: Any? = null,           // Table number (optional)
    var waiter: String = "",                // Assigned waiter name
    var items: List<ItemModel> = listOf(),  // List of items in the order
    var subtotal: String = "",              // Subtotal amount as a string (e.g., "111.00")
    var service: String = "",               // Service charge (e.g., "11.10")
    var total: String = "",                 // Total amount (e.g., "122.10")
    var timeOfOrder: String = "",           // Order time in "HH:mm:ss dd.MM.yyyy" format
    var estimatedTime: Int? = null,         // Estimated preparation time in minutes
    var status: String = "",                // Status of the order (e.g., "Paid")
    var action: String = "",                // Current action (e.g., "Accepted")
    var tip: Double? = null,                // Tip amount (optional)
    var progress_status: String = ""

)


data class ItemModel(
    val name: String = "",                  // Name of the item
    val quantity: Int = 0,                  // Quantity of the item
    val price: String = "",                 // Price of the item as a string (e.g., "44 PLN")
    val estimatedTime: Int = 0        // Estimated preparation time in minutes (optional)
)