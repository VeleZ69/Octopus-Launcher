package com.octopus.launcher.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class PopularAppsRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "octopus_launcher_prefs",
        Context.MODE_PRIVATE
    )
    
    private val _popularApps = MutableStateFlow<List<String>>(loadPopularApps())
    val popularApps: StateFlow<List<String>> = _popularApps.asStateFlow()
    
    // For backward compatibility, also provide as Set
    private val _popularAppsSet = MutableStateFlow<Set<String>>(_popularApps.value.toSet())
    val popularAppsSet: StateFlow<Set<String>> = _popularAppsSet.asStateFlow()
    
    private fun loadPopularApps(): List<String> {
        // Try to load as List first (new format)
        val listJson = prefs.getString("popular_apps_list", null)
        if (listJson != null) {
            // Simple JSON-like format: "app1,app2,app3"
            return if (listJson.isEmpty()) emptyList() else listJson.split(",")
        }
        
        // Fallback to Set format (old format)
        val set = prefs.getStringSet("popular_apps", emptySet()) ?: emptySet()
        return set.toList()
    }
    
    private fun updatePopularApps(newList: List<String>) {
        _popularApps.value = newList
        _popularAppsSet.value = newList.toSet()
    }
    
    suspend fun addPopularApp(packageName: String) = withContext(Dispatchers.IO) {
        val current = _popularApps.value.toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            savePopularApps(current)
            updatePopularApps(current)
            android.util.Log.d("PopularAppsRepository", "App added: $packageName. Total: ${current.size}")
        } else {
            android.util.Log.d("PopularAppsRepository", "App already exists: $packageName")
        }
    }
    
    suspend fun removePopularApp(packageName: String) = withContext(Dispatchers.IO) {
        val current = _popularApps.value.toMutableList()
        android.util.Log.d("PopularAppsRepository", "Removing app: $packageName. Current list size: ${current.size}")
        val removed = current.remove(packageName)
        android.util.Log.d("PopularAppsRepository", "App removed: $removed. New list size: ${current.size}")
        savePopularApps(current)
        updatePopularApps(current)
        android.util.Log.d("PopularAppsRepository", "App removal completed. Updated list: ${current.joinToString()}")
    }
    
    suspend fun moveApp(fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        val current = _popularApps.value.toMutableList()
        android.util.Log.d("PopularAppsRepository", "Moving app from index $fromIndex to $toIndex. Current list: ${current.joinToString()}")
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            android.util.Log.d("PopularAppsRepository", "App moved: $item. New list: ${current.joinToString()}")
            savePopularApps(current)
            updatePopularApps(current)
            android.util.Log.d("PopularAppsRepository", "App move completed")
        } else {
            android.util.Log.e("PopularAppsRepository", "Invalid indices: fromIndex=$fromIndex, toIndex=$toIndex, list size=${current.size}")
        }
    }
    
    suspend fun initializeWithRandomApps(allApps: List<String>, count: Int = 5) = withContext(Dispatchers.IO) {
        if (_popularApps.value.isEmpty() && allApps.isNotEmpty()) {
            val randomApps = allApps.shuffled().take(count)
            savePopularApps(randomApps)
            updatePopularApps(randomApps)
            android.util.Log.d("PopularAppsRepository", "Initialized with ${randomApps.size} random apps")
        }
    }
    
    private fun savePopularApps(apps: List<String>) {
        prefs.edit()
            .putString("popular_apps_list", apps.joinToString(","))
            .apply()
    }
}

