package com.example.waveoffoodadmin

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.waveoffoodadmin.adapter.TipsAdapter
import com.example.waveoffoodadmin.databinding.ActivityTipsInfoBinding
import com.example.waveoffoodadmin.model.Tip
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class TipsInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTipsInfoBinding
    private lateinit var database: DatabaseReference
    private val tipsList = mutableListOf<Tip>()
    private var restaurantKey: String? = null
    private val userCache = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTipsInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.backButton.setOnClickListener {
            finish()
        }


        restaurantKey = intent.getStringExtra("RESTAURANT_KEY")

        if (restaurantKey == null) {
            Toast.makeText(this, "Error: Missing restaurant key!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance("https://palatos-6612b-default-rtdb.firebaseio.com/").reference


        displayCurrentDateTime()


        fetchMonthlyTipsSummary()


        binding.selectDateButton.setOnClickListener {
            showDatePicker()
        }


        binding.nextButton.setOnClickListener {
            val selectedDate = binding.selectedDateTextView.text.toString()
            if (selectedDate.isEmpty()) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            } else {
                fetchTipsForDate(selectedDate)
                fetchDailyTipsSummary(selectedDate)
            }
        }
    }

    private fun displayCurrentDateTime() {
        val currentDateTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        binding.currentDateTime.text = "(Current Date/Time: $currentDateTime)"
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%02d.%02d.%d", dayOfMonth, month + 1, year)
                binding.selectedDateTextView.text = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun fetchTipsForDate(selectedDate: String) {
        database.child("restaurants").child(restaurantKey!!).child("orders")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    tipsList.clear()
                    val ordersOnDate = snapshot.children.filter { orderSnapshot ->
                        val timeOfOrder = orderSnapshot.child("timeOfOrder").value?.toString()
                        timeOfOrder != null && timeOfOrder.contains(selectedDate) && orderSnapshot.child("tip").exists()
                    }

                    if (ordersOnDate.isEmpty()) {
                        Toast.makeText(this@TipsInfoActivity, "No tips found for the selected date", Toast.LENGTH_SHORT).show()
                        return
                    }

                    for (orderSnapshot in ordersOnDate) {
                        val userId = orderSnapshot.child("userId").value.toString()
                        val tip = orderSnapshot.child("tip").value.toString().toInt()
                        val orderId = orderSnapshot.child("orderId").value.toString()
                        val tableNumber = orderSnapshot.child("tableNumber").value.toString()
                        val total = orderSnapshot.child("total").value.toString()
                        val waiter = orderSnapshot.child("waiter").value.toString()
                        val timeOfOrder = orderSnapshot.child("timeOfOrder").value.toString()

                        fetchUserName(userId) { userName ->
                            tipsList.add(
                                Tip(
                                    userName = userName,
                                    orderId = orderId,
                                    totalPrice = total,
                                    tableNumber = tableNumber,
                                    waiterName = waiter,
                                    timeOfOrder = timeOfOrder,
                                    amount = tip
                                )
                            )
                            setupRecyclerView()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@TipsInfoActivity, "Error fetching tips: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchMonthlyTipsSummary() {
        val currentMonth = SimpleDateFormat("MM.yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

        database.child("restaurants").child(restaurantKey!!).child("orders")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalTipsAmount = 0
                    var totalTipsCount = 0

                    for (orderSnapshot in snapshot.children) {
                        val timeOfOrder = orderSnapshot.child("timeOfOrder").value?.toString()
                        if (timeOfOrder != null && timeOfOrder.contains(currentMonth) && orderSnapshot.child("tip").exists()) {
                            val tip = orderSnapshot.child("tip").value.toString().toInt()
                            totalTipsAmount += tip
                            totalTipsCount++
                        }
                    }

                    binding.monthlyTipsCount.text = "Total Tips This Month: $totalTipsCount"
                    binding.monthlyTipsAmount.text = "Total Amount This Month: $totalTipsAmount PLN"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@TipsInfoActivity, "Error fetching monthly tips: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchDailyTipsSummary(selectedDate: String) {
        database.child("restaurants").child(restaurantKey!!).child("orders")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var dailyTipsAmount = 0
                    var dailyTipsCount = 0

                    for (orderSnapshot in snapshot.children) {
                        val timeOfOrder = orderSnapshot.child("timeOfOrder").value?.toString()
                        if (timeOfOrder != null && timeOfOrder.contains(selectedDate) && orderSnapshot.child("tip").exists()) {
                            val tip = orderSnapshot.child("tip").value.toString().toInt()
                            dailyTipsAmount += tip
                            dailyTipsCount++
                        }
                    }

                    binding.dailyTipsCount.text = "Tips on $selectedDate: $dailyTipsCount"
                    binding.dailyTipsAmount.text = "Amount on $selectedDate: $dailyTipsAmount PLN"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@TipsInfoActivity, "Error fetching daily tips: ${error.message}", Toast.LENGTH_SHORT).show()
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
                        userCache[userId] = userName // Cache the user name
                        callback(userName)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback("Unknown User")
                    }
                })
        }
    }

    private fun setupRecyclerView() {
        val adapter = TipsAdapter(this, tipsList)
        binding.tipsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tipsRecyclerView.adapter = adapter
    }
}
