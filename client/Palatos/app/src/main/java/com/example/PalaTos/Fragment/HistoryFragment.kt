package com.example.PalaTos.Fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.PalaTos.Adapter.OrderAdapter
import com.example.PalaTos.Model.SharedViewModel
import com.example.PalaTos.Model.Order
import com.example.PalaTos.Model.OrderItem
import com.example.PalaTos.R
import com.example.PalaTos.RestaurantDetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryFragment : Fragment() {

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var noOrdersText: TextView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var listOfOrders: MutableList<Order> = mutableListOf()

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_history, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        ordersRecyclerView = rootView.findViewById(R.id.ordersRecyclerView)
        noOrdersText = rootView.findViewById(R.id.noOrdersText)
        val backButton: View = rootView.findViewById(R.id.backButton)
        val feedbackButton: View = rootView.findViewById(R.id.feedbackButton)
        val addTipsButton: View = rootView.findViewById(R.id.addTipsButton)

        setupRecyclerView()
        retrieveOrderHistory()


        backButton.setOnClickListener {
            navigateBackToRestaurantDetail()
        }


        feedbackButton.setOnClickListener {
            navigateToFeedbackFragment()
        }


        addTipsButton.setOnClickListener {
            navigateToAddTipsFragment()
        }


        handleBackButton()

        return rootView
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(listOfOrders) { order ->
            markOrderAsPaid(order)
        }

        ordersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }
    }

    private fun markOrderAsPaid(order: Order) {
        val restaurantKey = sharedViewModel.restaurantKey.value ?: return
        val orderRef = database.reference.child("restaurants").child(restaurantKey).child("orders").child(order.orderId)

        orderRef.child("status").setValue("Paid").addOnSuccessListener {
            Log.d("HistoryFragment", "Order ${order.orderId} marked as Paid")
            listOfOrders.find { it.orderId == order.orderId }?.apply {
                status = "Paid"
            }
            orderAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Log.e("HistoryFragment", "Failed to mark order as Paid: ${it.message}")
        }
    }

    private fun retrieveOrderHistory() {
        val currentUserId = auth.currentUser?.uid ?: return
        val restaurantKey = sharedViewModel.restaurantKey.value ?: return
        val ordersRef = database.reference.child("restaurants").child(restaurantKey).child("orders")

        ordersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listOfOrders.clear()
                if (snapshot.exists()) {
                    for (orderSnapshot in snapshot.children) {
                        val userId = orderSnapshot.child("userId").getValue(String::class.java) ?: ""
                        if (userId == currentUserId) {
                            val orderId = orderSnapshot.child("orderId").getValue(String::class.java) ?: ""
                            val tableValue = orderSnapshot.child("tableNumber").value
                            val tableNumber = when (tableValue) {
                                is Long -> tableValue.toInt()
                                is String -> tableValue.toIntOrNull() ?: 0
                                else -> 0
                            }
                            val timeOfOrder = orderSnapshot.child("timeOfOrder").getValue(String::class.java) ?: ""
                            val subtotal = String.format("%.2f", orderSnapshot.child("subtotal").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0)
                            val service = String.format("%.2f", orderSnapshot.child("service").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0)
                            val total = String.format("%.2f", orderSnapshot.child("total").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0)
                            val status = orderSnapshot.child("status").getValue(String::class.java) ?: "Pending"
                            val estimatedTime = orderSnapshot.child("estimatedTime").getValue(Int::class.java) ?: 0

                            val itemsSnapshot = orderSnapshot.child("items")
                            val items = mutableListOf<OrderItem>()

                            for (itemSnapshot in itemsSnapshot.children) {
                                val name = itemSnapshot.child("name").getValue(String::class.java) ?: ""
                                val price = itemSnapshot.child("price").getValue(String::class.java) ?: "0.0"
                                val quantity = itemSnapshot.child("quantity").getValue(Int::class.java) ?: 0
                                val itemEstimatedTime = itemSnapshot.child("estimatedTime").getValue(Int::class.java) ?: 0

                                items.add(OrderItem(name, price, quantity, itemEstimatedTime))
                            }

                            val order = Order(
                                orderId = orderId,
                                userId = userId,
                                tableNumber = tableNumber,
                                waiter = "Loading...",
                                timeOfOrder = timeOfOrder,
                                items = items,
                                subtotal = subtotal,
                                service = service,
                                total = total,
                                status = status,
                                estimatedTime = estimatedTime
                            )

                            listOfOrders.add(order)
                        }
                    }
                    listOfOrders.sortByDescending { it.timeOfOrder }
                    orderAdapter.notifyDataSetChanged()
                    noOrdersText.visibility = if (listOfOrders.isEmpty()) View.VISIBLE else View.GONE
                    fetchWaiterNames(restaurantKey)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HistoryFragment", "Failed to retrieve order history: ${error.message}")
            }
        })
    }



    private fun fetchWaiterNames(restaurantKey: String) {
        val tablesRef = database.reference.child("restaurants").child(restaurantKey).child("tables")

        tablesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val waiterMap = mutableMapOf<Int, String>()

                for (tableSnapshot in snapshot.children) {
                    val tableValue = tableSnapshot.child("tableNumber").value
                    val tableNumber = when (tableValue) {
                        is Long -> tableValue.toInt()
                        is String -> tableValue.toIntOrNull() ?: continue
                        else -> continue
                    }

                    val waiterName = tableSnapshot.child("waiter").getValue(String::class.java) ?: "Unknown"
                    waiterMap[tableNumber] = waiterName
                }

                listOfOrders.forEach { order ->
                    order.waiter = waiterMap[order.tableNumber] ?: "Unknown"
                }
                orderAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HistoryFragment", "Failed to retrieve waiter names: ${error.message}")
            }
        })
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

    private fun handleBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToRestaurantDetail()
            }
        })
    }

    fun navigateToFeedbackFragment() {
        val feedbackFragment = FeedbackFragment()
        val bundle = Bundle().apply {
            putString("restaurantKey", sharedViewModel.restaurantKey.value)
            putString("userId", auth.currentUser?.uid)
        }
        feedbackFragment.arguments = bundle

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, feedbackFragment)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToAddTipsFragment() {
        val addTipsFragment = AddTipsFragment()
        val bundle = Bundle().apply {
            putString("restaurantKey", sharedViewModel.restaurantKey.value)
        }
        addTipsFragment.arguments = bundle

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, addTipsFragment)
            .addToBackStack(null)
            .commit()
    }
}
