package com.example.PalaTos

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.example.PalaTos.Fragment.CartFragment
import com.example.PalaTos.Model.CartItems
import com.example.PalaTos.Model.SharedViewModel
import com.example.PalaTos.databinding.ActivityDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailsBinding
    private var foodName: String? = null
    private var foodImage: String? = null
    private var foodDescription: String? = null
    private var foodIngredients: String? = null
    private var foodPrice: String? = null
    private var restaurantKey: String? = null
    private var estimatedTime: Int = 0
    private lateinit var auth: FirebaseAuth

    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()


        foodName = intent.getStringExtra("MenuItemName")
        foodDescription = intent.getStringExtra("MenuItemDescription") ?: "No description available"
        foodIngredients = intent.getStringExtra("MenuItemIngredients") ?: "Ingredients not provided"
        foodPrice = intent.getStringExtra("MenuItemPrice") ?: "0 PLN"
        foodImage = intent.getStringExtra("MenuItemImage")
        restaurantKey = intent.getStringExtra("restaurantKey")
        estimatedTime = intent.getIntExtra("MenuItemEstimatedTime", 0)


        Log.d("DetailsActivity", "Retrieved estimatedTime: $estimatedTime minutes")


        with(binding) {
            detailFoodName.text = foodName ?: "N/A"
            detailFoodPrice.text = foodPrice
            detailDescription.text = foodDescription
            detailIngredients.text = foodIngredients

            if (!foodImage.isNullOrEmpty()) {
                Glide.with(this@DetailsActivity)
                    .load(Uri.parse(foodImage))
                    .into(detailFoodImage)
            } else {

            }
        }

        binding.imageButton.setOnClickListener {
            finish()
        }

        binding.addItemButton.setOnClickListener {
            addItemToCart()
        }
    }

    private fun addItemToCart() {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (restaurantKey.isNullOrEmpty()) {
            Toast.makeText(this, "Restaurant not selected.", Toast.LENGTH_SHORT).show()
            return
        }


        val cartItem = CartItems(
            foodName = foodName ?: "Unknown",
            foodPrice = foodPrice ?: "0 PLN",
            foodDescription = foodDescription ?: "No description available",
            foodImage = foodImage ?: "",
            foodIngredients = foodIngredients ?: "No ingredients provided",
            foodQuantity = 1,
            estimatedTime = estimatedTime
        )


        val databaseRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("CartItems")
            .child(restaurantKey!!)


        databaseRef.push().setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Item added to cart successfully!",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add item.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToCart() {
        if (restaurantKey.isNullOrEmpty()) {
            Toast.makeText(this, "Restaurant not selected.", Toast.LENGTH_SHORT).show()
            return
        }


        sharedViewModel.setRestaurantKey(restaurantKey!!)

        val intent = Intent(this, CartFragment::class.java)
        startActivity(intent)
        Log.d("DetailsActivity", "Navigated to Cart with restaurantKey: $restaurantKey")
    }
}
