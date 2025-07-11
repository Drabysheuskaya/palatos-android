package com.example.waveoffoodadmin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.waveoffoodadmin.adapter.MenuAdapter
import com.example.waveoffoodadmin.databinding.ActivityMenuSectionBinding
import com.example.waveoffoodadmin.model.Items
import com.google.firebase.database.*

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuSectionBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var itemsList: MutableList<Items>
    private var restaurantKey: String? = null
    private var menuSection: String? = null
    private var unsavedChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuSectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restaurantKey = intent.getStringExtra("restaurantKey")
        menuSection = intent.getStringExtra("menuSection")

        if (restaurantKey.isNullOrEmpty() || menuSection.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid data. Please restart the app.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.sectionTitle.text = menuSection

        // Setup click listeners
        binding.backButton.setOnClickListener { handleBackPress() }
        binding.addButton.setOnClickListener { addNewItem() }
        binding.saveButton.setOnClickListener { saveItemsToFirebase() } // Save button functionality

        fetchMenuItems()
    }

    private fun fetchMenuItems() {
        database = FirebaseDatabase.getInstance()
        val sectionRef = database.reference
            .child("restaurants")
            .child(restaurantKey!!)
            .child("menu")
            .child(menuSection!!)
            .child("items")

        itemsList = mutableListOf()

        sectionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    snapshot.children.forEachIndexed { index, itemSnapshot ->
                        val item = itemSnapshot.getValue(Items::class.java)
                        if (item != null) {
                            item.firebaseKey = itemSnapshot.key ?: "item${index + 1}" // Track item key
                            itemsList.add(item)
                        }
                    }
                    setItemsAdapter()
                } else {
                    Toast.makeText(this@HomeActivity, "No items found for this section.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setItemsAdapter() {
        binding.sectionRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.sectionRecyclerView.adapter = MenuAdapter(
            itemsList,
            this,
            restaurantKey!!,
            onDeleteItem = { item -> promptDeleteConfirmation(item) },
            onEditItem = { item -> navigateToDetails(item) }
        )
    }

    private fun addNewItem() {
        val newItem = Items(
            foodName = "Example Food",
            foodPrice = "0 PLN",
            foodDescription = "Default description",
            foodIngredients = "Default ingredients",
            foodImage = "https://example.com/default-image.jpg",
            estimatedTime = 0
        )
        val newKey = "item${itemsList.size + 1}"
        newItem.firebaseKey = newKey

        itemsList.add(newItem)
        unsavedChanges = true
        setItemsAdapter()
    }

    private fun saveItemsToFirebase() {
        if (restaurantKey.isNullOrEmpty() || menuSection.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid data.", Toast.LENGTH_SHORT).show()
            return
        }

        val sectionRef = database.reference
            .child("restaurants")
            .child(restaurantKey!!)
            .child("menu")
            .child(menuSection!!)
            .child("items")

        val updates = mutableMapOf<String, Items>()
        itemsList.forEach { item ->
            updates[item.firebaseKey ?: "item${itemsList.indexOf(item) + 1}"] = item
        }

        sectionRef.setValue(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show()
                unsavedChanges = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save changes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToDetails(item: Items) {
        val intent = Intent(this, DetailsActivity::class.java).apply {
            putExtra("MenuItemName", item.foodName ?: "")
            putExtra("MenuItemImage", item.foodImage ?: "")
            putExtra("MenuItemPrice", item.foodPrice ?: "")
            putExtra("MenuItemDescription", item.foodDescription ?: "")
            putExtra("MenuItemIngredients", item.foodIngredients ?: "")
            putExtra("MenuItemEstimatedTime", item.estimatedTime ?: 0)
            putExtra("firebaseKey", item.firebaseKey ?: "")
            putExtra("restaurantKey", restaurantKey ?: "")
            putExtra("menuSection", menuSection ?: "")
        }
        startActivity(intent)
    }


    private fun handleBackPress() {
        if (unsavedChanges) {
            Toast.makeText(this, "Please save changes before leaving.", Toast.LENGTH_SHORT).show()
        } else {
            finish()
        }
    }

    private fun promptDeleteConfirmation(item: Items) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete ${item.foodName}?")
            .setPositiveButton("Yes") { _, _ ->
                itemsList.remove(item)
                unsavedChanges = true
                setItemsAdapter()
                Toast.makeText(this, "Item deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
