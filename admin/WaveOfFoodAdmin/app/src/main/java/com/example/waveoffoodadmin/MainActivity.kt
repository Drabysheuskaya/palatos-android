package com.example.waveoffoodadmin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.waveoffoodadmin.databinding.ActivityMainBinding
import com.example.waveoffoodadmin.model.SharedRestaurantViewModel
import com.google.firebase.database.*
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var database: FirebaseDatabase
    private lateinit var orderReference: DatabaseReference
    private var restaurantKey: String? = null
    private var userId: String? = null


    private val sharedRestaurantViewModel: SharedRestaurantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        restaurantKey = intent.getStringExtra("RESTAURANT_KEY")
        userId = intent.getStringExtra("USER_ID")

        Log.d("MainActivity", "Received restaurantKey: $restaurantKey, userId: $userId")

        if (restaurantKey == null || userId == null) {
            Toast.makeText(this, "Error: Missing restaurant key or user ID!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        sharedRestaurantViewModel.setRestaurantKey(restaurantKey!!)

        database = FirebaseDatabase.getInstance("https://palatos-6612b-default-rtdb.firebaseio.com/")


        fetchOrderSummary(userId!!)


        binding.editSectionButton.setOnClickListener {
            val intent = Intent(this, RestaurantDetailActivity::class.java).apply {
                putExtra("RESTAURANT_KEY", restaurantKey)
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }
        binding.ordersInfoButton.setOnClickListener {
            val intent = Intent(this, OrderInfoActivity::class.java).apply {
                putExtra("RESTAURANT_KEY", restaurantKey)
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }
        binding.newOrdersButton.setOnClickListener {
            val intent = Intent(this, NewOrdersActivity::class.java).apply {
                putExtra("RESTAURANT_KEY", restaurantKey)
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }

        binding.totalAmountButton.setOnClickListener {
            val intent = Intent(this, TotalSumActivity::class.java).apply {
                putExtra("RESTAURANT_KEY", restaurantKey)
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }

        binding.tipsInfoButton.setOnClickListener {
            val intent = Intent(this, TipsInfoActivity::class.java).apply {
                putExtra("RESTAURANT_KEY", restaurantKey)
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }


        binding.feedbackButton.setOnClickListener {
            val intent = Intent(this, FeedbackActivity::class.java).apply {
                putExtra("RESTAURANT_KEY", restaurantKey)
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }

        binding.profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra("RESTAURANT_KEY", restaurantKey)
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }

        binding.userInfoButton.setOnClickListener {
            val intent = Intent(this, UserManagementActivity::class.java).apply {
                putExtra("RESTAURANT_KEY", restaurantKey)
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }

        binding.logoutButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("LOGIN_REF", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("LOGIN_REF", false)
            editor.apply()
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun fetchOrderSummary(userId: String) {
        Log.d("MainActivity", "Starting fetchOrderSummary for userId: $userId and restaurantKey: $restaurantKey")

        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Log.d("MainActivity", "Current date is: $currentDate")

        orderReference = database.reference.child("restaurants").child(restaurantKey!!).child("orders")
        Log.d("MainActivity", "Order reference set to: $orderReference")


        orderReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("MainActivity", "onDataChange triggered with snapshot: ${snapshot.key}")

                var completedOrdersToday = 0
                var notAcceptedOrdersToday = 0
                var inProgressOrdersToday = 0

                for (orderSnapshot in snapshot.children) {
                    Log.d("MainActivity", "Processing orderSnapshot: ${orderSnapshot.key}")

                    val timeOfOrder = orderSnapshot.child("timeOfOrder").value as? String
                    Log.d("MainActivity", "timeOfOrder: $timeOfOrder")

                    // Extract and reformat the date part from timeOfOrder
                    val orderDate = timeOfOrder?.split(" ")?.get(1)?.let { datePart ->
                        java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(datePart)?.let { parsedDate ->
                            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate)
                        }
                    }
                    Log.d("MainActivity", "Extracted order date: $orderDate")

                    val progressStatus = orderSnapshot.child("progress_status").value as? String
                    Log.d("MainActivity", "Progress status: $progressStatus")

                    val action = orderSnapshot.child("action").value as? String
                    Log.d("MainActivity", "Action: $action")

                    if (orderDate == currentDate) {
                        Log.d("MainActivity", "Order matches today's date")

                        when {
                            progressStatus == "Delivered" && action == "accepted" -> {
                                completedOrdersToday++
                                Log.d("MainActivity", "Incremented completedOrdersToday to $completedOrdersToday")
                            }
                            progressStatus == null && action != "accepted" -> {
                                notAcceptedOrdersToday++
                                Log.d("MainActivity", "Incremented notAcceptedOrdersToday to $notAcceptedOrdersToday")
                            }
                            progressStatus == "In Progress" -> {
                                inProgressOrdersToday++
                                Log.d("MainActivity", "Incremented inProgressOrdersToday to $inProgressOrdersToday")
                            }
                        }
                    }
                }

                binding.completedOrdersTextView.text = completedOrdersToday.toString()
                Log.d("MainActivity", "Updated completedOrdersTextView with value: $completedOrdersToday")

                binding.notAcceptedOrdersTextView.text = notAcceptedOrdersToday.toString()
                Log.d("MainActivity", "Updated notAcceptedOrdersTextView with value: $notAcceptedOrdersToday")

                binding.inProgressOrdersTextView.text = inProgressOrdersToday.toString()
                Log.d("MainActivity", "Updated inProgressOrdersTextView with value: $inProgressOrdersToday")

                Log.d(
                    "MainActivity",
                    "Summary - Completed: $completedOrdersToday, Not Accepted: $notAcceptedOrdersToday, In Progress: $inProgressOrdersToday"
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Failed to fetch order summary: ${error.message}")
                Toast.makeText(this@MainActivity, "Failed to fetch order summary.", Toast.LENGTH_SHORT).show()
            }
        })
    }



}
