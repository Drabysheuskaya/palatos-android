package com.example.PalaTos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CongratsActivity : AppCompatActivity() {

    private lateinit var orderId: String
    private lateinit var restaurantKey: String
    private lateinit var payButton: Button

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_congrats)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val orderPlacedMessage = findViewById<TextView>(R.id.orderPlacedMessage)
        payButton = findViewById(R.id.payButton)


        val total = intent.getStringExtra("TOTAL") ?: "0.0 PLN"
        orderId = intent.getStringExtra("ORDER_ID") ?: ""
        restaurantKey = intent.getStringExtra("RESTAURANT_KEY") ?: ""

        orderPlacedMessage.text = "Your order is placed!\nTotal: $total\nNow you can pay for the order in the pay section."


        backButton.setOnClickListener {
            finish()
        }


        payButton.setOnClickListener {
            if (orderId.isNotEmpty() && restaurantKey.isNotEmpty()) {
                payButton.isEnabled = false
                updateOrderStatusToPaid()
            }
        }
    }

    private fun updateOrderStatusToPaid() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val orderRef = FirebaseDatabase.getInstance().reference
            .child("restaurants")
            .child(restaurantKey)
            .child("orders")
            .child(orderId)

        orderRef.child("status").setValue("Paid").addOnSuccessListener {
            Toast.makeText(this, "Order marked as Paid!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ChooseLocationActivity::class.java)
            startActivity(intent)
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update order status.", Toast.LENGTH_SHORT).show()
            payButton.isEnabled = true
        }
    }
}
