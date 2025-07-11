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
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.example.PalaTos.Adapter.MenuAdapter
import com.example.PalaTos.Model.MenuItem
import com.example.PalaTos.Model.SharedViewModel
import com.example.PalaTos.R
import com.example.PalaTos.RestaurantDetailActivity
import com.example.PalaTos.databinding.FragmentHomeBinding
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var menuItems: MutableList<MenuItem>

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var menuSection: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val restaurantKey = it.getString("restaurantKey")
            menuSection = it.getString("menuSection")

            if (!restaurantKey.isNullOrEmpty()) {
                sharedViewModel.setRestaurantKey(restaurantKey)
                Log.d("HomeFragment", "Restaurant Key set: $restaurantKey")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up back button click listener
        binding.backButton.setOnClickListener {
            navigateBackToRestaurantDetail()
        }

        sharedViewModel.restaurantKey.observe(viewLifecycleOwner) { restaurantKey ->
            if (restaurantKey.isNullOrEmpty() || menuSection.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Invalid arguments. Please restart the app.", Toast.LENGTH_LONG).show()
                return@observe
            }

            binding.sectionTitle.text = menuSection
            fetchMenuSectionItems(restaurantKey)
        }
    }


    private fun fetchMenuSectionItems(restaurantKey: String) {
        database = FirebaseDatabase.getInstance()
        val sectionRef = database.reference
            .child("restaurants")
            .child(restaurantKey)
            .child("menu")
            .child(menuSection!!)
            .child("items")

        menuItems = mutableListOf()

        sectionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    snapshot.children.mapNotNullTo(menuItems) {
                        it.getValue(MenuItem::class.java)
                    }
                    setMenuItemsAdapter(menuItems, restaurantKey)
                } else {
                    Toast.makeText(requireContext(), "No items found for this section.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setMenuItemsAdapter(items: List<MenuItem>, restaurantKey: String) {
        binding.sectionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.sectionRecyclerView.adapter = MenuAdapter(items, requireContext(), restaurantKey)
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
