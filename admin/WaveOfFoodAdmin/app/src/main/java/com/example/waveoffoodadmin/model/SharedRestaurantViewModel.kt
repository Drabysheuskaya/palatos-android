package com.example.waveoffoodadmin.model


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedRestaurantViewModel : ViewModel() {
    private val _restaurantKey = MutableLiveData<String>()


    val restaurantKey: LiveData<String> get() = _restaurantKey


    fun setRestaurantKey(key: String) {
        _restaurantKey.value = key
    }
}
