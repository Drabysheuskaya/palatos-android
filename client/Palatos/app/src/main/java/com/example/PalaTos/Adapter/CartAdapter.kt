package com.example.PalaTos.Adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.PalaTos.Model.CartItems
import com.example.PalaTos.databinding.CartItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartAdapter(
    private val context: Context,
    private val foodNames: MutableList<String>,
    private val foodPrices: MutableList<String>,
    private val foodDescriptions: MutableList<String>,
    private val foodImagesUri: MutableList<String>,
    private val foodQuantities: MutableList<Int>,
    private val foodIngredients: MutableList<String>,
    private val estimatedTimes: MutableList<Int>,
    private val restaurantKey: String,
    private val onTotalPriceUpdated: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {


    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val userId = auth.currentUser?.uid.orEmpty()
    private val cartItemsReference = database.reference
        .child("users")
        .child(userId)
        .child("CartItems")
        .child(restaurantKey)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = foodNames.size

    fun calculateSubtotal(): Double {
        var subtotal = 0.0
        for (i in foodPrices.indices) {
            val price = foodPrices[i].replace(" PLN", "").toDoubleOrNull() ?: 0.0
            subtotal += price * foodQuantities[i]
        }
        return subtotal
    }


    fun getCartItems(): List<CartItems> {
        val items = mutableListOf<CartItems>()
        for (i in foodNames.indices) {
            items.add(
                CartItems(
                    foodName = foodNames[i],
                    foodPrice = foodPrices[i],
                    foodDescription = foodDescriptions[i],
                    foodImage = foodImagesUri[i],
                    foodIngredients = foodIngredients[i],
                    foodQuantity = foodQuantities[i],
                    estimatedTime = estimatedTimes[i]
                )
            )
        }
        return items
    }


    inner class CartViewHolder(private val binding: CartItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.apply {
                cartFoodName.text = foodNames[position]
                cartItemPrice.text = "${foodPrices[position]} "
                itemQuantity.text = foodQuantities[position].toString()

                Glide.with(context)
                    .load(Uri.parse(foodImagesUri[position]))
                    .into(cartImage)

                minusButton.setOnClickListener { decreaseQuantity(position) }
                plusButton.setOnClickListener { increaseQuantity(position) }
                deleteButton.setOnClickListener { deleteItem(position) }
            }
        }

        private fun increaseQuantity(position: Int) {
            if (foodQuantities[position] < 10) {
                foodQuantities[position]++
                binding.itemQuantity.text = foodQuantities[position].toString()
                updateQuantityInDatabase(position)
                onTotalPriceUpdated()
            } else {
                Toast.makeText(context, "Maximum quantity reached.", Toast.LENGTH_SHORT).show()
            }
        }

        private fun decreaseQuantity(position: Int) {
            if (foodQuantities[position] > 1) {
                foodQuantities[position]--
                binding.itemQuantity.text = foodQuantities[position].toString()
                updateQuantityInDatabase(position)
                onTotalPriceUpdated()
            } else {
                Toast.makeText(context, "Minimum quantity is 1.", Toast.LENGTH_SHORT).show()
            }
        }

        private fun deleteItem(position: Int) {
            getUniqueKeyAtPosition(position) { uniqueKey ->
                if (uniqueKey != null) {
                    cartItemsReference.child(uniqueKey).removeValue().addOnSuccessListener {
                        foodNames.removeAt(position)
                        foodPrices.removeAt(position)
                        foodDescriptions.removeAt(position)
                        foodImagesUri.removeAt(position)
                        foodQuantities.removeAt(position)
                        foodIngredients.removeAt(position)
                        notifyItemRemoved(position)
                        onTotalPriceUpdated()
                        Toast.makeText(context, "Item deleted successfully!", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun getUniqueKeyAtPosition(position: Int, onComplete: (String?) -> Unit) {
            cartItemsReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var uniqueKey: String? = null
                    snapshot.children.forEachIndexed { index, dataSnapshot ->
                        if (index == position) {
                            uniqueKey = dataSnapshot.key
                            return@forEachIndexed
                        }
                    }
                    onComplete(uniqueKey)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
                    onComplete(null)
                }
            })
        }

        private fun updateQuantityInDatabase(position: Int) {
            getUniqueKeyAtPosition(position) { uniqueKey ->
                if (uniqueKey != null) {
                    cartItemsReference.child(uniqueKey).child("foodQuantity")
                        .setValue(foodQuantities[position])
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to update quantity", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }
}
