package com.example.PalaTos.Adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.PalaTos.DetailsActivity
import com.example.PalaTos.Model.MenuItem
import com.example.PalaTos.databinding.MenuItemBinding

class MenuAdapter(
    private val menuItems: List<MenuItem>,
    private val context: Context,
    private val restaurantKey: String
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    override fun getItemCount(): Int = menuItems.size

    inner class MenuViewHolder(private val binding: MenuItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.seeDetailsButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    openDetailsActivity(menuItems[position])
                }
            }
        }

        private fun openDetailsActivity(menuItem: MenuItem) {
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.foodName)
                putExtra("MenuItemImage", menuItem.foodImage)
                putExtra("MenuItemPrice", menuItem.foodPrice)
                putExtra("MenuItemDescription", menuItem.foodDescription)
                putExtra("MenuItemIngredients", menuItem.foodIngredients)
                putExtra("MenuItemEstimatedTime", menuItem.estimatedTime ?: 0)
                putExtra("restaurantKey", restaurantKey)
            }
            context.startActivity(intent)
        }


        fun bind(menuItem: MenuItem) {
            binding.apply {
                foodName.text = menuItem.foodName
                foodPrice.text = menuItem.foodPrice
                Glide.with(context)
                    .load(Uri.parse(menuItem.foodImage))
                    .into(foodImage)
            }
        }
    }
}
