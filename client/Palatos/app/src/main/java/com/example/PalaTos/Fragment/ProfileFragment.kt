package com.example.PalaTos.Fragment

import UserModel
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.PalaTos.Model.SharedViewModel
import com.example.PalaTos.RestaurantDetailActivity
import com.example.PalaTos.SignActivity
import com.example.PalaTos.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var restaurantKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)


        toggleFields(false)


        sharedViewModel.restaurantKey.observe(viewLifecycleOwner) { key ->
            if (!key.isNullOrEmpty()) {
                restaurantKey = key
                loadUserDataFromFirebase()
                listenForNameUpdates()
                Toast.makeText(requireContext(), "Restaurant Key: $key", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "No restaurant selected.", Toast.LENGTH_SHORT).show()
            }
        }


        binding.backButton.setOnClickListener {
            navigateBackToRestaurantDetails()
        }


        setUserData()


        binding.editProfileButton.setOnClickListener {
            toggleFields(true)
            binding.tableNumberInput.isEnabled = false
        }


        binding.saveInfoButton.setOnClickListener {
            if (validateInputs()) {
                saveUserDataWithTableFromOrderInfo()
                toggleFields(false)
            }
        }


        binding.logOutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), SignActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (restaurantKey != null) {
            loadUserDataFromFirebase()
        }
    }

    /**
     * Listens for real-time updates to the name field and updates the UI accordingly.
     */
    private fun listenForNameUpdates() {
        val userId = auth.currentUser?.uid
        if (userId != null && restaurantKey != null) {
            val nameRef = database.reference.child("users").child(userId).child("restaurants").child(restaurantKey!!).child("name")
            nameRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updatedName = snapshot.value?.toString() ?: "No Name"
                    binding.nameInput.setText(updatedName)
                    Log.d("ProfileFragment", "Name updated in real-time: $updatedName")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ProfileFragment", "Failed to listen for name updates: ${error.message}")
                }
            })
        }
    }

    /**
     * Loads the user data including the table number from Firebase.
     */
    private fun loadUserDataFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId != null && restaurantKey != null) {
            val restaurantRef = database.reference.child("users").child(userId).child("restaurants").child(restaurantKey!!)
            restaurantRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val tableNumber = snapshot.child("tableNumber").value?.toString() ?: ""
                        binding.tableNumberInput.setText(tableNumber)
                        Log.d("ProfileFragment", "Loaded table number: $tableNumber")
                    } else {
                        Toast.makeText(requireContext(), "User data not found!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ProfileFragment", "Failed to load user data: ${error.message}")
                }
            })
        }
    }

    /**
     * Toggles the editability of input fields.
     */
    private fun toggleFields(enable: Boolean) {
        binding.nameInput.isEnabled = enable
        binding.tableNumberInput.isEnabled = false
        binding.emailInput.isEnabled = enable
        binding.phoneInput.isEnabled = enable
        binding.passwordInput.isEnabled = enable
    }

    /**
     * Loads user data from Firebase and populates the fields.
     */
    private fun setUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userReference = database.getReference("users").child(userId)
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userProfile = snapshot.getValue(UserModel::class.java)
                        if (userProfile != null) {
                            binding.emailInput.setText(userProfile.email ?: "")
                            binding.phoneInput.setText(userProfile.phone ?: "")
                            binding.passwordInput.setText(userProfile.password ?: "")
                        } else {
                            Toast.makeText(requireContext(), "User data is empty!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "User data not found!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "User ID not found!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Validates user inputs.
     */
    private fun validateInputs(): Boolean {
        val phone = binding.phoneInput.text.toString()


        if (phone.length != 9 || !phone.all { it.isDigit() }) {
            binding.phoneInput.error = "Phone number must be exactly 9 digits"
            return false
        }

        return true
    }

    /**
     * Saves user data to Firebase, including the table number loaded from OrderInfoActivity.
     */
    private fun saveUserDataWithTableFromOrderInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null && restaurantKey != null) {
            val name = binding.nameInput.text.toString()
            val email = binding.emailInput.text.toString()
            val phone = binding.phoneInput.text.toString()
            val password = binding.passwordInput.text.toString()

            // Load table number and sync name with OrderInfoActivity
            val restaurantRef = database.reference.child("users").child(userId).child("restaurants").child(restaurantKey!!)
            restaurantRef.child("tableNumber").get().addOnSuccessListener { snapshot ->
                val tableNumber = snapshot.value?.toString() ?: ""
                binding.tableNumberInput.setText(tableNumber)

                val userData = mapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "password" to password
                )

                // Update general user data and restaurant-specific data
                val userReference = database.getReference("users").child(userId)
                val restaurantNameRef = restaurantRef.child("name")

                userReference.updateChildren(userData).addOnSuccessListener {
                    restaurantNameRef.setValue(name).addOnSuccessListener {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { error ->
                        Toast.makeText(requireContext(), "Failed to update name: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { error ->
                    Toast.makeText(requireContext(), "Failed to update profile: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { error ->
                Toast.makeText(requireContext(), "Failed to load table number: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("ProfileFragment", "Failed to load table number: ${error.message}")
            }
        } else {
            Toast.makeText(requireContext(), "User ID or restaurant key not found!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Navigates back to the RestaurantDetailActivity.
     */
    private fun navigateBackToRestaurantDetails() {
        val intent = Intent(requireContext(), RestaurantDetailActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
}
