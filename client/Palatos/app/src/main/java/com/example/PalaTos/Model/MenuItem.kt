package com.example.PalaTos.Model


import com.google.firebase.database.PropertyName

data class MenuItem(
    @get:PropertyName("foodName") @set:PropertyName("foodName") var foodName: String? = null,
    @get:PropertyName("foodPrice") @set:PropertyName("foodPrice") var foodPrice: String? = null,
    @get:PropertyName("foodImage") @set:PropertyName("foodImage") var foodImage: String? = null,
    @get:PropertyName("foodDescription") @set:PropertyName("foodDescription") var foodDescription: String? = null,
    @get:PropertyName("foodIngredients") @set:PropertyName("foodIngredients") var foodIngredients: String? = null,
    @get:PropertyName("restaurantKey") @set:PropertyName("restaurantKey") var restaurantKey: String? = null,
    @get:PropertyName("estimatedTime") @set:PropertyName("estimatedTime") var estimatedTime: Int? = null
)

