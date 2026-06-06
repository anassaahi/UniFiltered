package com.example.unifiltered.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unifiltered.model.Post
import com.example.unifiltered.repository.PostRepository
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// A simple sealed class to manage the AI UI state
sealed class AiSummaryState {
    object Idle : AiSummaryState()
    object Loading : AiSummaryState()
    data class Success(val response: String) : AiSummaryState()
    data class Error(val message: String) : AiSummaryState()
}

class FeedViewModel : ViewModel() {
    private val repository = PostRepository()

    // Connect to Firebase Functions (Matching the "asia-south1" region from your logs)
    private val functions: FirebaseFunctions = Firebase.functions("asia-south1")

    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    private val _aiSummaryState = MutableStateFlow<AiSummaryState>(AiSummaryState.Idle)
    val aiSummaryState: StateFlow<AiSummaryState> = _aiSummaryState

    // This continues to instantly filter the visual feed locally as the user types
    val posts: StateFlow<List<Post>> = combine(_allPosts, _searchQuery) { postList, query ->
        if (query.isBlank()) {
            postList
        } else {
            postList.filter { post ->
                post.content.contains(query, ignoreCase = true) ||
                        post.authorName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        fetchPosts()
    }

    private fun fetchPosts() {
        viewModelScope.launch {
            repository.getAllPosts().collect { postList ->
                _allPosts.value = postList
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _aiSummaryState.value = AiSummaryState.Idle
        }
    }

    // Sends the query to the cloud for deep database/comment searching
    fun generateAiSummaryForSearch(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _aiSummaryState.value = AiSummaryState.Loading

            try {
                // Package the search text
                val data = hashMapOf("query" to query)

                // Call the 'smartSearch' cloud function
                val result = functions
                    .getHttpsCallable("smartSearch")
                    .call(data)
                    .await()

                // Extract the AI's answer from the server response
                val resultData = result.data as? Map<*, *>
                val aiResponse = resultData?.get("response") as? String ?: "No response generated."

                _aiSummaryState.value = AiSummaryState.Success(aiResponse)

            } catch (e: Exception) {
                e.printStackTrace()
                _aiSummaryState.value = AiSummaryState.Error("Failed to reach J.A.R.V.I.S.")
            }
        }
    }

    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        viewModelScope.launch {
            repository.toggleLike(postId, isCurrentlyLiked)
        }
    }
}