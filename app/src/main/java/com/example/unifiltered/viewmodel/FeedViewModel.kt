package com.example.unifiltered.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unifiltered.model.Post
import com.example.unifiltered.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {
    private val repository = PostRepository()

    // Holds the RAW list of all posts directly from Firebase
    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())

    // Holds whatever text the user is currently typing in the search bar
    private val _searchQuery = MutableStateFlow("")

    // MAGIC HAPPENS HERE: We combine the raw posts and the search query.
    // Every time the user types a letter, this instantly recalculates!
    val posts: StateFlow<List<Post>> = combine(_allPosts, _searchQuery) { postList, query ->
        if (query.isBlank()) {
            postList // If search is empty, show everything
        } else {
            postList.filter { post ->
                // Check if the search text is in the post content OR the author's name
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

    // The Fragment will call this whenever the user presses a key
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        viewModelScope.launch {
            repository.toggleLike(postId, isCurrentlyLiked)
        }
    }
}