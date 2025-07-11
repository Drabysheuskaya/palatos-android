package com.example.waveoffoodadmin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.waveoffoodadmin.databinding.ActivityDetailsBinding
import com.example.waveoffoodadmin.model.Items
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailsBinding
    private var menuItem: Items? = null
    private var restaurantKey: String? = null
    private var menuSection: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        menuItem = Items(
            foodName = intent.getStringExtra("MenuItemName"),
            foodPrice = intent.getStringExtra("MenuItemPrice"),
            foodDescription = intent.getStringExtra("MenuItemDescription"),
            foodIngredients = intent.getStringExtra("MenuItemIngredients"),
            foodImage = intent.getStringExtra("MenuItemImage"),
            estimatedTime = intent.getIntExtra("MenuItemEstimatedTime", 0),
            firebaseKey = intent.getStringExtra("firebaseKey")
        )
        restaurantKey = intent.getStringExtra("restaurantKey")
        menuSection = intent.getStringExtra("menuSection")


        if (restaurantKey.isNullOrEmpty() || menuSection.isNullOrEmpty() || menuItem == null || menuItem?.firebaseKey.isNullOrEmpty()) {
            showErrorAndExit("Invalid data. Please restart the app.")
            return
        }

        bindDataToUI()


        binding.backButton.setOnClickListener { finish() }
        binding.editImageButton.setOnClickListener { toggleImageUrlInput() }
        binding.saveButton.setOnClickListener { saveChangesToFirebase() }
    }

    private fun bindDataToUI() {
        menuItem?.let {
            binding.detailFoodName.setText(it.foodName.orEmpty())
            binding.detailFoodPrice.setText(it.foodPrice.orEmpty())
            binding.detailDescription.setText(it.foodDescription.orEmpty())
            binding.detailIngredients.setText(it.foodIngredients.orEmpty())
            binding.detailEstimatedTime.setText(it.estimatedTime?.toString() ?: "0")
            binding.imageUrlInput.setText(it.foodImage.orEmpty())

            Glide.with(this)
                .load(it.foodImage.orEmpty())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(binding.detailFoodImage)
        }
    }

    private fun toggleImageUrlInput() {
        if (binding.imageUrlInput.visibility == View.GONE) {
            binding.imageUrlInput.visibility = View.VISIBLE
        } else {
            val newImageUrl = binding.imageUrlInput.text.toString().trim()
            if (newImageUrl.isNotEmpty()) {
                menuItem?.foodImage = newImageUrl
                Glide.with(this)
                    .load(newImageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(binding.detailFoodImage)
            } else {
                Toast.makeText(this, "Please enter a valid image URL.", Toast.LENGTH_SHORT).show()
            }
            binding.imageUrlInput.visibility = View.GONE
        }
    }

    private fun saveChangesToFirebase() {
        val foodName = binding.detailFoodName.text.toString().trim()
        val foodPrice = binding.detailFoodPrice.text.toString().trim()
        val foodImage = menuItem?.foodImage.orEmpty()
        val estimatedTimeStr = binding.detailEstimatedTime.text.toString().trim()


        if (foodName.isEmpty() || foodPrice.isEmpty() || foodImage.isEmpty() || menuItem?.firebaseKey.isNullOrEmpty()) {
            Toast.makeText(this, "All fields must be filled.", Toast.LENGTH_SHORT).show()
            return
        }

        val estimatedTime = estimatedTimeStr.toIntOrNull() ?: 0
        val updatedMenuItem = menuItem!!.copy(
            foodName = foodName,
            foodPrice = foodPrice,
            foodDescription = binding.detailDescription.text.toString().trim().ifEmpty { "No description provided" },
            foodIngredients = binding.detailIngredients.text.toString().trim().ifEmpty { "Ingredients not specified" },
            foodImage = foodImage,
            estimatedTime = estimatedTime
        )

        updateFirebaseData(updatedMenuItem)
    }

    private fun updateFirebaseData(menuItem: Items) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val databaseRef = FirebaseDatabase.getInstance().reference
                    .child("restaurants")
                    .child(restaurantKey!!)
                    .child("menu")
                    .child(menuSection!!)
                    .child("items")
                    .child(menuItem.firebaseKey!!)

                Log.d("DetailsActivity", "Updating Firebase at path: ${databaseRef.path}")

                databaseRef.setValue(menuItem).await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetailsActivity, "Item updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("DetailsActivity", "Firebase update failed: ${e.message}")
                    Toast.makeText(this@DetailsActivity, "Failed to update item: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showErrorAndExit(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}
