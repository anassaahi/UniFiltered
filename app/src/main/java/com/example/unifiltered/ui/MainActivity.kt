package com.example.unifiltered.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.unifiltered.R
import com.example.unifiltered.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Added the missing 'var' keyword here!
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find the NavController (the engine that drives navigation)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Connect the BottomNavigationView to the NavController
        binding.bottomNavigationView.setupWithNavController(navController)
    }
}