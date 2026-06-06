package com.example.unifiltered.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unifiltered.model.Comment
import com.example.unifiltered.model.ThreadedComment
import com.example.unifiltered.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PostDetailViewModel : ViewModel() {
    private val repository = PostRepository()

    // FIXED: Now holds ThreadedComment instead of normal Comment
    private val _comments = MutableStateFlow<List<ThreadedComment>>(emptyList())
    val comments: StateFlow<List<ThreadedComment>> = _comments

    fun loadComments(postId: String) {
        viewModelScope.launch {
            repository.getComments(postId).collect { commentList ->
                // Sort them into a Reddit thread before giving them to the UI!
                _comments.value = buildThreadedList(commentList)
            }
        }
    }

    // Takes a raw list from Firestore and turns it into a perfectly ordered Reddit thread
    private fun buildThreadedList(allComments: List<Comment>): List<ThreadedComment> {
        val commentMap = allComments.groupBy { it.parentId }
        val result = mutableListOf<ThreadedComment>()

        fun addChildren(parentId: String?, depth: Int) {
            val children = commentMap[parentId]?.sortedBy { it.timestamp } ?: return
            for (child in children) {
                result.add(ThreadedComment(child, depth))
                addChildren(child.commentId, depth + 1)
            }
        }

        addChildren(null, 0)
        return result
    }

    // FIXED: Now accepts the parentId to pass to the repository
    fun addComment(postId: String, text: String, parentId: String? = null) {
        viewModelScope.launch {
            repository.addComment(postId, text, parentId)
        }
    }
}