package com.example.waveoffoodadmin


import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.waveoffoodadmin.adapter.OrdersAdapter
import com.example.waveoffoodadmin.model.ItemModel
import com.example.waveoffoodadmin.model.OrderModel

import com.google.firebase.database.*

class NewOrdersActivity : AppCompatActivity() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var ordersAdapter: OrdersAdapter
    private val orderList = mutableListOf<OrderModel>()
    private lateinit var database: DatabaseReference
    private var restaurantKey: String? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_orders)

        rvOrders = findViewById(R.id.rvOrders)
        val backButton = findViewById<ImageView>(R.id.backButton)

        database = FirebaseDatabase.getInstance("https://palatos-6612b-default-rtdb.firebaseio.com/").reference

        restaurantKey = intent.getStringExtra("RESTAURANT_KEY")
        userId = intent.getStringExtra("USER_ID")

        if (restaurantKey.isNullOrEmpty() || userId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Missing restaurant key or user ID!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("RESTAURANT_KEY", restaurantKey)
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
            finish()
        }

        setupRecyclerView()
        fetchNewOrders(restaurantKey!!)
    }

    private fun setupRecyclerView() {
        ordersAdapter = OrdersAdapter(orderList, { orderId ->
            acceptOrder(orderId)
        }, { userId, callback ->
            fetchUserName(userId, callback)
        })
        rvOrders.layoutManager = LinearLayoutManager(this)
        rvOrders.adapter = ordersAdapter
    }

    private fun fetchNewOrders(restaurantKey: String) {
        database.child("restaurants").child(restaurantKey).child("orders")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    orderList.clear()
                    for (orderSnapshot in snapshot.children) {
                        try {
                            val order = orderSnapshot.getValue(OrderModel::class.java)
                            if (order != null && order.action != "accepted") {
                                order.orderId = orderSnapshot.key ?: ""
                                val userId = orderSnapshot.child("userId").getValue(String::class.java) ?: ""
                                fetchWaiterName(
                                    restaurantKey,
                                    order.tableNumber.toString()
                                ) { waiterName ->
                                    order.waiter = waiterName
                                    order.userId = userId

                                    // Parse items
                                    val itemsSnapshot = orderSnapshot.child("items")
                                    val items = mutableListOf<ItemModel>()
                                    for (item in itemsSnapshot.children) {
                                        item.getValue(ItemModel::class.java)?.let { items.add(it) }
                                    }
                                    order.items = items

                                    orderList.add(order)
                                    ordersAdapter.notifyDataSetChanged()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@NewOrdersActivity,
                                "Error parsing order: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    if (orderList.isEmpty()) {
                        Toast.makeText(this@NewOrdersActivity, "No new orders found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@NewOrdersActivity, "Failed to load orders: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun fetchWaiterName(restaurantKey: String, tableNumber: String, callback: (String) -> Unit) {
        database.child("restaurants").child(restaurantKey).child("tables").child("table$tableNumber")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val waiterName = snapshot.child("waiter").getValue(String::class.java) ?: "Unknown Waiter"
                    callback(waiterName)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback("Unknown Waiter")
                }
            })
    }

    private fun fetchUserName(userId: String, callback: (String) -> Unit) {
        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.child("name").getValue(String::class.java) ?: "Unknown User"
                callback(userName)
            }

            override fun onCancelled(error: DatabaseError) {
                callback("Unknown User")
            }
        })
    }

    private fun acceptOrder(orderId: String) {
        database.child("restaurants").child(restaurantKey!!).child("orders").child(orderId).child("action")
            .setValue("accepted")
            .addOnSuccessListener {
                Toast.makeText(this, "Order accepted", Toast.LENGTH_SHORT).show()
                val iterator = orderList.iterator()
                while (iterator.hasNext()) {
                    val order = iterator.next()
                    if (order.orderId == orderId) {
                        iterator.remove()
                        break
                    }
                }
                ordersAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to accept order: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
