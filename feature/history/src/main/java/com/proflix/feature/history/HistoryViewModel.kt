package com.proflix.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proflix.database.dao.HistoryDao
import com.proflix.database.entity.HistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: HistoryDao
) : ViewModel() {

    val history: StateFlow<List<HistoryEntity>> = historyDao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearHistory() {
        viewModelScope.launch {
            historyDao.clearHistory()
        }
    }
}
