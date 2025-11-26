package com.octopus.launcher.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Wallpaper(
    val url: String,
    val title: String,
    val copyright: String,
    val fallbackUrl: String? = null
)

class ScreensaverViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _wallpaper = MutableStateFlow<Wallpaper?>(null)
    val wallpaper: StateFlow<Wallpaper?> = _wallpaper.asStateFlow()

    private val wallpaperStoreImages = listOf(
        Wallpaper(
            url = "https://wallpaper-store.netlify.app/images/wallpaper1.jpg",
            title = "Mountain Sunrise",
            copyright = "Wallpaper Store • UHD"
        ),
        Wallpaper(
            url = "https://wallpaper-store.netlify.app/images/wallpaper2.jpg",
            title = "Ocean Waves", 
            copyright = "Wallpaper Store • UHD"
        ),
        Wallpaper(
            url = "https://wallpaper-store.netlify.app/images/wallpaper3.jpg",
            title = "Forest Path",
            copyright = "Wallpaper Store • UHD"
        ),
        Wallpaper(
            url = "https://wallpaper-store.netlify.app/images/wallpaper5.jpg",
            title = "Northern Lights",
            copyright = "Wallpaper Store • UHD"
        ),
        Wallpaper(
            url = "https://wallpaper-store.netlify.app/images/wallpaper6.jpg",
            title = "Desert Dunes",
            copyright = "Wallpaper Store • UHD"
        ),
        Wallpaper(
            url = "https://wallpaper-store.netlify.app/images/wallpaper7.jpg",
            title = "Lake Reflection",
            copyright = "Wallpaper Store • UHD"
        ),
        Wallpaper(
            url = "https://wallpaper-store.netlify.app/images/wallpaper8.jpg",
            title = "Snowy Peaks",
            copyright = "Wallpaper Store • UHD"
        ),
        Wallpaper(
            url = "https://wallpaper-store.netlify.app/images/wallpaper10.jpg",
            title = "Star Galaxy",
            copyright = "Wallpaper Store • UHD"
        )
    )

    fun loadWallpaperStoreImage() {
        viewModelScope.launch {
            val randomImage = wallpaperStoreImages.random()
            _wallpaper.value = randomImage
            android.util.Log.d("ScreensaverViewModel", "Loaded Wallpaper Store image: ${randomImage.title}")
        }
    }

    // Для совместимости
    fun loadLocalWallpaper(url: String, titlePair: Pair<String, String>) {
        viewModelScope.launch {
            _wallpaper.value = Wallpaper(
                url = url,
                title = titlePair.first,
                copyright = titlePair.second
            )
        }
    }

    fun loadWallpaper() {
        loadWallpaperStoreImage()
    }
}