package com.example.waveoffoodadmin

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.waveoffoodadmin.adapter.OrderInfoAdapter
import com.example.waveoffoodadmin.model.ItemModel
import com.example.waveoffoodadmin.model.OrderModel
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class OrderInfoActivity : AppCompatActivity() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var ordersAdapter: OrderInfoAdapter
    private val orderList = mutableListOf<OrderModel>()
    private lateinit var database: DatabaseReference
    private var restaurantKey: String? = null
    private var selectedDate: String = ""
    private val userCache = mutableMapOf<String, String>() // Cache for user names
    private val waiterCache = mutableMapOf<String, String>() // Cache for waiter names
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvTotalOrders: TextView
    private lateinit var spinnerFilter: Spinner


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_info)

        rvOrders = findViewById(R.id.rvOrders)
        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnPickDate = findViewById<Button>(R.id.btnPickDate)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvTotalOrders = findViewById(R.id.tvTotalOrders)
        spinnerFilter = findViewById(R.id.spinnerFilter)


        database = FirebaseDatabase.getInstance("https://palatos-6612b-default-rtdb.firebaseio.com/").reference
        restaurantKey = intent.getStringExtra("RESTAURANT_KEY")

        if (restaurantKey.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Missing restaurant key!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        backButton.setOnClickListener { finish() }

        setupRecyclerView()

        btnPickDate.setOnClickListener {
            showDatePicker()
        }
        setupSpinner()
    }

    private fun setupRecyclerView() {
        ordersAdapter = OrderInfoAdapter(orderList, { userId, callback ->
            fetchUserName(userId, callback)
        }, { order ->
            updateOrderStatus(order)
        }, restaurantKey!!) // Pass restaurantKey here


        rvOrders.layoutManager = LinearLayoutManager(this)
        rvOrders.adapter = ordersAdapter
    }



    @SuppressLint("SetTextI18n")
    private fun showDatePicker() {
        val calendar = Calendar.getInstance() // Current date
        val maxDate = calendar.timeInMillis // Get the current date in milliseconds

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                selectedDate = sdf.format(calendar.time)
                tvSelectedDate.text = "Selected Date: $selectedDate"

                // Reset spinner to "Day"
                spinnerFilter.setSelection(0)

                // Automatically fetch orders for the selected day
                fetchOrdersByDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = maxDate // Set the max date to today's date
            show()
        }
    }



    private fun setupSpinner() {
        val filterOptions = listOf("Day", "Month")

        // Create an ArrayAdapter using the custom layout
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_white_text, // Custom layout for spinner text (white)
            filterOptions
        )

        // Set the dropdown view to use the same custom layout
        adapter.setDropDownViewResource(R.layout.spinner_item_white_text)

        // Attach the adapter to the spinner
        spinnerFilter.adapter = adapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedFilter = filterOptions[position]
                if (selectedFilter == "Day") {
                    if (selectedDate.isNotEmpty()) {
                        fetchOrdersByDate(selectedDate)
                    } else {
                        Toast.makeText(this@OrderInfoActivity, "Please select a date first.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (selectedDate.isNotEmpty()) {
                        fetchOrdersByMonth()
                    } else {
                        Toast.makeText(this@OrderInfoActivity, "Please select a date first.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }




    private fun fetchOrdersByDate(date: String) {
        tvSelectedDate.text = "Selected Date: $date" // Ensure the date is displayed
        orderList.clear() // Clear the previous orders
        ordersAdapter.notifyDataSetChanged()
        tvTotalOrders.text = "Total Orders: 0"

        database.child("restaurants")
            .child(restaurantKey!!)
            .child("orders")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@OrderInfoActivity, "No orders found for $date", Toast.LENGTH_SHORT).show()
                        return
                    }

                    var totalOrders = 0 // Track the number of orders

                    for (orderSnapshot in snapshot.children) {
                        val timeOfOrder = orderSnapshot.child("timeOfOrder").getValue(String::class.java)
                        val action = orderSnapshot.child("action").getValue(String::class.java)

                        if (timeOfOrder != null && timeOfOrder.contains(date) && action == "accepted") {
                            val order = orderSnapshot.getValue(OrderModel::class.java)
                            if (order != null) {
                                order.orderId = orderSnapshot.key ?: ""
                                val tableNumber = order.tableNumber.toString()

                                // Fetch waiter name and update order list
                                fetchWaiterName(tableNumber) { waiterName ->
                                    order.waiter = waiterName
                                    orderList.add(order)
                                    ordersAdapter.notifyDataSetChanged()
                                }
                                totalOrders++
                            }
                        }
                    }

                    tvTotalOrders.text = "Total Orders: $totalOrders"
                    if (totalOrders == 0) {
                        Toast.makeText(this@OrderInfoActivity, "No orders found for $date", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@OrderInfoActivity, "Failed to fetch orders: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }



    private fun fetchOrdersByMonth() {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date first.", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse the selected date and set the start of the month
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val selectedDateParsed = sdf.parse(selectedDate)!!
        calendar.time = selectedDateParsed
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = sdf.format(calendar.time) // Start of the month

        // Clear existing data
        orderList.clear()
        ordersAdapter.run { notifyDataSetChanged() }
        tvTotalOrders.text = "Total Orders: 0"

        database.child("restaurants")
            .child(restaurantKey!!)
            .child("orders")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@OrderInfoActivity, "No orders found for the selected month.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    var totalOrders = 0
                    for (orderSnapshot in snapshot.children) {
                        val timeOfOrder = orderSnapshot.child("timeOfOrder").getValue(String::class.java)
                        val action = orderSnapshot.child("action").getValue(String::class.java)

                        if (timeOfOrder != null && action == "accepted") {
                            try {
                                // Extract the date part from the timeOfOrder string
                                val orderDatePart = timeOfOrder.split(" ")[1] // "05.01.2025"
                                val orderDate = sdf.parse(orderDatePart)!!

                                // Check if the order date is within the range
                                if (orderDate in sdf.parse(startOfMonth)!!..selectedDateParsed) {
                                    val order = orderSnapshot.getValue(OrderModel::class.java)
                                    if (order != null) {
                                        order.orderId = orderSnapshot.key ?: ""
                                        val tableNumber = order.tableNumber.toString()

                                        // Fetch waiter name and update the order list
                                        fetchWaiterName(tableNumber) { waiterName ->
                                            order.waiter = waiterName
                                            orderList.add(order)
                                            ordersAdapter.notifyDataSetChanged()
                                        }
                                        totalOrders++
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@OrderInfoActivity,
                                    "Error parsing order dates: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    tvTotalOrders.text = "Total Orders: $totalOrders"
                    if (totalOrders == 0) {
                        Toast.makeText(this@OrderInfoActivity, "No orders found for the selected range.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@OrderInfoActivity, "Failed to fetch orders: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun fetchUserName(userId: String, callback: (String) -> Unit) {
        if (userCache.containsKey(userId)) {
            callback(userCache[userId]!!)
        } else {
            database.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userName = snapshot.child("name").value?.toString() ?: "Unknown User"
                        userCache[userId] = userName
                        callback(userName)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback("Unknown User")
                    }
                })
        }
    }

    private fun fetchWaiterName(tableNumber: String, callback: (String) -> Unit) {
        if (waiterCache.containsKey(tableNumber)) {
            callback(waiterCache[tableNumber]!!)
        } else {
            database.child("restaurants").child(restaurantKey!!).child("tables").child("table$tableNumber")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val waiterName = snapshot.child("waiter").value?.toString() ?: "Unknown Waiter"
                        waiterCache[tableNumber] = waiterName
                        callback(waiterName)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback("Unknown Waiter")
                    }
                })
        }
    }



    private fun updateOrderStatus(order: OrderModel) {
        val orderRef = database.child("restaurants").child(restaurantKey!!).child("orders").child(order.orderId)

        orderRef.child("status").setValue(order.status)
        orderRef.child("progress_status").setValue(order.progress_status)
            .addOnSuccessListener {
                Toast.makeText(this, "Order updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update order: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }



}

