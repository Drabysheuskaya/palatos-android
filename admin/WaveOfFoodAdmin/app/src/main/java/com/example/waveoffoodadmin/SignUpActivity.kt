package com.example.waveoffoodadmin

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.waveoffoodadmin.databinding.ActivitySignUpBinding
import com.example.waveoffoodadmin.model.UserModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SignUpActivity : AppCompatActivity() {
    private var userName: String? = null
    private var ownerName: String? = null
    private var phoneNumber: String? = null
    private var selectedLocationKey: String? = null
    private var selectedRestaurantKey: String? = null
    private var selectedRestaurantName: String? = null
    private var email: String? = null
    private var password: String? = null
    private var auth: FirebaseAuth? = null
    private var database: DatabaseReference? = null
    private var binding: ActivitySignUpBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        auth = FirebaseAuth.getInstance()
        database =
            FirebaseDatabase.getInstance("https://palatos-6612b-default-rtdb.firebaseio.com/").reference

        Log.d(TAG, "Database reference initialized: " + database.toString())

        populateRestaurantSpinner()

        binding!!.createUserButton.setOnClickListener { v ->
            userName = binding!!.name.text.toString().trim()
            ownerName = binding!!.ownerName.text.toString().trim()
            phoneNumber = binding!!.phoneNumber.text.toString().trim()
            email = binding!!.emailOrPhone.text.toString().trim()
            password = binding!!.passsword.text.toString().trim()

            Log.d(
                TAG, "Create User Button clicked. Inputs: " +
                        "userName=" + userName +
                        ", ownerName=" + ownerName +
                        ", phoneNumber=" + phoneNumber +
                        ", email=" + email +
                        ", selectedLocationKey=" + selectedLocationKey +
                        ", selectedRestaurantKey=" + selectedRestaurantKey
            )

            if (userName!!.isEmpty() || ownerName!!.isEmpty() || phoneNumber!!.isEmpty() ||
                email!!.isEmpty() || password!!.isEmpty() || selectedRestaurantKey == null || selectedLocationKey == null
            ) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (!isValidPhoneNumber(phoneNumber!!)) {
                Toast.makeText(
                    this,
                    "Invalid phone number. Enter exactly 9 digits.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            checkIfEmailApproved(email!!)
        }

        binding!!.signInButton.setOnClickListener { v ->
            val intent = Intent(
                this,
                LoginActivity::class.java
            )
            startActivity(intent)
        }
    }

    private fun populateRestaurantSpinner() {
        Log.d(TAG, "Populating restaurant spinner")

        database!!.child("location").get().addOnSuccessListener { snapshot: DataSnapshot ->
            if (snapshot.exists()) {
                Log.d(TAG, "Successfully fetched location data: $snapshot")

                val restaurantNames = mutableListOf<String>()
                val locationKeys = mutableListOf<String>()

                for (locationSnapshot in snapshot.children) {
                    val locationKey = locationSnapshot.key
                    val name = locationSnapshot.child("name").getValue(String::class.java)
                    val address = locationSnapshot.child("address").getValue(String::class.java)
                    val restaurantKey = locationSnapshot.child("restaurantKey").getValue(String::class.java)

                    if (!name.isNullOrEmpty() && !address.isNullOrEmpty() && !locationKey.isNullOrEmpty() && !restaurantKey.isNullOrEmpty()) {
                        restaurantNames.add("$name, $address")
                        locationKeys.add(locationKey) // Keep track of location keys
                    }
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, restaurantNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding!!.restaurantSpinner.adapter = adapter

                // Handle spinner item selection
                binding!!.restaurantSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedLocationKey = locationKeys[position] // Get selected location key
                        val selectedSnapshot = snapshot.child(selectedLocationKey!!) // Fetch corresponding snapshot
                        selectedRestaurantKey = selectedSnapshot.child("restaurantKey").getValue(String::class.java)
                        selectedRestaurantName = selectedSnapshot.child("name").getValue(String::class.java)

                        Log.d(TAG, "Selected Location: $selectedLocationKey, Restaurant Key: $selectedRestaurantKey")
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedLocationKey = null
                        selectedRestaurantKey = null
                        selectedRestaurantName = null
                    }
                }
            } else {
                Log.w(TAG, "No location data found.")
                Toast.makeText(this, "No restaurant data available", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e: Exception ->
            Log.e(TAG, "Failed to fetch restaurant data: ${e.message}")
            Toast.makeText(this, "Failed to load locations", Toast.LENGTH_SHORT).show()
        }
    }


    private fun checkIfEmailApproved(email: String) {
        if (selectedLocationKey.isNullOrEmpty() || selectedRestaurantKey.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a valid restaurant", Toast.LENGTH_SHORT).show()
            return
        }

        // Reference the APPROVED_ADMINS node for the selected location
        val approvedAdminsRef = database!!.child("location")
            .child(selectedLocationKey!!)
            .child("APPROVED_ADMINS")

        Log.d(TAG, "Checking if email is approved for location: $selectedLocationKey, restaurantKey: $selectedRestaurantKey")

        approvedAdminsRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val adminEmail = snapshot.child("email").getValue(String::class.java)

                Log.d(TAG, "Retrieved email: $adminEmail")

                // Validate email and restaurantKey together
                if (adminEmail == email) {
                    database!!.child("location").child(selectedLocationKey!!)
                        .get().addOnSuccessListener { locationSnapshot ->
                            val actualRestaurantKey = locationSnapshot.child("restaurantKey").getValue(String::class.java)
                            if (actualRestaurantKey == selectedRestaurantKey) {
                                Log.d(TAG, "Email and restaurant key approved: $email")
                                createAccount(email, password!!)
                            } else {
                                Log.w(TAG, "Restaurant key mismatch: expected $selectedRestaurantKey, found $actualRestaurantKey")
                                Toast.makeText(this, "Email not approved for this restaurant.", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener { error ->
                            Log.e(TAG, "Error validating restaurant key: ${error.message}")
                            Toast.makeText(this, "Error validating restaurant key.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Log.w(TAG, "Email not approved: $email")
                    Toast.makeText(this, "Email not approved for this restaurant.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w(TAG, "No approved admins found for location: $selectedLocationKey")
                Toast.makeText(this, "No approved admins found for this location.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { error ->
            Log.e(TAG, "Error checking approved admins: ${error.message}")
            Toast.makeText(this, "Error checking email approval.", Toast.LENGTH_SHORT).show()
        }
    }



    private fun createAccount(email: String, password: String) {
        auth!!.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task: Task<AuthResult?> ->
                if (task.isSuccessful) {
                    Log.d(
                        TAG,
                        "Account created successfully: $email"
                    )
                    saveUserData()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    Log.e(
                        TAG,
                        "Failed to create account: " + task.exception!!.message
                    )
                    Toast.makeText(
                        this,
                        "Failed to create account: " + task.exception!!.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun saveUserData() {
        val userId = if (auth!!.currentUser != null) auth!!.currentUser!!.uid else null
        if (userId == null) {
            Log.e(TAG, "Failed to save user data: User ID is null.")
            return
        }

        val user = UserModel(
            userName,
            ownerName,
            phoneNumber,
            email,
            password,
            selectedRestaurantName,
            selectedRestaurantKey
        )

        database!!.child("restaurants").child("admins").child(userId).setValue(user)
            .addOnCompleteListener { task: Task<Void?> ->
                if (task.isSuccessful) {
                    Log.d(
                        TAG,
                        "User data saved successfully: $userId"
                    )
                    Toast.makeText(
                        this,
                        "User data saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.e(
                        TAG,
                        "Failed to save user data: " + task.exception!!.message
                    )
                    Toast.makeText(
                        this,
                        "Failed to save user data: " + task.exception!!.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.length == 9 && phone.matches("\\d+".toRegex())
    }

    companion object {
        private const val TAG = "SignUpActivity"
    }
}
