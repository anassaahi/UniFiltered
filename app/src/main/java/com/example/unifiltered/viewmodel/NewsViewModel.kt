package com.example.unifiltered.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unifiltered.model.Society
import com.example.unifiltered.repository.SocietyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewsViewModel : ViewModel() {
    private val repository = SocietyRepository()
    private val _societies = MutableStateFlow<List<Society>>(emptyList())
    val societies: StateFlow<List<Society>> = _societies

    init {
        fetchSocieties()
    }

    private fun fetchSocieties() {
        viewModelScope.launch {
            repository.getAllSocieties().collect { societyList ->
                _societies.value = societyList
            }
        }
    }
}