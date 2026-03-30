package com.example.unifiltered.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unifiltered.model.Post
import com.example.unifiltered.repository.PostRepository
import com.example.unifiltered.repository.SocietyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SocietyDetailViewModel : ViewModel() {
    private val _liveSociety = MutableStateFlow<com.example.unifiltered.model.Society?>(null)
    val liveSociety: StateFlow<com.example.unifiltered.model.Society?> = _liveSociety

    private val societyRepo = SocietyRepository()
    private val postRepo = PostRepository()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing

    private val _societyPosts = MutableStateFlow<List<Post>>(emptyList())
    val societyPosts: StateFlow<List<Post>> = _societyPosts

    fun checkFollowStatus(societyId: String) {
        viewModelScope.launch {
            _isFollowing.value = societyRepo.isUserFollowing(societyId)
        }
    }

    fun toggleFollow(societyId: String) {
        viewModelScope.launch {
            val currentState = _isFollowing.value
            val result = societyRepo.toggleFollow(societyId, currentState)
            if (result.isSuccess) {
                // Instantly flip the button state in the UI
                _isFollowing.value = !currentState
            }
        }
    }

    fun loadSocietyPosts(societyId: String) {
        viewModelScope.launch {
            postRepo.getPostsForSociety(societyId).collect { posts ->
                // Sort them locally (newest first)
                val sortedPosts = posts.sortedByDescending { it.timestamp }
                _societyPosts.value = sortedPosts
            }
        }
    }

    // 2. Add this function to start the listener
    fun listenToSociety(societyId: String) {
        viewModelScope.launch {
            societyRepo.getSocietyLive(societyId).collect { society ->
                _liveSociety.value = society
            }
        }
    }

    // We reuse the like function from the feed!
    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        viewModelScope.launch {
            postRepo.toggleLike(postId, isCurrentlyLiked)
        }
    }
}