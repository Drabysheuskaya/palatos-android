package com.example.waveoffoodadmin.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.waveoffoodadmin.DetailsActivity
import com.example.waveoffoodadmin.databinding.MenuItemBinding
import com.example.waveoffoodadmin.model.Items

class MenuAdapter(
    private val itemsList: MutableList<Items>,
    private val context: Context,
    private val restaurantKey: String,
    private val onDeleteItem: (Items) -> Unit,
    private val onEditItem: (Items) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(itemsList[position])
    }

    override fun getItemCount(): Int = itemsList.size

    inner class MenuViewHolder(private val binding: MenuItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Set up Delete Button functionality
            binding.deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = itemsList[position]
                    Toast.makeText(
                        context,
                        "Confirm deletion of ${item.foodName}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDeleteItem(item) // Trigger the deletion callback
                }
            }


            binding.editButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditItem(itemsList[position]) // Trigger the editing callback
                }
            }
        }

        fun bind(item: Items) {
            // Set food name and price
            binding.foodName.text = item.foodName
            binding.foodPrice.text = item.foodPrice

            // Load food image using Glide
            if (!item.foodImage.isNullOrEmpty()) {
                Glide.with(context)
                    .load(Uri.parse(item.foodImage))
                    .into(binding.foodImage)
            } else {
                binding.foodImage.setImageResource(android.R.color.transparent)
            }


            binding.root.setOnClickListener {
                openDetailsActivity(item)
            }
        }

        private fun openDetailsActivity(item: Items) {
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", item.foodName)
                putExtra("MenuItemImage", item.foodImage)
                putExtra("MenuItemPrice", item.foodPrice)
                putExtra("MenuItemDescription", item.foodDescription)
                putExtra("MenuItemIngredients", item.foodIngredients)
                putExtra("MenuItemEstimatedTime", item.estimatedTime ?: 0)
                putExtra("restaurantKey", restaurantKey)
            }
            context.startActivity(intent)
        }
    }
}
