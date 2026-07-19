package com.proflix.feature.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proflix.database.dao.FavoriteDao
import com.proflix.database.entity.FavoriteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeFavorite(animeId: String) {
        viewModelScope.launch {
            favoriteDao.deleteFavorite(animeId)
        }
    }
}
