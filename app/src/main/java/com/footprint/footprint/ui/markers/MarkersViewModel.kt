package com.footprint.footprint.ui.markers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.footprint.footprint.data.local.AppDatabase
import com.footprint.footprint.data.local.entity.MarkerEntity
import com.footprint.footprint.data.repository.MarkerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MarkersUiState(
    val markers: List<MarkerEntity> = emptyList(),
    val isLoading: Boolean = true,
    val selectedMarker: MarkerEntity? = null
)

class MarkersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MarkerRepository
    
    private val _uiState = MutableStateFlow(MarkersUiState())
    val uiState: StateFlow<MarkersUiState> = _uiState.asStateFlow()
    
    init {
        val database = AppDatabase.getInstance(application)
        repository = MarkerRepository(database.markerDao())
        
        viewModelScope.launch {
            repository.getAllMarkers().collect { markers ->
                _uiState.value = _uiState.value.copy(
                    markers = markers,
                    isLoading = false
                )
            }
        }
    }
    
    fun selectMarker(marker: MarkerEntity?) {
        _uiState.value = _uiState.value.copy(selectedMarker = marker)
    }
    
    fun deleteMarker(marker: MarkerEntity) {
        viewModelScope.launch {
            repository.deleteMarker(marker)
        }
    }
    
    fun updateMarker(marker: MarkerEntity) {
        viewModelScope.launch {
            repository.updateMarker(marker)
        }
    }
}
