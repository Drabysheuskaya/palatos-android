package com.example.PalaTos

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.PalaTos.databinding.ActivityChooseLocationBinding
import com.google.firebase.database.*

class ChooseLocationActivity : AppCompatActivity() {

    private val binding: ActivityChooseLocationBinding by lazy {
        ActivityChooseLocationBinding.inflate(layoutInflater)
    }

    private lateinit var sharedPreferences: SharedPreferences
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("location")
    private val locationMap = mutableMapOf<String, Pair<String, String>>()
    private var selectedLocationKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("PalaTosPrefs", MODE_PRIVATE)

        fetchLocations()

        binding.nextButton.setOnClickListener {
            if (selectedLocationKey != null) {
                saveSelectedLocation()
                navigateToRestaurantDetailActivity()
            } else {
                Toast.makeText(this, "Please select a location first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToRestaurantDetailActivity() {
        if (selectedLocationKey != null) {
            database.child(selectedLocationKey!!).get().addOnSuccessListener { snapshot ->
                val restaurantKey = snapshot.child("restaurantKey").getValue(String::class.java)
                if (restaurantKey != null) {
                    val intent = Intent(this, RestaurantDetailActivity::class.java).apply {
                        putExtra("restaurantKey", restaurantKey)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Restaurant key not found.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to fetch data.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Fetches location data from Firebase Realtime Database.
     */
    private fun fetchLocations() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                locationMap.clear()
                for (locationSnapshot in snapshot.children) {
                    val key = locationSnapshot.key
                    val name = locationSnapshot.child("name").getValue(String::class.java)
                    val address = locationSnapshot.child("address").getValue(String::class.java)
                    if (key != null && name != null && address != null) {
                        locationMap[key] = Pair(name, address)
                    }
                }
                populateLocationButtons()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ChooseLocationActivity,
                    "Error fetching locations: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    /**
     * Dynamically creates buttons for each location retrieved from Firebase.
     */
    private fun populateLocationButtons() {
        binding.locationsContainer.removeAllViews()

        for ((key, value) in locationMap) {
            val (name, address) = value
            val button = Button(this).apply {
                text = "$name\n$address"
                setBackgroundColor(Color.WHITE)
                setTextColor(Color.BLACK)
                setPadding(16, 16, 16, 16)
                textSize = 16f
                setOnClickListener {
                    handleLocationSelection(this, key)
                }
            }
            binding.locationsContainer.addView(button)
        }
    }

    /**
     * Highlights the selected location button and stores the selected location key.
     */
    private fun handleLocationSelection(selectedButton: Button, locationKey: String) {

        for (i in 0 until binding.locationsContainer.childCount) {
            val child = binding.locationsContainer.getChildAt(i) as Button
            child.setBackgroundColor(Color.WHITE)
            child.setTextColor(Color.BLACK)
        }

        selectedButton.setBackgroundColor(Color.GRAY)
        selectedButton.setTextColor(Color.WHITE)

        selectedLocationKey = locationKey
    }


    private fun saveSelectedLocation() {
        val editor = sharedPreferences.edit()
        editor.putString("selectedLocation", selectedLocationKey)
        editor.apply()
    }

}
