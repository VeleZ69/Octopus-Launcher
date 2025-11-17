package com.octopus.launcher.ui.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.octopus.launcher.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    
    private val _color1 = MutableStateFlow(repository.getBackgroundColor1())
    val color1: StateFlow<Color> = _color1.asStateFlow()
    
    private val _color2 = MutableStateFlow(repository.getBackgroundColor2())
    val color2: StateFlow<Color> = _color2.asStateFlow()
    
    private val _color3 = MutableStateFlow(repository.getBackgroundColor3())
    val color3: StateFlow<Color> = _color3.asStateFlow()
    
    private val _backgroundImagePath = MutableStateFlow<String?>(repository.getBackgroundImagePath())
    val backgroundImagePath: StateFlow<String?> = _backgroundImagePath.asStateFlow()
    
    private val _backgroundBlurEnabled = MutableStateFlow(repository.isBackgroundBlurEnabled())
    val backgroundBlurEnabled: StateFlow<Boolean> = _backgroundBlurEnabled.asStateFlow()
    
    fun setColor1(color: Color) {
        _color1.value = color
        repository.setBackgroundColor1(color)
    }
    
    fun setColor2(color: Color) {
        _color2.value = color
        repository.setBackgroundColor2(color)
    }
    
    fun setColor3(color: Color) {
        _color3.value = color
        repository.setBackgroundColor3(color)
    }
    
    fun resetToDefaults() {
        repository.resetToDefaults()
        _color1.value = repository.getBackgroundColor1()
        _color2.value = repository.getBackgroundColor2()
        _color3.value = repository.getBackgroundColor3()
    }
    
    fun getBackgroundColors(): Triple<Color, Color, Color> {
        return Triple(_color1.value, _color2.value, _color3.value)
    }
    
    fun setBackgroundImagePath(path: String?) {
        // Clear cache for old path
        if (_backgroundImagePath.value != null && _backgroundImagePath.value != path) {
            com.octopus.launcher.ui.components.BackgroundImageCache.clearCache(_backgroundImagePath.value)
        }
        
        _backgroundImagePath.value = path
        repository.setBackgroundImagePath(path)
        
        // Preload new bitmap if path is set
        if (path != null) {
            viewModelScope.launch(Dispatchers.Default) {
                val displayMetrics = getApplication<Application>().resources.displayMetrics
                com.octopus.launcher.ui.components.BackgroundImageCache.preloadBitmap(
                    path,
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels
                )
            }
        }
    }
    
    fun clearBackgroundImage() {
        // Clear cache for old path
        if (_backgroundImagePath.value != null) {
            com.octopus.launcher.ui.components.BackgroundImageCache.clearCache(_backgroundImagePath.value)
        }
        
        _backgroundImagePath.value = null
        repository.clearBackgroundImage()
    }
    
    fun setBackgroundBlurEnabled(enabled: Boolean) {
        _backgroundBlurEnabled.value = enabled
        repository.setBackgroundBlurEnabled(enabled)
    }
}

