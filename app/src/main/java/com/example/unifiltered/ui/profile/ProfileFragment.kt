package com.example.unifiltered.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.unifiltered.databinding.FragmentProfileBinding
import com.example.unifiltered.ui.AdminDashboardActivity
import com.example.unifiltered.ui.auth.LoginActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var isUploadingBanner = false
    // Permissions & Upload Launchers
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = permissions[if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        if (cameraGranted && storageGranted) showImagePickerDialog() else Toast.makeText(requireContext(), "Permissions required", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { lifecycleScope.launch { uploadProfileImage(it) } }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { lifecycleScope.launch { uploadProfileImageBitmap(it) } }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid ?: return
        val userEmail = currentUser.email ?: ""

        // Admin setup
        if (userEmail == "anas@gmail.com" || userEmail == "anasfaizsahi6@gmail.com") {
            binding.btnAdminLair.visibility = View.VISIBLE
        }
        binding.btnAdminLair.setOnClickListener { startActivity(Intent(requireContext(), AdminDashboardActivity::class.java)) }

        // Setup image uploads
        setupProfileImageUpload()

        // 1. Fetch User Data for Header
        lifecycleScope.launch {
            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()

                // Text fields
                binding.tvProfileName.text = userDoc.getString("name") ?: "Unknown User"
                binding.chipDepartment.text = userDoc.getString("department") ?: "BS Computer Science"
                binding.chipBatch.text = userDoc.getString("batch") ?: "Class of '26"
                binding.tvBio.text = userDoc.getString("bio") ?: "Write a bio to tell the campus who you are!"

                // ... (inside the lifecycleScope.launch where you set the text) ...
                val cred = userDoc.getLong("campusCred") ?: 0L
                binding.tvCampusCred.text = cred.toString() // Set the number

                // Trigger the flame animation!
                startCredEruptionAnimation()

                // Images
                val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                val coverImageUrl = userDoc.getString("coverImageUrl") ?: ""

                if (profileImageUrl.isNotEmpty()) binding.ivProfileImage.load(profileImageUrl) { crossfade(true) }
                if (coverImageUrl.isNotEmpty()) binding.ivCoverBanner.load(coverImageUrl) { crossfade(true) }

            } catch (e: Exception) {
                binding.tvProfileName.text = "Student"
            }
        }

        // 2. Setup the ViewPager & Tabs (The heavy lifting is now delegated here!)
        val pagerAdapter = ProfilePagerAdapter(this)
        binding.profileViewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.profileTabLayout, binding.profileViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Posts"
                1 -> "Comments"
                2 -> "Saved"
                else -> ""
            }
        }.attach()
// Open Edit Profile Screen
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        // Logout
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun setupProfileImageUpload() {
        // Profile Image Clicks
        binding.btnUploadImage.setOnClickListener {
            isUploadingBanner = false
            checkPermissionsAndShowPicker()
        }
        binding.ivProfileImage.setOnClickListener {
            isUploadingBanner = false
            checkPermissionsAndShowPicker()
        }

        // NEW: Cover Banner Click
        binding.ivCoverBanner.setOnClickListener {
            isUploadingBanner = true
            checkPermissionsAndShowPicker()
        }
    }

    private fun checkPermissionsAndShowPicker() {
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) permissionsNeeded.add(Manifest.permission.CAMERA)
        val storagePerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(requireContext(), storagePerm) != PackageManager.PERMISSION_GRANTED) permissionsNeeded.add(storagePerm)

        if (permissionsNeeded.isNotEmpty()) permissionLauncher.launch(permissionsNeeded.toTypedArray()) else showImagePickerDialog()
    }

    private fun showImagePickerDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Upload Profile Image")
            .setItems(arrayOf("Take Photo", "Choose from Gallery", "Cancel")) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch("image/*")
                }
            }.show()
    }

    // 1. For Gallery Uploads
    private suspend fun uploadProfileImage(imageUri: android.net.Uri) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return
            Toast.makeText(requireContext(), "Uploading...", Toast.LENGTH_SHORT).show()

            // Route to the correct folder and database field
            val folderName = if (isUploadingBanner) "cover_images" else "profile_images"
            val dbField = if (isUploadingBanner) "coverImageUrl" else "profileImageUrl"

            val storageRef = storage.reference.child("$folderName/${currentUserId}_${System.currentTimeMillis()}.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            db.collection("users").document(currentUserId).update(dbField, downloadUrl).await()

            // Update the correct UI element
            if (isUploadingBanner) {
                binding.ivCoverBanner.load(downloadUrl) { crossfade(true) }
            } else {
                binding.ivProfileImage.load(downloadUrl) { crossfade(true) }
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. For Camera Uploads
    private suspend fun uploadProfileImageBitmap(bitmap: Bitmap) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return
            Toast.makeText(requireContext(), "Uploading...", Toast.LENGTH_SHORT).show()

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

            // Route to the correct folder and database field
            val folderName = if (isUploadingBanner) "cover_images" else "profile_images"
            val dbField = if (isUploadingBanner) "coverImageUrl" else "profileImageUrl"

            val storageRef = storage.reference.child("$folderName/${currentUserId}_${System.currentTimeMillis()}.jpg")
            storageRef.putBytes(baos.toByteArray()).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            db.collection("users").document(currentUserId).update(dbField, downloadUrl).await()

            // Update the correct UI element
            if (isUploadingBanner) {
                binding.ivCoverBanner.load(downloadUrl) { crossfade(true) }
            } else {
                binding.ivProfileImage.load(downloadUrl) { crossfade(true) }
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun startCredEruptionAnimation() {
        val eruptionRing = binding.viewCredEruption

        // 1. Expand outward (X and Y axis)
        val scaleX = ObjectAnimator.ofFloat(eruptionRing, View.SCALE_X, 1f, 2.8f).apply {
            repeatCount = ObjectAnimator.INFINITE
            duration = 1500 // 1.5 seconds per pulse
        }
        val scaleY = ObjectAnimator.ofFloat(eruptionRing, View.SCALE_Y, 1f, 2.8f).apply {
            repeatCount = ObjectAnimator.INFINITE
            duration = 1500
        }

        // 2. Fade out as it expands
        val alpha = ObjectAnimator.ofFloat(eruptionRing, View.ALPHA, 0.8f, 0f).apply {
            repeatCount = ObjectAnimator.INFINITE
            duration = 1500
        }

        // 3. Play them all at the exact same time
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            start()
        }
    }
}