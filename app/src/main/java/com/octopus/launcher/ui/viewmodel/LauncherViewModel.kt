package com.octopus.launcher.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.octopus.launcher.data.AppInfo
import com.octopus.launcher.data.AppRepository
import com.octopus.launcher.data.FavoritesRepository
import com.octopus.launcher.data.PopularAppsRepository
import com.octopus.launcher.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val popularAppsRepository = PopularAppsRepository(application)
    private val settingsRepository = SettingsRepository(application)
    
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    
    val favorites: StateFlow<Set<String>> = favoritesRepository.favorites
    val popularApps: StateFlow<List<String>> = popularAppsRepository.popularApps
    val popularAppsSet: StateFlow<Set<String>> = popularAppsRepository.popularAppsSet
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()
    
    private val _backgroundColors = MutableStateFlow<Triple<Color, Color, Color>>(
        Triple(
            settingsRepository.getBackgroundColor1(),
            settingsRepository.getBackgroundColor2(),
            settingsRepository.getBackgroundColor3()
        )
    )
    val backgroundColors: StateFlow<Triple<Color, Color, Color>> = _backgroundColors.asStateFlow()
    
    private val _backgroundImagePath = MutableStateFlow<String?>(settingsRepository.getBackgroundImagePath())
    val backgroundImagePath: StateFlow<String?> = _backgroundImagePath.asStateFlow()
    
    private val _backgroundBlurEnabled = MutableStateFlow(settingsRepository.isBackgroundBlurEnabled())
    val backgroundBlurEnabled: StateFlow<Boolean> = _backgroundBlurEnabled.asStateFlow()
    
    init {
        loadApps()
        viewModelScope.launch {
            _searchQuery.collect { query ->
                filterApps(query)
            }
        }
        
        // Preload background image on init
        viewModelScope.launch(Dispatchers.Default) {
            val imagePath = _backgroundImagePath.value
            if (imagePath != null) {
                val displayMetrics = getApplication<Application>().resources.displayMetrics
                com.octopus.launcher.ui.components.BackgroundImageCache.preloadBitmap(
                    imagePath,
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels
                )
            }
        }
    }
    
    private fun loadApps() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Get all launcher apps first (most common) - on background thread
                val allApps = repository.getAllApps()
                
                // If we have apps, use them
                // Otherwise try leanback apps
                val apps = if (allApps.isNotEmpty()) {
                    allApps
                } else {
                    repository.getLeanbackApps()
                }
                
                // Filter out the launcher itself
                val filteredApps = apps.filter { 
                    it.packageName != "com.octopus.launcher" 
                }
                
                // Update on main thread
                withContext(Dispatchers.Main) {
                    _apps.value = filteredApps
                    
                    // Initialize popular apps with random apps if empty
                    val packageNames = filteredApps.map { it.packageName }
                    popularAppsRepository.initializeWithRandomApps(packageNames, 5)
                    
                    filterApps(_searchQuery.value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Try to load apps again as fallback
                try {
                    val allApps = repository.getAllApps()
                    val filteredApps = allApps.filter { 
                        it.packageName != "com.octopus.launcher" 
                    }
                    withContext(Dispatchers.Main) {
                        _apps.value = filteredApps
                        filterApps(_searchQuery.value)
                    }
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    withContext(Dispatchers.Main) {
                        _apps.value = emptyList()
                    }
                }
            }
        }
    }
    
    fun reloadApps() {
        loadApps()
    }
    
    private fun filterApps(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val filtered = if (query.isBlank()) {
                _apps.value
            } else {
                // Use parallel processing for filtering large lists
                _apps.value.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
                }
            }
            // Update on main thread
            withContext(Dispatchers.Main) {
                _filteredApps.value = filtered
            }
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(packageName)
        }
    }
    
    fun launchApp(packageName: String) {
        repository.launchApp(packageName)
    }
    
    fun addPopularApp(packageName: String) {
        viewModelScope.launch {
            popularAppsRepository.addPopularApp(packageName)
        }
    }
    
    fun removePopularApp(packageName: String) {
        viewModelScope.launch {
            popularAppsRepository.removePopularApp(packageName)
        }
    }
    
    fun movePopularApp(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            popularAppsRepository.moveApp(fromIndex, toIndex)
        }
    }
    
    fun refreshBackgroundColors() {
        _backgroundColors.value = Triple(
            settingsRepository.getBackgroundColor1(),
            settingsRepository.getBackgroundColor2(),
            settingsRepository.getBackgroundColor3()
        )
        val newImagePath = settingsRepository.getBackgroundImagePath()
        
        // Clear cache for old path if it changed
        if (_backgroundImagePath.value != null && _backgroundImagePath.value != newImagePath) {
            com.octopus.launcher.ui.components.BackgroundImageCache.clearCache(_backgroundImagePath.value)
        }
        
        _backgroundImagePath.value = newImagePath
        _backgroundBlurEnabled.value = settingsRepository.isBackgroundBlurEnabled()
        
        // Preload bitmap if image path is set
        if (newImagePath != null) {
            viewModelScope.launch(Dispatchers.Default) {
                val displayMetrics = getApplication<Application>().resources.displayMetrics
                com.octopus.launcher.ui.components.BackgroundImageCache.preloadBitmap(
                    newImagePath,
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels
                )
            }
        }
    }
}

