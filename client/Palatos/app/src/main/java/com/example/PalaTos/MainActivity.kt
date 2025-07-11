package com.example.PalaTos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.PalaTos.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val navController = findNavController(R.id.fragmentContainerView)


        binding.bottomNavigationView.setupWithNavController(navController)


        handleIntentArguments(navController)
    }

    private fun handleIntentArguments(navController: NavController) {
        val restaurantKey = intent.getStringExtra("restaurantKey")
        val menuSection = intent.getStringExtra("menuSection")

        if (!restaurantKey.isNullOrEmpty() && !menuSection.isNullOrEmpty()) {
            val bundle = Bundle().apply {
                putString("restaurantKey", restaurantKey)
                putString("menuSection", menuSection)
            }
            navController.navigate(R.id.homeFragment, bundle)
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val navController = findNavController(R.id.fragmentContainerView)
        if (!navController.popBackStack()) {
            finish()
        }
    }
}
