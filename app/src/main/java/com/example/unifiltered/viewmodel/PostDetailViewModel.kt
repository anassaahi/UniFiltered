package com.example.unifiltered.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unifiltered.model.Comment
import com.example.unifiltered.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PostDetailViewModel : ViewModel() {
    private val repository = PostRepository()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    // We pass the postId here so the ViewModel knows which post to listen to
    fun loadComments(postId: String) {
        viewModelScope.launch {
            repository.getComments(postId).collect { commentList ->
                _comments.value = commentList
            }
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            repository.addComment(postId, text)
        }
    }
}