package com.example.waveoffoodadmin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.waveoffoodadmin.databinding.RestaurantDetailBinding
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

@Suppress("DEPRECATION")
class RestaurantDetailActivity : AppCompatActivity() {

    private lateinit var binding: RestaurantDetailBinding
    private var restaurantKey: String? = null
    private var selectedImageUri: Uri? = null
    private lateinit var restaurantRef: DatabaseReference
    private var isEditEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RestaurantDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restaurantKey = intent.getStringExtra("RESTAURANT_KEY")

        if (restaurantKey.isNullOrEmpty()) {
            showErrorAndExit("Error: Missing restaurant key!")
            return
        }

        restaurantRef = FirebaseDatabase.getInstance()
            .getReference("restaurants")
            .child(restaurantKey!!)

        fetchRestaurantDetails()
        setupListeners()
        toggleEditMode(false)
    }

    private fun toggleEditMode(enabled: Boolean) {
        binding.restaurantDescription.isEnabled = enabled
        binding.editImageButton.isEnabled = enabled
        binding.deleteImageButton.isEnabled = enabled
    }

    private fun saveChanges() {
        val description = binding.restaurantDescription.text.toString().trim()


        restaurantRef.child("description").setValue(description)
            .addOnSuccessListener {
                Toast.makeText(this, "Description updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update description: ${e.message}", Toast.LENGTH_SHORT).show()
            }


        selectedImageUri?.let { uri ->
            val storageRef = FirebaseStorage.getInstance().reference.child("restaurant_images/${UUID.randomUUID()}")
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { url ->
                        restaurantRef.child("image").setValue(url.toString())
                            .addOnSuccessListener {
                                Toast.makeText(this, "Image updated successfully!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to update image: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchRestaurantDetails() {
        restaurantRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    val description = snapshot.child("description").getValue(String::class.java)
                    val imageUrl = snapshot.child("image").getValue(String::class.java)

                    if (name != null) binding.restaurantName.text = name
                    if (description != null) binding.restaurantDescription.setText(description)
                    if (imageUrl != null) {
                        Glide.with(this@RestaurantDetailActivity)
                            .load(imageUrl)
                            .into(binding.restaurantImage)
                        binding.imageUrlInput.setText(imageUrl)}
                    populateMenuSections(snapshot.child("menu"))
                } else {
                    showErrorAndExit("Restaurant details not found.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showErrorAndExit("Failed to fetch restaurant details: ${error.message}")
            }
        })
    }

    private fun populateMenuSections(menuSnapshot: DataSnapshot) {
        binding.menuItemsContainer.removeAllViews()

        for (sectionSnapshot in menuSnapshot.children) {
            val sectionName = sectionSnapshot.key
            if (sectionName != null) {
                val sectionLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 16, 0, 16)
                    }
                }


                val sectionTextView = createSectionButton(sectionName)
                sectionLayout.addView(sectionTextView)


                val editButton = ImageButton(this).apply {
                    setImageResource(R.drawable.ic_edit)
                    layoutParams = LinearLayout.LayoutParams(
                        48.dpToPx(this@RestaurantDetailActivity),
                        48.dpToPx(this@RestaurantDetailActivity)
                    ).apply {
                        setMargins(8, 0, 8, 0)
                    }
                    background = null
                    setOnClickListener { showEditSectionDialog(sectionName) }
                }
                sectionLayout.addView(editButton)


                val deleteButton = ImageButton(this).apply {
                    setImageResource(R.drawable.ic_delete)
                    layoutParams = LinearLayout.LayoutParams(
                        48.dpToPx(this@RestaurantDetailActivity),
                        48.dpToPx(this@RestaurantDetailActivity)
                    ).apply {
                        setMargins(8, 0, 8, 0)
                    }
                    background = null
                    setOnClickListener { confirmDeleteSection(sectionName) }
                }
                sectionLayout.addView(deleteButton)


                binding.menuItemsContainer.addView(sectionLayout)
            }
        }
    }

    private fun createSectionButton(sectionName: String): TextView {
        return TextView(this).apply {
            text = sectionName
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                setMargins(16, 8, 16, 8)
            }
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black, null))
            setPadding(8, 8, 8, 8)
            isClickable = true
            isFocusable = true

            setOnClickListener {
                navigateToHomeActivity(sectionName)
            }
        }
    }

    private fun navigateToHomeActivity(sectionName: String) {
        val intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("restaurantKey", restaurantKey)
            putExtra("menuSection", sectionName)
        }
        startActivity(intent)
    }

    private fun showEditSectionDialog(sectionName: String) {
        val input = EditText(this).apply {
            setText(sectionName)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Section")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val sectionRef = restaurantRef.child("menu").child(sectionName)
                    sectionRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            val sectionData = snapshot.value
                            sectionRef.removeValue()
                            restaurantRef.child("menu").child(newName).setValue(sectionData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Section updated successfully.", Toast.LENGTH_SHORT).show()
                                    fetchRestaurantDetails()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to update section: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Section not found.", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error fetching section data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteSection(sectionName: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Section")
            .setMessage("Are you sure you want to delete the section \"$sectionName\"?")
            .setPositiveButton("Delete") { _, _ ->
                restaurantRef.child("menu").child(sectionName).removeValue()
                    .addOnSuccessListener { Toast.makeText(this, "Section deleted.", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(this, "Failed to delete section.", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupListeners() {

        binding.editDescriptionButton.setOnClickListener {
            isEditEnabled = true
            toggleEditMode(true)
            binding.editDescriptionButton.visibility = View.GONE
            binding.saveDescriptionButton.visibility = View.VISIBLE
        }

        binding.saveDescriptionButton.setOnClickListener {
            if (isEditEnabled) {
                saveChanges()
                binding.editDescriptionButton.visibility = View.VISIBLE
                binding.saveDescriptionButton.visibility = View.GONE
            } else {
                Toast.makeText(this, "Click 'Edit' to enable editing.", Toast.LENGTH_SHORT).show()
            }
        }


        binding.editImageButton.setOnClickListener {
            if (binding.imageUrlInput.visibility == View.GONE) {

                binding.imageUrlInput.visibility = View.VISIBLE
                binding.editImageButton.setImageResource(R.drawable.ic_save)
            } else {

                val newImageUrl = binding.imageUrlInput.text.toString().trim()
                if (newImageUrl.isNotEmpty()) {
                    updateImageInFirebase(newImageUrl)
                    binding.imageUrlInput.visibility = View.GONE
                    binding.editImageButton.setImageResource(R.drawable.ic_edit)
                } else {
                    Toast.makeText(this, "Image URL cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
        }


        binding.deleteImageButton.setOnClickListener {
            restaurantRef.child("image").removeValue()
                .addOnSuccessListener {
                    binding.restaurantImage.setImageResource(R.drawable.placeholder_image)
                    binding.imageUrlInput.text.clear()
                    Toast.makeText(this, "Image deleted successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete image.", Toast.LENGTH_SHORT).show()
                }
        }


        binding.addSectionButton.setOnClickListener {
            showAddSectionDialog()
        }


        binding.backButton.setOnClickListener {
            finish()
        }
    }


    private fun showEditImageUrlDialog() {
        binding.imageUrlInput.visibility = View.VISIBLE

        binding.imageUrlInput.setOnEditorActionListener { _, _, _ ->
            val newImageUrl = binding.imageUrlInput.text.toString().trim()
            if (newImageUrl.isNotEmpty()) {
                updateImageInFirebase(newImageUrl)
                binding.imageUrlInput.visibility = View.GONE
            } else {
                Toast.makeText(this, "Image URL cannot be empty.", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun updateImageInFirebase(imageUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                restaurantRef.child("image").setValue(imageUrl).await()
                withContext(Dispatchers.Main) {
                    Glide.with(this@RestaurantDetailActivity)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(binding.restaurantImage)
                    Toast.makeText(this@RestaurantDetailActivity, "Image updated successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RestaurantDetailActivity, "Failed to update image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddSectionDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Enter section name"
        }

        AlertDialog.Builder(this)
            .setTitle("Add Section")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val newSectionName = input.text.toString().trim()
                if (newSectionName.isNotEmpty()) {
                    val defaultSectionData = mapOf(
                        "items" to mapOf(
                            "item1" to mapOf(
                                "foodName" to "Example Food",
                                "foodPrice" to "0 PLN",
                                "foodDescription" to "Default description",
                                "foodIngredients" to "Default ingredients",
                                "foodImage" to "https://example.com/default-image.jpg",
                                "estimatedTime" to 0
                            )
                        )
                    )

                    restaurantRef.child("menu").child(newSectionName).setValue(defaultSectionData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Section added successfully.", Toast.LENGTH_SHORT).show()
                            createMenuSectionView(newSectionName)?.let {
                                binding.menuItemsContainer.addView(it, binding.menuItemsContainer.childCount - 1)
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Section name cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createMenuSectionView(sectionName: String): View {
        val sectionView = LayoutInflater.from(this).inflate(R.layout.menu_section_item, null)

        val sectionTextView = sectionView.findViewById<TextView>(R.id.sectionName)
        val itemContainer = sectionView.findViewById<LinearLayout>(R.id.itemContainer)

        sectionTextView.text = sectionName


        restaurantRef.child("menu").child(sectionName).child("items").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                itemContainer.removeAllViews()
                for (itemSnapshot in snapshot.children) {
                    val itemName = itemSnapshot.child("foodName").getValue(String::class.java) ?: "Unnamed Item"
                    val itemPrice = itemSnapshot.child("foodPrice").getValue(String::class.java) ?: "No Price"

                    val itemTextView = TextView(this@RestaurantDetailActivity).apply {
                        text = "$itemName - $itemPrice"
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(16, 8, 16, 8) }
                        textSize = 14f
                    }

                    itemContainer.addView(itemTextView)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RestaurantDetailActivity, "Error loading items: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        return sectionView
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            binding.restaurantImage.setImageURI(selectedImageUri)
        }
    }

    private fun showErrorAndExit(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }
}

fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}
