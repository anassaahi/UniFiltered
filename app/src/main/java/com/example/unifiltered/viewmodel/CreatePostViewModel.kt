package com.example.unifiltered.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unifiltered.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CreatePostViewModel : ViewModel() {
    private val repository = PostRepository()

    private val _postState = MutableStateFlow<CreatePostState>(CreatePostState.Idle)
    val postState: StateFlow<CreatePostState> = _postState

    fun createPost(content: String) {
        viewModelScope.launch {
            _postState.value = CreatePostState.Loading
            val result = repository.createPost(content)

            if (result.isSuccess) {
                _postState.value = CreatePostState.Success
            } else {
                _postState.value = CreatePostState.Error(result.exceptionOrNull()?.message ?: "Failed to post")
            }
        }
    }
}

// State tracker specifically for the creation process
sealed class CreatePostState {
    object Idle : CreatePostState()
    object Loading : CreatePostState()
    object Success : CreatePostState()
    data class Error(val message: String) : CreatePostState()
}