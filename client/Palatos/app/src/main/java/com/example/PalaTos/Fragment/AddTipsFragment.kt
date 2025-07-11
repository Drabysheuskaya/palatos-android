package com.example.PalaTos.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.PalaTos.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AddTipsFragment : Fragment() {

    private lateinit var waiterNameTextView: TextView
    private lateinit var tipAmountEditText: EditText
    private lateinit var payButton: Button
    private lateinit var backButton: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var restaurantKey: String? = null
    private var tableNumber: String? = null
    private var lastOrderId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_add_tips, container, false)


        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()


        waiterNameTextView = rootView.findViewById(R.id.waiterNameTextView)
        tipAmountEditText = rootView.findViewById(R.id.tipAmountEditText)
        payButton = rootView.findViewById(R.id.payButton)
        backButton = rootView.findViewById(R.id.backButton)


        arguments?.let {
            restaurantKey = it.getString("restaurantKey")
        }

        if (restaurantKey == null) {
            Toast.makeText(requireContext(), "Restaurant key is missing.", Toast.LENGTH_SHORT).show()
            return rootView
        }


        loadLatestOrderAndWaiter()


        backButton.setOnClickListener { navigateBack() }


        payButton.setOnClickListener {
            if (validateTipInput()) {
                saveTipToOrder()
            }
        }

        return rootView
    }

    private fun loadLatestOrderAndWaiter() {
        val userId = auth.currentUser?.uid ?: return
        val ordersRef = database.getReference("restaurants")
            .child(restaurantKey!!)
            .child("orders")

        ordersRef.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var latestOrderTime = ""
                    for (orderSnapshot in snapshot.children) {
                        val timeOfOrder = orderSnapshot.child("timeOfOrder").getValue(String::class.java) ?: continue
                        if (timeOfOrder > latestOrderTime) {
                            latestOrderTime = timeOfOrder
                            lastOrderId = orderSnapshot.child("orderId").getValue(String::class.java)

                            val tableValue = orderSnapshot.child("tableNumber").value
                            tableNumber = when (tableValue) {
                                is Long -> tableValue.toString()
                                is String -> tableValue
                                else -> null
                            }
                        }
                    }

                    if (lastOrderId != null && tableNumber != null) {
                        loadWaiterName()
                    } else {
                        Toast.makeText(requireContext(), "No orders found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AddTipsFragment", "Failed to load latest order: ${error.message}")
                }
            })
    }

    private fun loadWaiterName() {
        if (restaurantKey == null || tableNumber == null) return

        val tableRef = database.getReference("restaurants")
            .child(restaurantKey!!)
            .child("tables")
            .child("table$tableNumber")

        tableRef.child("waiter").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val waiterName = snapshot.getValue(String::class.java)
                waiterNameTextView.text = waiterName ?: "Unknown"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AddTipsFragment", "Failed to load waiter name: ${error.message}")
            }
        })
    }

    private fun validateTipInput(): Boolean {
        val tipInput = tipAmountEditText.text.toString().trim()
        if (tipInput.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a tip amount.", Toast.LENGTH_SHORT).show()
            return false
        }

        val tipAmount = tipInput.toDoubleOrNull()
        if (tipAmount == null || tipAmount <= 0) {
            Toast.makeText(requireContext(), "Tip amount must be greater than zero.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveTipToOrder() {
        if (restaurantKey == null || lastOrderId == null || tableNumber == null) {
            Toast.makeText(requireContext(), "Missing order or restaurant information.", Toast.LENGTH_SHORT).show()
            return
        }

        val tipAmount = tipAmountEditText.text.toString().trim().toDouble()
        val tipData = mapOf(
            "tip" to tipAmount,
            "userId" to auth.currentUser?.uid,
            "waiter" to waiterNameTextView.text.toString(),
            "tableNumber" to tableNumber,
            "orderId" to lastOrderId
        )

        val orderRef = database.getReference("restaurants")
            .child(restaurantKey!!)
            .child("orders")
            .child(lastOrderId!!)

        orderRef.updateChildren(tipData).addOnSuccessListener {
            Toast.makeText(requireContext(), "Tip added successfully!", Toast.LENGTH_SHORT).show()
            navigateBack()
        }.addOnFailureListener { e ->
            Log.e("AddTipsFragment", "Failed to add tip: ${e.message}")
            Toast.makeText(requireContext(), "Failed to add tip.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateBack() {
        val historyFragment = HistoryFragment()
        val bundle = Bundle()
        bundle.putString("restaurantKey", restaurantKey)
        historyFragment.arguments = bundle

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, historyFragment)
            .addToBackStack(null)
            .commit()
    }
}
