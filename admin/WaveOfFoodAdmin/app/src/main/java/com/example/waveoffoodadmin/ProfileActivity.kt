package com.example.waveoffoodadmin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.waveoffoodadmin.databinding.FragmentProfileBinding
import com.example.waveoffoodadmin.model.UserModel
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var database: DatabaseReference
    private var restaurantKey: String? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)


        userId = intent.getStringExtra("USER_ID")
        restaurantKey = intent.getStringExtra("RESTAURANT_KEY")

        if (userId != null && restaurantKey != null) {
            Log.d("ProfileActivity", "Loading user data for userId: $userId and restaurantKey: $restaurantKey")
            database = FirebaseDatabase.getInstance("https://palatos-6612b-default-rtdb.firebaseio.com/").reference
            loadUserData(userId!!)
        } else {
            Log.e("ProfileActivity", "Failed to load profile: Missing user ID or restaurant key.")
            Toast.makeText(this, "Failed to load profile. Missing user or restaurant key.", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupListeners()
    }

    /**
     * Sets up button listeners for Edit, Save, and Back actions.
     */
    private fun setupListeners() {
        binding.editProfileButton.setOnClickListener {
            toggleFields(true)
        }

        binding.saveInfoButton.setOnClickListener {
            if (validateInputs()) {
                saveUserData()
                toggleFields(false)
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    /**
     * Loads user data from Firebase and populates the fields.
     */
    private fun loadUserData(userId: String) {
        Log.d("ProfileActivity", "Attempting to load data for user ID: $userId")

        database.child("restaurants").child("admins").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(UserModel::class.java)
                        if (user != null) {
                            if (user.restaurantKey == restaurantKey) {
                                populateFields(user)
                                Log.d("ProfileActivity", "User data loaded successfully.")
                            } else {
                                Log.e("ProfileActivity", "Mismatch: User's restaurantKey (${user.restaurantKey}) does not match selected key ($restaurantKey)")
                                Toast.makeText(
                                    this@ProfileActivity,
                                    "User data mismatch with selected restaurant!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Log.e("ProfileActivity", "Failed to parse user data!")
                            Toast.makeText(this@ProfileActivity, "Failed to parse user data!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("ProfileActivity", "No data found for user ID: $userId")
                        Toast.makeText(this@ProfileActivity, "No user data found!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ProfileActivity", "Failed to load user data: ${error.message}")
                    Toast.makeText(this@ProfileActivity, "Failed to load user data.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /**
     * Populates the UI fields with user data.
     */
    private fun populateFields(user: UserModel) {
        binding.name.setText(user.userName ?: "")
        binding.ownerName.setText(user.ownerName ?: "")
        binding.phoneNumber.setText(user.phoneNumber ?: "")
        binding.emailOrPhone.setText(user.email ?: "")
        binding.passsword.setText(user.password ?: "")
        binding.restaurantName.setText(user.restaurantName ?: "")
        toggleFields(false)
    }

    /**
     * Saves the edited user data to Firebase.
     */
    private fun saveUserData() {
        if (userId != null) {
            val user = UserModel(
                userName = binding.name.text.toString().trim(),
                ownerName = binding.ownerName.text.toString().trim(),
                phoneNumber = binding.phoneNumber.text.toString().trim(),
                email = binding.emailOrPhone.text.toString().trim(),
                password = binding.passsword.text.toString().trim(),
                restaurantName = binding.restaurantName.text.toString().trim(),
                restaurantKey = restaurantKey
            )

            database.child("restaurants").child("admins").child(userId!!).setValue(user)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("ProfileActivity", "Profile updated successfully for userId: $userId")
                        Toast.makeText(this@ProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("ProfileActivity", "Failed to update profile: ${task.exception}")
                        Toast.makeText(this@ProfileActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Log.e("ProfileActivity", "Failed to save profile: User ID is null.")
            Toast.makeText(this, "Failed to save profile. User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Toggles the editability of the input fields.
     */
    private fun toggleFields(enable: Boolean) {
        binding.name.isEnabled = enable
        binding.ownerName.isEnabled = enable
        binding.phoneNumber.isEnabled = enable
        binding.emailOrPhone.isEnabled = enable
        binding.passsword.isEnabled = enable
        binding.restaurantName.isEnabled = false
    }

    /**
     * Validates the user inputs before saving.
     */
    private fun validateInputs(): Boolean {
        val phone = binding.phoneNumber.text.toString().trim()


        return if (phone.length == 9 && phone.all { it.isDigit() }) {
            true
        } else {
            binding.phoneNumber.error = "Phone number must be exactly 9 digits"
            false
        }
    }
}
