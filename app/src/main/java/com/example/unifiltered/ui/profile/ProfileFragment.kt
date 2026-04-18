package com.example.unifiltered.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.unifiltered.databinding.FragmentProfileBinding
import com.example.unifiltered.repository.PostRepository
import com.example.unifiltered.ui.AdminDashboardActivity
import com.example.unifiltered.ui.auth.LoginActivity
import com.example.unifiltered.ui.feed.PostAdapter
import com.example.unifiltered.ui.feed.PostDetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var postAdapter: PostAdapter
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val postRepo = PostRepository()
    private val storage = FirebaseStorage.getInstance()

    // Permission launcher for camera and storage
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = permissions[
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES
            else
                Manifest.permission.READ_EXTERNAL_STORAGE
        ] ?: false

        if (cameraGranted && storageGranted) {
            showImagePickerDialog()
        } else {
            Toast.makeText(requireContext(), "Permissions required to upload image", Toast.LENGTH_SHORT).show()
        }
    }

    // Image picker launcher (gallery)
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri ->
            lifecycleScope.launch {
                uploadProfileImage(imageUri)
            }
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            lifecycleScope.launch {
                uploadProfileImageBitmap(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid ?: ""
        val userEmail = currentUser?.email ?: ""

        // 1. Basic UI Setup & Admin Check
        binding.tvProfileEmail.text = userEmail
        if (userEmail == "anas@gmail.com" || userEmail == "anasfaizsahi6@gmail.com") {
            binding.btnAdminLair.visibility = View.VISIBLE
        }

        binding.btnAdminLair.setOnClickListener {
            startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
        }

        // 2. Setup Profile Image Upload
        setupProfileImageUpload(currentUserId)

        // 3. Fetch User's Name and Profile Image from Firestore
        lifecycleScope.launch {
            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val name = userDoc.getString("name") ?: "Unknown User"
                val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""

                binding.tvProfileName.text = name

                // Load profile image if exists
                if (profileImageUrl.isNotEmpty()) {
                    binding.ivProfileImage.load(profileImageUrl) {
                        crossfade(true)
                    }
                    binding.tvUploadHint.visibility = View.GONE
                } else {
                    binding.tvUploadHint.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.tvProfileName.text = "Student"
            }
        }

        // 4. Setup RecyclerView for "My Posts"
        postAdapter = PostAdapter(
            currentUserId = currentUserId,
            onPostClick = { clickedPost ->
                val intent = Intent(requireContext(), PostDetailActivity::class.java).apply {
                    putExtra("POST_ID", clickedPost.postId)
                    putExtra("AUTHOR_NAME", clickedPost.authorName)
                    putExtra("CONTENT", clickedPost.content)
                    putExtra("LIKES_COUNT", clickedPost.likedBy.size)
                }
                startActivity(intent)
            },
            onLikeClick = { clickedPost, isCurrentlyLiked ->
                lifecycleScope.launch {
                    postRepo.toggleLike(clickedPost.postId, isCurrentlyLiked)
                }
            }
        )

        binding.recyclerViewMyPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // 5. Load the Posts & Hide Spinner
        lifecycleScope.launch {
            postRepo.getMyPosts(currentUserId).collect { posts ->
                binding.profileProgressBar.visibility = View.GONE
                postAdapter.submitList(posts)
            }
        }

        // 6. Log Out Logic
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            // Clear the activity stack and jump back to Login
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    private fun setupProfileImageUpload(currentUserId: String) {
        binding.btnUploadImage.setOnClickListener {
            checkPermissionsAndShowPicker()
        }

        // Also allow clicking on the image itself to upload
        binding.ivProfileImage.setOnClickListener {
            checkPermissionsAndShowPicker()
        }
    }

    private fun checkPermissionsAndShowPicker() {
        val permissionsNeeded = mutableListOf<String>()

        // Camera permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        // Storage permission
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                storagePermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(storagePermission)
        }

        if (permissionsNeeded.isNotEmpty()) {
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Upload Profile Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null) // Take photo
                    1 -> galleryLauncher.launch("image/*") // Pick from gallery
                    2 -> {} // Cancel
                }
            }
            .create()
            .show()
    }

    private suspend fun uploadProfileImage(imageUri: android.net.Uri) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return
            Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show()

            // Upload to Firebase Storage
            val fileName = "profile_images/${currentUserId}_${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child(fileName)

            val uploadTask = storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Update Firestore with the image URL
            db.collection("users").document(currentUserId).update(
                mapOf("profileImageUrl" to downloadUrl)
            ).await()

            // Update UI
            binding.ivProfileImage.load(downloadUrl) {
                crossfade(true)
            }
            binding.tvUploadHint.visibility = View.GONE

            Toast.makeText(requireContext(), "Profile image uploaded successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun uploadProfileImageBitmap(bitmap: Bitmap) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return
            Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show()

            // Convert bitmap to byte array
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            // Upload to Firebase Storage
            val fileName = "profile_images/${currentUserId}_${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child(fileName)

            storageRef.putBytes(imageData).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Update Firestore with the image URL
            db.collection("users").document(currentUserId).update(
                mapOf("profileImageUrl" to downloadUrl)
            ).await()

            // Update UI
            binding.ivProfileImage.load(downloadUrl) {
                crossfade(true)
            }
            binding.tvUploadHint.visibility = View.GONE

            Toast.makeText(requireContext(), "Profile image uploaded successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}