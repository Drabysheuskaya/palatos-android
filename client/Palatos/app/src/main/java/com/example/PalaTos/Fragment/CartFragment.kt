package com.example.PalaTos.Fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.PalaTos.Adapter.CartAdapter
import com.example.PalaTos.OrderInfoActivity
import com.example.PalaTos.RestaurantDetailActivity
import com.example.PalaTos.Model.CartItems
import com.example.PalaTos.Model.SharedViewModel
import com.example.PalaTos.databinding.FragmentCartBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartFragment : Fragment() {

    private lateinit var binding: FragmentCartBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var cartAdapter: CartAdapter
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var userId = ""

    private var subtotal = 0.0
    private var serviceCharge = 0.0
    private var total = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        userId = auth.currentUser?.uid ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(context, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show()
            Log.e("CartFragment", "User not authenticated")
            return binding.root
        }


        binding.backButton.setOnClickListener {
            navigateBackToRestaurantDetail()
        }


        sharedViewModel.restaurantKey.observe(viewLifecycleOwner) { restaurantKey ->
            if (restaurantKey.isNullOrEmpty()) {
                Toast.makeText(context, "No restaurant selected. Please select a restaurant.", Toast.LENGTH_SHORT).show()
                return@observe
            }


            loadCartItems(restaurantKey)
        }

        binding.proceedButton.setOnClickListener {
            proceedToOrderInfo()
        }

        return binding.root
    }

    private fun loadCartItems(restaurantKey: String) {
        val cartReference = database.reference
            .child("users")
            .child(userId)
            .child("CartItems")
            .child(restaurantKey)

        cartReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    retrieveCartItems(snapshot, restaurantKey)
                } else {
                    Toast.makeText(context, "No items in the cart for this restaurant.", Toast.LENGTH_SHORT).show()
                    Log.d("CartFragment", "No items found for restaurant: $restaurantKey")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load cart data.", Toast.LENGTH_SHORT).show()
                Log.e("CartFragment", "Error loading cart data: ${error.message}")
            }
        })
    }

    private fun retrieveCartItems(snapshot: DataSnapshot, restaurantKey: String) {
        val foodNames = mutableListOf<String>()
        val foodPrices = mutableListOf<String>()
        val foodDescriptions = mutableListOf<String>()
        val foodImagesUri = mutableListOf<String>()
        val foodIngredients = mutableListOf<String>()
        val foodQuantities = mutableListOf<Int>()
        val estimatedTimes = mutableListOf<Int>()

        subtotal = 0.0

        snapshot.children.forEach { foodSnapshot ->
            val cartItem = foodSnapshot.getValue(CartItems::class.java)
            val estimatedTime = foodSnapshot.child("estimatedTime").getValue(Int::class.java) ?: 0

            cartItem?.let {
                foodNames.add(it.foodName ?: "Unknown")
                foodPrices.add(it.foodPrice ?: "0 PLN")
                foodDescriptions.add(it.foodDescription ?: "No description")
                foodImagesUri.add(it.foodImage ?: "")
                foodIngredients.add(it.foodIngredients ?: "Unknown")
                foodQuantities.add(it.foodQuantity ?: 1)
                estimatedTimes.add(estimatedTime)
                subtotal += (it.foodPrice?.replace(" PLN", "")?.toDoubleOrNull() ?: 0.0) * (it.foodQuantity ?: 1)
            }
        }

        serviceCharge = subtotal * 0.10
        total = subtotal + serviceCharge
        updatePriceDisplay()

        cartAdapter = CartAdapter(
            requireContext(),
            foodNames,
            foodPrices,
            foodDescriptions,
            foodImagesUri,
            foodQuantities,
            foodIngredients,
            estimatedTimes,  // Pass estimated times
            restaurantKey,
            onTotalPriceUpdated = { recalculateTotal() }
        )

        binding.cartRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.cartRecyclerView.adapter = cartAdapter

        Log.d("CartFragment", "Cart adapter set with ${foodNames.size} items for restaurant: $restaurantKey")
    }

    private fun recalculateTotal() {
        subtotal = cartAdapter.calculateSubtotal()
        serviceCharge = subtotal * 0.10
        total = subtotal + serviceCharge
        updatePriceDisplay()
    }

    private fun updatePriceDisplay() {
        binding.subtotalText.text = "Sub-Total: %.2f PLN".format(subtotal)
        binding.serviceChargeText.text = "Service: %.2f PLN".format(serviceCharge)
        binding.totalText.text = "Total: %.2f PLN".format(total)
        Log.d("CartFragment", "Updated price display: Subtotal = $subtotal, Total = $total")
    }

    private fun proceedToOrderInfo() {
        val intent = Intent(requireContext(), OrderInfoActivity::class.java).apply {
            putExtra("TOTAL", total.toString())
            putExtra("SUBTOTAL", subtotal.toString())
            putExtra("SERVICE_CHARGE", serviceCharge.toString())
            putParcelableArrayListExtra("CART_ITEMS", ArrayList(cartAdapter.getCartItems()))
            putExtra("restaurantKey", sharedViewModel.restaurantKey.value)
        }
        startActivity(intent)
        Log.d("CartFragment", "Proceeded to order info with total: $total")
    }

    private fun navigateBackToRestaurantDetail() {
        val restaurantKey = sharedViewModel.restaurantKey.value
        if (restaurantKey.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Restaurant key is missing.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(requireContext(), RestaurantDetailActivity::class.java).apply {
            putExtra("restaurantKey", restaurantKey)
        }
        startActivity(intent)
        requireActivity().finish()
    }
}
