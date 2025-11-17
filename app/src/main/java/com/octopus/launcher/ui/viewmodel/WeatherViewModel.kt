package com.octopus.launcher.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.octopus.launcher.data.WeatherInfo
import com.octopus.launcher.data.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository(application)
    
    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)
    val weatherInfo: StateFlow<WeatherInfo?> = _weatherInfo.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        // Load cached weather immediately (synchronous) to avoid showing default values
        viewModelScope.launch {
            try {
                // Try to get cached weather first (fast, synchronous operation)
                val cached = repository.getCachedWeather()
                if (cached != null) {
                    _weatherInfo.value = cached
                    android.util.Log.d("WeatherViewModel", "Initial weather loaded from cache: ${cached.temperature}°, ${cached.condition}")
                    // Still load fresh weather in background
                    loadWeather()
                } else {
                    // If no cache, force load immediately
                    android.util.Log.d("WeatherViewModel", "No cached weather found, loading fresh data...")
                    loadWeather()
                }
            } catch (e: Exception) {
                android.util.Log.e("WeatherViewModel", "Error loading initial weather", e)
                // On error, try to load fresh weather
                loadWeather()
            }
        }
        
        // Update weather periodically (every 5 minutes)
        // This coroutine will be cancelled when ViewModel is cleared
        viewModelScope.launch {
            try {
                while (true) {
                    delay(300000) // 5 minutes
                    android.util.Log.d("WeatherViewModel", "Periodic weather update triggered")
                    loadWeather(forceRefresh = true)
                }
            } catch (e: CancellationException) {
                // Expected when ViewModel is cleared - just exit
                throw e
            }
        }
    }
    
    fun loadWeather(forceRefresh: Boolean = false) {
        // Don't prevent loading if already loading - allow refresh
        // The check was preventing periodic updates from working
        viewModelScope.launch {
            // Only skip if we're already loading AND we have valid weather info AND not forcing refresh
            if (!forceRefresh && _isLoading.value && _weatherInfo.value != null) {
                android.util.Log.d("WeatherViewModel", "Skipping load - already loading and have weather info")
                return@launch
            }
            
            _isLoading.value = true
            try {
                val weather = repository.getWeatherInfo(forceRefresh = forceRefresh)
                _weatherInfo.value = weather
                android.util.Log.d("WeatherViewModel", "Weather loaded: ${weather.temperature}°, ${weather.condition}, ${weather.city}")
            } catch (e: Exception) {
                android.util.Log.e("WeatherViewModel", "Error loading weather", e)
                e.printStackTrace()
                // Keep existing weather info on error, don't reset to null
                // If no weather info exists, try to get cached from repository
                if (_weatherInfo.value == null) {
                    try {
                        // Try to get cached weather directly (non-suspend)
                        val cached = repository.getCachedWeather()
                        if (cached != null) {
                            _weatherInfo.value = cached
                            android.util.Log.d("WeatherViewModel", "Using cached weather after error: ${cached.temperature}°, ${cached.condition}")
                        } else {
                            // Last resort - try repository again (it will return fallback)
                            val fallback = repository.getWeatherInfo(forceRefresh = false)
                            _weatherInfo.value = fallback
                        }
                    } catch (e2: Exception) {
                        android.util.Log.e("WeatherViewModel", "Error getting cached weather", e2)
                        // Ignore - keep null or existing value
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshWeather() {
        loadWeather(forceRefresh = true)
    }
}

