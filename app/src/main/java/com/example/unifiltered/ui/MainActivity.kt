package com.example.unifiltered.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.unifiltered.R
import com.example.unifiltered.databinding.ActivityMainBinding
import com.example.unifiltered.utils.NetworkMonitor
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.launch

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
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        // Connect the BottomNavigationView to the NavController
        binding.bottomNavigationView.setupWithNavController(navController)

        val networkMonitor = NetworkMonitor(this)
        val offlineBanner = findViewById<View>(R.id.tvOfflineBanner) // Or use binding.tvOfflineBanner

        // Listen to the network in the background
        lifecycleScope.launch {
            networkMonitor.isConnected.collect { isOnline ->
                if (isOnline) {
                    // Hide the red banner
                    offlineBanner.visibility = View.GONE
                } else {
                    // Show the red banner
                    offlineBanner.visibility = View.VISIBLE
                }
            }
        }
    }
}