package com.example.PalaTos.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.PalaTos.databinding.FragmentMenuBottomSheetBinding
import com.example.PalaTos.Model.MenuItem
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.example.PalaTos.Adapter.MenuAdapter
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MenuBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentMenuBottomSheetBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var menuItems: MutableList<MenuItem>
    private var restaurantKey: String? = null // Add restaurantKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            restaurantKey = it.getString("restaurantKey")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMenuBottomSheetBinding.inflate(inflater, container, false)
        binding.buttonBack.setOnClickListener {
            dismiss()
        }
        retrieveMenuItems()
        return binding.root
    }

    private fun retrieveMenuItems() {
        database = FirebaseDatabase.getInstance()
        val foodRef: DatabaseReference = database.reference.child("menu")
        menuItems = mutableListOf()

        foodRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (foodSnapshot in snapshot.children) {
                    val menuItem = foodSnapshot.getValue(MenuItem::class.java)
                    menuItem?.let { menuItems.add(it) }
                }
                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MenuBottomSheet", "Error fetching menu items: ${error.message}")
            }
        })
    }

    private fun setAdapter() {
        if (!restaurantKey.isNullOrEmpty() && menuItems.isNotEmpty()) {
            val adapter = MenuAdapter(menuItems, requireContext(), restaurantKey!!)
            binding.menuRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.menuRecyclerView.adapter = adapter
        } else {
            Log.d("MenuBottomSheet", "No data to set or restaurantKey missing")
        }
    }

    companion object {
    }
}

