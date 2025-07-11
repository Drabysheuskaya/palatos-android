package com.example.PalaTos

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.PalaTos.Model.CartItems
import com.example.PalaTos.databinding.ActivityOrderInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OrderInfoActivity : AppCompatActivity() {
    private var binding: ActivityOrderInfoBinding? = null
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private var userId = ""
    private val cartItems: MutableList<CartItems> = mutableListOf()
    private var subtotal = 0.0
    private var serviceCharge = 0.0
    private var total = 0.0
    private val tableList: MutableList<String?> = ArrayList()
    private var restaurantKey: String? = ""
    private var isTableEditModeEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderInfoBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        userId = if (auth.currentUser != null) auth.currentUser!!.uid else ""
        restaurantKey =
            if (intent.getStringExtra("restaurantKey") != null) intent.getStringExtra("restaurantKey") else ""

        val passedTotal = intent.getStringExtra("TOTAL")
        if (passedTotal != null && !passedTotal.isEmpty()) {
            total = passedTotal.replace(" PLN", "").toDouble()
            binding!!.totalPrice.text =
                String.format(Locale.getDefault(), "%.2f PLN", total)
        }

        if (!userId.isEmpty() && !restaurantKey!!.isEmpty()) {
            loadUserProfile()
            loadAvailableTables()
            loadCartItems()
            listenForNameUpdates()
        } else {
            Toast.makeText(
                this,
                "User not authenticated or invalid restaurant key!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }

        binding!!.placeOrderButton.setOnClickListener { v -> placeOrder() }
        binding!!.backButton.setOnClickListener { v -> finish() }

        binding!!.editNameButton.setOnClickListener { v ->
            enableEditMode(
                binding!!.nameInput,
                binding!!.editNameButton,
                binding!!.saveNameButton
            )
        }
        binding!!.saveNameButton.setOnClickListener { v ->
            val updatedName = binding!!.nameInput.text.toString().trim()
            if (!updatedName.isEmpty()) {
                updateUserField("name", updatedName)
                disableEditMode(
                    binding!!.nameInput,
                    binding!!.editNameButton,
                    binding!!.saveNameButton
                )
            } else {
                Toast.makeText(this, "Name cannot be empty!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding!!.editTableButton.setOnClickListener { v -> enableTableEditMode() }
        binding!!.saveTableButton.setOnClickListener { v -> saveUpdatedTableNumber() }

        binding!!.tableNumberSpinner.visibility = View.GONE
        binding!!.saveTableButton.visibility = View.GONE
    }



    private fun loadCartItems() {
        val cartRef = database.child("users").child(userId).child("CartItems").child(restaurantKey!!)

        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cartItems.clear()
                subtotal = 0.0

                if (snapshot.exists()) {
                    snapshot.children.forEach { itemSnapshot ->
                        val cartItem = itemSnapshot.getValue(CartItems::class.java)
                        cartItem?.let {
                            cartItems.add(it)
                            subtotal += (it.foodPrice.replace(" PLN", "").toDoubleOrNull() ?: 0.0) * it.foodQuantity
                        }
                    }

                    serviceCharge = subtotal * 0.10
                    total = subtotal + serviceCharge

                    // Update total price in the UI
                    binding?.totalPrice?.text = "%.2f PLN".format(total)

                    Log.d("OrderInfoActivity", "Loaded cart items: ${cartItems.size}")
                } else {
                    Toast.makeText(this@OrderInfoActivity, "No items in cart.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@OrderInfoActivity, "Failed to load cart items: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun loadUserProfile() {
        val userRef = database.child("users").child(userId).child("restaurants").child(
            restaurantKey!!
        )
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = if (snapshot.child("name").getValue(
                            String::class.java
                        ) != null
                    ) snapshot.child("name").getValue(
                        String::class.java
                    ) else "No Name"
                    val tableNumber = if (snapshot.child("tableNumber").getValue(
                            String::class.java
                        ) != null
                    ) snapshot.child("tableNumber").getValue(
                        String::class.java
                    ) else ""

                    binding!!.nameInput.setText(name)

                    val index = tableList.indexOf(tableNumber)
                    if (index != -1) {
                        binding!!.tableNumberSpinner.setSelection(index)
                    }
                } else {
                    Toast.makeText(
                        this@OrderInfoActivity,
                        "User data not found!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@OrderInfoActivity,
                    "Failed to load profile: " + error.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadAvailableTables() {
        val tablesRef = database.child("restaurants").child(restaurantKey!!).child("tables")
        tablesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tableList.clear()
                if (snapshot.exists()) {
                    for (tableSnapshot in snapshot.children) {
                        val tableNumber: String? = when (val value = tableSnapshot.child("tableNumber").value) {
                            is Long -> value.toString()
                            is String -> value
                            else -> null
                        }

                        tableNumber?.let {
                            tableList.add(it)
                        }
                    }

                    if (tableList.isNotEmpty()) {
                        val adapter = ArrayAdapter(
                            this@OrderInfoActivity,
                            R.layout.simple_spinner_item,
                            tableList
                        )
                        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                        binding!!.tableNumberSpinner.adapter = adapter
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@OrderInfoActivity,
                    "Failed to load tables: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }




    private fun enableTableEditMode() {
        if (!isTableEditModeEnabled) {
            binding!!.tableNumberSpinner.visibility = View.VISIBLE
            binding!!.editTableButton.visibility = View.GONE
            binding!!.saveTableButton.visibility = View.VISIBLE
            isTableEditModeEnabled = true
        }
    }

    private fun saveUpdatedTableNumber() {
        val selectedTable =
            if (binding!!.tableNumberSpinner.selectedItem != null) binding!!.tableNumberSpinner.selectedItem.toString() else ""
        if (!selectedTable.isEmpty()) {
            val tableRef = database.child("users").child(userId).child("restaurants").child(
                restaurantKey!!
            ).child("tableNumber")
            tableRef.setValue(selectedTable).addOnSuccessListener { aVoid: Void? ->
                Toast.makeText(
                    this,
                    "Table number updated successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                binding!!.tableNumberSpinner.visibility = View.GONE
                binding!!.editTableButton.visibility = View.VISIBLE
                binding!!.saveTableButton.visibility = View.GONE
                isTableEditModeEnabled = false
            }.addOnFailureListener { e: Exception? ->
                Toast.makeText(
                    this,
                    "Failed to update table number.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(this, "Please select a valid table number!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listenForNameUpdates() {
        val nameRef = database.child("users").child(userId).child("restaurants").child(
            restaurantKey!!
        ).child("name")
        nameRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedName =
                    if (snapshot.getValue(String::class.java) != null) snapshot.getValue(
                        String::class.java
                    ) else "No Name"
                binding!!.nameInput.setText(updatedName)
                Log.d("OrderInfoActivity", "Name updated in real-time: $updatedName")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("OrderInfoActivity", "Failed to listen for name updates: " + error.message)
            }
        })
    }

    private fun updateUserField(field: String, value: String) {
        val userRef = database.child("users").child(userId).child("restaurants").child(
            restaurantKey!!
        ).child(field)
        userRef.setValue(value).addOnSuccessListener { aVoid: Void? ->
            Toast.makeText(
                this,
                "$field updated successfully!", Toast.LENGTH_SHORT
            ).show()
        }
            .addOnFailureListener { e: Exception ->
                Toast.makeText(
                    this,
                    "Failed to update " + field + ": " + e.message,
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(
                    "OrderInfoActivity",
                    "Failed to update " + field + ": " + e.message
                )
            }
    }



    private fun placeOrder() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty. Cannot place order.", Toast.LENGTH_SHORT).show()
            return
        }

        val orderId = generateOrderId()
        val currentTime = currentTime
        val tableNumber = binding!!.tableNumberSpinner.selectedItem.toString().toInt()

        var totalEstimatedTime = 0
        cartItems.forEach {
            val itemTime = it.estimatedTime * it.foodQuantity
            totalEstimatedTime = maxOf(totalEstimatedTime, itemTime)
        }

        Log.d("OrderInfoActivity", "Placing order: $orderId with items: ${cartItems.size}")

        val items: List<Map<String, Any>> = cartItems.map {
            mapOf(
                "name" to it.foodName,
                "price" to it.foodPrice,
                "quantity" to it.foodQuantity,
                "estimatedTime" to it.estimatedTime
            )
        }

        val orderData: MutableMap<String, Any> = mutableMapOf(
            "orderId" to orderId,
            "items" to items,
            "subtotal" to String.format("%.2f", subtotal),
            "service" to String.format("%.2f", serviceCharge),
            "total" to String.format("%.2f", total),
            "tableNumber" to tableNumber,
            "timeOfOrder" to currentTime,
            "status" to "Pending",
            "waiter" to "Pending Assignment",
            "userId" to userId,
            "estimatedTime" to totalEstimatedTime
        )

        binding!!.placeOrderButton.isEnabled = false //

        val ordersRef = database.child("restaurants").child(restaurantKey!!).child("orders").child(orderId)
        ordersRef.setValue(orderData).addOnSuccessListener {
            Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show()
            clearCart()
            navigateToCongrats(orderId)
        }.addOnFailureListener { e: Exception ->
            Toast.makeText(this, "Failed to place order: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("OrderInfoActivity", "Failed to place order: ${e.message}")
            binding!!.placeOrderButton.isEnabled = true // Re-enable on failure
        }
    }


    private fun clearCart() {
        val cartRef = database.child("users").child(userId).child("CartItems")
        cartRef.removeValue().addOnSuccessListener { aVoid: Void? ->
            Toast.makeText(
                this,
                "Cart cleared successfully.",
                Toast.LENGTH_SHORT
            ).show()
        }
            .addOnFailureListener { e: Exception? ->
                Toast.makeText(
                    this,
                    "Failed to clear cart.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun navigateToCongrats(orderId: String) {
        val intent = Intent(this, CongratsActivity::class.java)
        intent.putExtra("TOTAL", String.format(Locale.getDefault(), "%.2f PLN", total))
        intent.putExtra("ORDER_ID", orderId)
        intent.putExtra("RESTAURANT_KEY", restaurantKey)
        startActivity(intent)
        finish()
    }


    private fun generateOrderId(): String {
        val calendar = Calendar.getInstance()
        val monthOfYear = calendar[Calendar.MONTH]
        val dayOfMonth = calendar[Calendar.DAY_OF_MONTH]
        val timeInMillis = System.currentTimeMillis() % 10000

        val prefix = ('A'.code + monthOfYear).toChar()
        return "$prefix$dayOfMonth$timeInMillis"
    }



    private val currentTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.getDefault())
            return sdf.format(Date())
        }

    private fun enableEditMode(inputField: View, editButton: View, saveButton: View) {
        inputField.isEnabled = true
        editButton.visibility = View.GONE
        saveButton.visibility = View.VISIBLE
    }

    private fun disableEditMode(inputField: View, editButton: View, saveButton: View) {
        inputField.isEnabled = false
        editButton.visibility = View.VISIBLE
        saveButton.visibility = View.GONE
    }
}
