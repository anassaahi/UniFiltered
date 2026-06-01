package com.example.unifiltered.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unifiltered.model.Society
import com.example.unifiltered.repository.PostRepository
import com.example.unifiltered.repository.SocietyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CreatePostViewModel : ViewModel() {
    private val repository = PostRepository()
    private val societyRepository = SocietyRepository()

    private val _postState = MutableStateFlow<CreatePostState>(CreatePostState.Idle)
    val postState: StateFlow<CreatePostState> = _postState

    private val _mySocieties = MutableStateFlow<List<Society>>(emptyList())
    val mySocieties: StateFlow<List<Society>> = _mySocieties

    fun loadMySocieties() {
        viewModelScope.launch {
            val result = societyRepository.getMyOwnedSocieties()
            if (result.isSuccess) {
                _mySocieties.value = result.getOrDefault(emptyList())
            }
        }
    }

    fun createPost(content: String, selectedSociety: Society? = null, imageUri: Uri? = null) {
        viewModelScope.launch {
            _postState.value = CreatePostState.Loading

            // UPDATED: Pass the imageUri to the repository
            val result = repository.createPost(
                content = content,
                societyId = selectedSociety?.societyId,
                societyName = selectedSociety?.name,
                imageUri = imageUri
            )

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