package com.example.waveoffoodadmin.model


import com.google.firebase.database.PropertyName

data class Items(
    @get:PropertyName("foodName") @set:PropertyName("foodName") var foodName: String? = null,
    @get:PropertyName("foodPrice") @set:PropertyName("foodPrice") var foodPrice: String? = null,
    @get:PropertyName("foodImage") @set:PropertyName("foodImage") var foodImage: String? = null,
    @get:PropertyName("foodDescription") @set:PropertyName("foodDescription") var foodDescription: String? = null,
    @get:PropertyName("foodIngredients") @set:PropertyName("foodIngredients") var foodIngredients: String? = null,
    @get:PropertyName("restaurantKey") @set:PropertyName("restaurantKey") var restaurantKey: String? = null, // Add this field
    @get:PropertyName("estimatedTime") @set:PropertyName("estimatedTime") var estimatedTime: Int? = null,
    var firebaseKey: String? = null // Add this field
)

