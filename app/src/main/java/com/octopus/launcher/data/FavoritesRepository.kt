package com.octopus.launcher.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class FavoritesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "octopus_launcher_prefs",
        Context.MODE_PRIVATE
    )
    
    private val _favorites = MutableStateFlow<Set<String>>(loadFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()
    
    private fun loadFavorites(): Set<String> {
        return prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    }
    
    suspend fun addFavorite(packageName: String) = withContext(Dispatchers.IO) {
        val current = _favorites.value.toMutableSet()
        current.add(packageName)
        saveFavorites(current)
        _favorites.value = current
    }
    
    suspend fun removeFavorite(packageName: String) = withContext(Dispatchers.IO) {
        val current = _favorites.value.toMutableSet()
        current.remove(packageName)
        saveFavorites(current)
        _favorites.value = current
    }
    
    suspend fun toggleFavorite(packageName: String) = withContext(Dispatchers.IO) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        saveFavorites(current)
        _favorites.value = current
    }
    
    private fun saveFavorites(favorites: Set<String>) {
        prefs.edit()
            .putStringSet("favorites", favorites)
            .apply()
    }
}

