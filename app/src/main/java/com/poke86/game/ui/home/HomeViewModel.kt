package com.poke86.game.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poke86.game.domain.model.Category
import com.poke86.game.domain.model.Game
import com.poke86.game.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val categories: StateFlow<List<Category>> =
        MutableStateFlow(repository.getCategories()).asStateFlow()

    val filteredGames: StateFlow<List<Game>> = combine(
        MutableStateFlow(repository.getGames()),
        _selectedCategory
    ) { games, category ->
        if (category == "all") games
        else games.filter { category in it.categories }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = repository.getGames()
    )

    fun onCategorySelected(categoryId: String) {
        _selectedCategory.value = categoryId
    }
}
