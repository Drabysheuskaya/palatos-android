package com.example.waveoffoodadmin

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.waveoffoodadmin.adapter.TotalSumAdapter
import com.example.waveoffoodadmin.databinding.ActivityTotalSumBinding
import com.example.waveoffoodadmin.model.OrderModel
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class TotalSumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTotalSumBinding
    private val orderList = mutableListOf<OrderModel>()
    private lateinit var database: DatabaseReference
    private var restaurantKey: String? = null
    private val userCache = mutableMapOf<String, String>()
    private val waiterCache = mutableMapOf<String, String>()
    private var selectedDate: String = ""
    private var selectedPeriod: String = "Day"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTotalSumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restaurantKey = intent.getStringExtra("RESTAURANT_KEY")
        if (restaurantKey.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Missing restaurant key!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().reference
        setupRecyclerView()
        setupSpinner()
        binding.backButton.setOnClickListener { finish() }
        binding.selectDateButton.setOnClickListener { showDatePicker() }
    }

    private fun setupRecyclerView() {
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = TotalSumAdapter(
            orders = orderList,
            fetchUserName = { userId, callback -> fetchUserName(userId, callback) },
            fetchWaiterName = { tableNumber, callback -> fetchWaiterName(tableNumber, callback) },
            restaurantKey = restaurantKey!!
        )
    }

    private fun setupSpinner() {
        val periods = listOf("Day", "Month", "Year")


        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_white_text,
            periods
        )
        adapter.setDropDownViewResource(R.layout.spinner_item_white_text)


        binding.spinnerPeriod.adapter = adapter

        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPeriod = periods[position]
                when (selectedPeriod) {
                    "Day" -> fetchOrdersByDate(selectedDate)
                    "Month" -> fetchOrdersByMonth()
                    "Year" -> fetchOrdersByYear()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    @SuppressLint("SetTextI18n")
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                selectedDate = sdf.format(calendar.time)
                binding.tvSelectedDate.text = "Selected Date: $selectedDate"
                fetchOrdersByDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }

    private fun fetchOrdersByDate(date: String) {
        if (date.isEmpty()) {
            Log.e("TotalSumActivity", "Selected date is empty!")
            return
        }

        orderList.clear()
        binding.rvOrders.adapter?.notifyDataSetChanged()
        Log.d("TotalSumActivity", "Fetching orders for date: $date")

        database.child("restaurants").child(restaurantKey!!).child("orders")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d("TotalSumActivity", "No orders found for restaurantKey: $restaurantKey")
                        Toast.makeText(this@TotalSumActivity, "No orders found for $date", Toast.LENGTH_SHORT).show()
                        return
                    }

                    var totalAmount = 0.0
                    for (orderSnapshot in snapshot.children) {
                        val timeOfOrder = orderSnapshot.child("timeOfOrder").getValue(String::class.java)
                        val action = orderSnapshot.child("action").getValue(String::class.java)
                        val progressStatus = orderSnapshot.child("progress_status").getValue(String::class.java)
                        val status = orderSnapshot.child("status").getValue(String::class.java)
                        val total = orderSnapshot.child("total").getValue(String::class.java)?.toDoubleOrNull()

                        // Ignore orders if progress_status or status fields are missing
                        if (progressStatus == null || status == null) {
                            Log.d("OrderCheck", "Skipping order due to missing fields: progress_status=$progressStatus, status=$status")
                            continue
                        }

                        // Filter orders based on action, progress_status, and status
                        if (timeOfOrder != null && action == "accepted" &&
                            progressStatus == "Delivered" && status == "Received"
                        ) {
                            val orderDate = timeOfOrder.split(" ")[1] // Extract "dd.MM.yyyy"
                            if (orderDate == date) {
                                val order = orderSnapshot.getValue(OrderModel::class.java)
                                if (order != null) {
                                    Log.d("OrderCheck", "Adding order: ${order.orderId}")
                                    orderList.add(order)
                                    totalAmount += total ?: 0.0
                                }
                            }
                        }
                    }

                    binding.rvOrders.adapter?.notifyDataSetChanged()
                    binding.tvTotalOrders.text = "Total Orders: ${orderList.size}"
                    binding.tvTotalAmount.text = "Total Amount: %.2f PLN".format(totalAmount)

                    if (orderList.isEmpty()) {
                        Toast.makeText(this@TotalSumActivity, "No orders found for $date", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TotalSumActivity", "Database error: ${error.message}")
                    Toast.makeText(this@TotalSumActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }





    private fun fetchOrdersByMonth() {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date first.", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val selectedDateParsed = try {
            sdf.parse(selectedDate)
        } catch (e: Exception) {
            Log.e("TotalSumActivity", "Invalid selected date: $selectedDate", e)
            null
        }

        if (selectedDateParsed == null) {
            Toast.makeText(this, "Invalid selected date.", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance()
        calendar.time = selectedDateParsed
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = sdf.format(calendar.time)

        orderList.clear()
        binding.rvOrders.adapter?.notifyDataSetChanged()
        binding.tvTotalOrders.text = "Total Orders: 0"

        database.child("restaurants").child(restaurantKey!!).child("orders")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@TotalSumActivity, "No orders found for the selected range.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    var totalAmount = 0.0
                    for (orderSnapshot in snapshot.children) {
                        val timeOfOrder = orderSnapshot.child("timeOfOrder").getValue(String::class.java)
                        val action = orderSnapshot.child("action").getValue(String::class.java)
                        val progressStatus = orderSnapshot.child("progress_status").getValue(String::class.java)
                        val status = orderSnapshot.child("status").getValue(String::class.java)
                        val total = orderSnapshot.child("total").getValue(String::class.java)?.toDoubleOrNull()

                        if (progressStatus == null || status == null) {
                            Log.d("OrderCheck", "Skipping order due to missing fields: progress_status=$progressStatus, status=$status")
                            continue
                        }


                        if (timeOfOrder != null && action == "accepted" &&
                            progressStatus == "Delivered" && status == "Received"
                        ) {
                            val orderDatePart = timeOfOrder.split(" ").find { it.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}")) }
                            if (orderDatePart != null) {
                                try {
                                    val orderDateParsed = sdf.parse(orderDatePart)
                                    if (orderDateParsed != null && orderDateParsed in sdf.parse(startOfMonth)..selectedDateParsed) {
                                        val order = orderSnapshot.getValue(OrderModel::class.java)
                                        if (order != null) {
                                            Log.d("OrderCheck", "Adding order: ${order.orderId}")
                                            orderList.add(order)
                                            totalAmount += total ?: 0.0
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("TotalSumActivity", "Error parsing order date: $timeOfOrder", e)
                                }
                            }
                        }
                    }

                    binding.rvOrders.adapter?.notifyDataSetChanged()
                    binding.tvTotalOrders.text = "Total Orders: ${orderList.size}"
                    binding.tvTotalAmount.text = "Total Amount: %.2f PLN".format(totalAmount)

                    if (orderList.isEmpty()) {
                        Toast.makeText(this@TotalSumActivity, "No orders found for the selected range.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TotalSumActivity", "Database error: ${error.message}")
                    Toast.makeText(this@TotalSumActivity, "Failed to fetch orders: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }



    private fun fetchOrdersByYear() {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date first.", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val selectedDateParsed = try {
            sdf.parse(selectedDate)
        } catch (e: Exception) {
            Log.e("TotalSumActivity", "Invalid selected date: $selectedDate", e)
            null
        }

        if (selectedDateParsed == null) {
            Toast.makeText(this, "Invalid selected date.", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance()
        calendar.time = selectedDateParsed
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        val startOfYear = sdf.format(calendar.time)

        orderList.clear()
        binding.rvOrders.adapter?.notifyDataSetChanged()
        binding.tvTotalOrders.text = "Total Orders: 0"

        database.child("restaurants").child(restaurantKey!!).child("orders")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@TotalSumActivity, "No orders found for the selected range.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    var totalAmount = 0.0
                    for (orderSnapshot in snapshot.children) {
                        val timeOfOrder = orderSnapshot.child("timeOfOrder").getValue(String::class.java)
                        val action = orderSnapshot.child("action").getValue(String::class.java)
                        val progressStatus = orderSnapshot.child("progress_status").getValue(String::class.java)
                        val status = orderSnapshot.child("status").getValue(String::class.java)
                        val total = orderSnapshot.child("total").getValue(String::class.java)?.toDoubleOrNull()


                        if (progressStatus == null || status == null) {
                            Log.d("OrderCheck", "Skipping order due to missing fields: progress_status=$progressStatus, status=$status")
                            continue
                        }


                        if (timeOfOrder != null && action == "accepted" &&
                            progressStatus == "Delivered" &&  status == "Received"
                        ) {
                            val orderDatePart = timeOfOrder.split(" ").find { it.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}")) }
                            if (orderDatePart != null) {
                                try {
                                    val orderDateParsed = sdf.parse(orderDatePart)
                                    if (orderDateParsed != null && orderDateParsed in sdf.parse(startOfYear)!!..selectedDateParsed) {
                                        val order = orderSnapshot.getValue(OrderModel::class.java)
                                        if (order != null) {
                                            Log.d("OrderCheck", "Adding order: ${order.orderId}")
                                            orderList.add(order)
                                            totalAmount += total ?: 0.0
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("TotalSumActivity", "Error parsing order date: $timeOfOrder", e)
                                }
                            }
                        }
                    }

                    binding.rvOrders.adapter?.notifyDataSetChanged()
                    binding.tvTotalOrders.text = "Total Orders: ${orderList.size}"
                    binding.tvTotalAmount.text = "Total Amount: %.2f PLN".format(totalAmount)

                    if (orderList.isEmpty()) {
                        Toast.makeText(this@TotalSumActivity, "No orders found for the selected range.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TotalSumActivity", "Database error: ${error.message}")
                    Toast.makeText(this@TotalSumActivity, "Failed to fetch orders: ${error.message}", Toast.LENGTH_SHORT).show()
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
}
