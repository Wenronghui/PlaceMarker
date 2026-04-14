package com.footprint.footprint.ui.tracks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.footprint.footprint.data.local.AppDatabase
import com.footprint.footprint.data.local.entity.TrackEntity
import com.footprint.footprint.data.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TracksUiState(
    val tracks: List<TrackEntity> = emptyList(),
    val isLoading: Boolean = true
)

class TracksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TrackRepository
    
    private val _uiState = MutableStateFlow(TracksUiState())
    val uiState: StateFlow<TracksUiState> = _uiState.asStateFlow()
    
    init {
        val database = AppDatabase.getInstance(application)
        repository = TrackRepository(database.trackDao())
        
        viewModelScope.launch {
            repository.getAllTracks().collect { tracks ->
                _uiState.value = _uiState.value.copy(
                    tracks = tracks,
                    isLoading = false
                )
            }
        }
    }
    
    fun deleteTrack(track: TrackEntity) {
        viewModelScope.launch {
            repository.deleteTrack(track)
        }
    }
}
