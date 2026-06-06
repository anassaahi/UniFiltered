package com.example.unifiltered.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unifiltered.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            finish()
            return
        }

        // 1. Fetch current data and fill the text boxes
        loadCurrentProfileData(currentUserId)

        // 2. Handle the Back Button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 3. Handle the Save Button
        binding.btnSaveProfile.setOnClickListener {
            saveProfileData(currentUserId)
        }
    }

    private fun loadCurrentProfileData(userId: String) {
        binding.progressBarEdit.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val document = db.collection("users").document(userId).get().await()

                // Populate fields if data exists
                binding.etEditName.setText(document.getString("name") ?: "")
                binding.etEditBio.setText(document.getString("bio") ?: "")
                binding.etEditDepartment.setText(document.getString("department") ?: "")
                binding.etEditBatch.setText(document.getString("batch") ?: "")

            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarEdit.visibility = View.GONE
            }
        }
    }

    private fun saveProfileData(userId: String) {
        val newName = binding.etEditName.text.toString().trim()
        val newBio = binding.etEditBio.text.toString().trim()
        val newDept = binding.etEditDepartment.text.toString().trim()
        val newBatch = binding.etEditBatch.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBarEdit.visibility = View.VISIBLE
        binding.btnSaveProfile.isEnabled = false

        lifecycleScope.launch {
            try {
                // Map the new data
                val updates = mapOf(
                    "name" to newName,
                    "bio" to newBio,
                    "department" to newDept,
                    "batch" to newBatch
                )

                // Push updates to Firestore
                db.collection("users").document(userId).update(updates).await()

                Toast.makeText(this@EditProfileActivity, "Profile Updated!", Toast.LENGTH_SHORT).show()
                finish() // Close the activity and go back to the Profile screen

            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBarEdit.visibility = View.GONE
                binding.btnSaveProfile.isEnabled = true
            }
        }
    }
}