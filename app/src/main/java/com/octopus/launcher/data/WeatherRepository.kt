package com.octopus.launcher.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.net.URL
import org.json.JSONObject
import android.location.LocationListener
import android.content.SharedPreferences
import kotlin.coroutines.resume

data class WeatherInfo(
    val temperature: Int,
    val condition: String,
    val city: String
)

class WeatherRepository(private val context: Context) {
    // Using Open-Meteo API - completely free, no API key required
    // https://open-meteo.com/
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "octopus_launcher_weather",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_WEATHER_TEMP = "weather_temp"
        private const val KEY_WEATHER_CONDITION = "weather_condition"
        private const val KEY_WEATHER_CITY = "weather_city"
        private const val KEY_WEATHER_TIMESTAMP = "weather_timestamp"
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    fun getCachedWeather(): WeatherInfo? {
        val timestamp = prefs.getLong(KEY_WEATHER_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        
        // Return cached weather if it's still valid (less than 5 minutes old)
        if (currentTime - timestamp < CACHE_DURATION_MS) {
            val temp = prefs.getInt(KEY_WEATHER_TEMP, Int.MIN_VALUE)
            val condition = prefs.getString(KEY_WEATHER_CONDITION, null)
            val city = prefs.getString(KEY_WEATHER_CITY, null)
            
            if (temp != Int.MIN_VALUE && condition != null && city != null) {
                android.util.Log.d("WeatherRepository", "Using cached weather: $temp°, $condition, $city")
                return WeatherInfo(temp, condition, city)
            }
        }
        return null
    }
    
    private fun cacheWeather(weather: WeatherInfo) {
        prefs.edit()
            .putInt(KEY_WEATHER_TEMP, weather.temperature)
            .putString(KEY_WEATHER_CONDITION, weather.condition)
            .putString(KEY_WEATHER_CITY, weather.city)
            .putLong(KEY_WEATHER_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }
    
    suspend fun getWeatherInfo(forceRefresh: Boolean = false): WeatherInfo = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("WeatherRepository", "getWeatherInfo: Starting weather sync (forceRefresh=$forceRefresh)")
        
        // Try to get cached weather first (unless forcing refresh)
        if (!forceRefresh) {
            val cachedWeather = getCachedWeather()
            if (cachedWeather != null) {
                android.util.Log.d("WeatherRepository", "getWeatherInfo: Using cached weather (${System.currentTimeMillis() - startTime}ms)")
                return@withContext cachedWeather
            }
        } else {
            android.util.Log.d("WeatherRepository", "getWeatherInfo: Force refresh requested, skipping cache")
        }
        
        try {
            val locationStartTime = System.currentTimeMillis()
            android.util.Log.d("WeatherRepository", "getWeatherInfo: Getting location...")
            val location = getCurrentLocation()
            android.util.Log.d("WeatherRepository", "getWeatherInfo: Location obtained in ${System.currentTimeMillis() - locationStartTime}ms: ${location?.latitude}, ${location?.longitude}")
            
            val cityStartTime = System.currentTimeMillis()
            val city = if (location != null) {
                getCityFromLocation(location)
            } else {
                "Неизвестно"
            }
            android.util.Log.d("WeatherRepository", "getWeatherInfo: City obtained in ${System.currentTimeMillis() - cityStartTime}ms: $city")
            
            // Try to get real weather data from Open-Meteo API
            val weatherInfo = if (location != null) {
                val apiStartTime = System.currentTimeMillis()
                android.util.Log.d("WeatherRepository", "getWeatherInfo: Fetching weather from API...")
                val info = getWeatherFromAPI(location.latitude, location.longitude, city)
                android.util.Log.d("WeatherRepository", "getWeatherInfo: API call completed in ${System.currentTimeMillis() - apiStartTime}ms")
                info
            } else {
                android.util.Log.w("WeatherRepository", "getWeatherInfo: No location available, skipping API call")
                null
            }
            
            // Return real data if available, otherwise use cached or fallback
            val result = weatherInfo ?: (getCachedWeather() ?: WeatherInfo(
                temperature = 22,
                condition = "Солнечно",
                city = city
            ))
            
            // Cache the result if we got real data
            if (weatherInfo != null) {
                cacheWeather(weatherInfo)
            }
            
            android.util.Log.d("WeatherRepository", "getWeatherInfo: Total sync time: ${System.currentTimeMillis() - startTime}ms")
            result
        } catch (e: Exception) {
            android.util.Log.e("WeatherRepository", "getWeatherInfo: Error after ${System.currentTimeMillis() - startTime}ms", e)
            e.printStackTrace()
            // Try to use cached weather on error
            val cached = getCachedWeather()
            if (cached != null) {
                android.util.Log.d("WeatherRepository", "getWeatherInfo: Using cached weather after error")
                return@withContext cached
            }
            
            // Last resort - fallback
            val location = getCurrentLocation()
            val city = if (location != null) {
                getCityFromLocation(location)
            } else {
                "Неизвестно"
            }
            WeatherInfo(
                temperature = 22,
                condition = "Солнечно",
                city = city
            )
        }
    }
    
    private suspend fun getWeatherFromAPI(latitude: Double, longitude: Double, city: String): WeatherInfo? {
        return try {
            val apiStartTime = System.currentTimeMillis()
            // Using Open-Meteo API - free, no API key needed
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code&timezone=auto"
            android.util.Log.d("WeatherRepository", "getWeatherFromAPI: Requesting from $urlString")
            
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "OctopusLauncher/1.0")
            
            val connectStartTime = System.currentTimeMillis()
            connection.connect()
            android.util.Log.d("WeatherRepository", "getWeatherFromAPI: Connection established in ${System.currentTimeMillis() - connectStartTime}ms")
            
            val readStartTime = System.currentTimeMillis()
            val response = connection.getInputStream().bufferedReader().use { it.readText() }
            android.util.Log.d("WeatherRepository", "getWeatherFromAPI: Response read in ${System.currentTimeMillis() - readStartTime}ms, size: ${response.length} bytes")
            
            val parseStartTime = System.currentTimeMillis()
            // Parse Open-Meteo JSON response
            val json = JSONObject(response)
            val current = json.getJSONObject("current")
            
            // Get temperature in Celsius
            val tempC = current.getDouble("temperature_2m").toInt()
            
            // Get weather code and convert to Russian description
            val weatherCode = current.getInt("weather_code")
            val condition = getWeatherConditionFromCode(weatherCode)
            android.util.Log.d("WeatherRepository", "getWeatherFromAPI: Parsed in ${System.currentTimeMillis() - parseStartTime}ms")
            
            android.util.Log.d("WeatherRepository", "getWeatherFromAPI: Total API call time: ${System.currentTimeMillis() - apiStartTime}ms, temp=$tempC, condition=$condition, city=$city")
            
            WeatherInfo(
                temperature = tempC,
                condition = condition,
                city = city
            )
        } catch (e: Exception) {
            android.util.Log.e("WeatherRepository", "getWeatherFromAPI: Error fetching weather from Open-Meteo API", e)
            null
        }
    }
    
    // Convert WMO weather code to Russian description
    private fun getWeatherConditionFromCode(code: Int): String {
        return when (code) {
            0 -> "Ясно"
            1 -> "Преимущественно ясно"
            2 -> "Переменная облачность"
            3 -> "Пасмурно"
            45 -> "Туман"
            48 -> "Иней"
            51, 53, 55 -> "Моросящий дождь"
            56, 57 -> "Ледяной дождь"
            61, 63, 65 -> "Дождь"
            66, 67 -> "Ледяной дождь"
            71, 73, 75 -> "Снег"
            77 -> "Снежные зерна"
            80, 81, 82 -> "Ливень"
            85, 86 -> "Снегопад"
            95 -> "Гроза"
            96, 99 -> "Гроза с градом"
            else -> "Неизвестно"
        }
    }
    
    private suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        try {
            // Check permissions
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                android.util.Log.w("WeatherRepository", "Location permissions not granted")
                return@withContext null
            }
            
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext null
            
            // Try all providers and get the most recent valid location
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            
            var bestLocation: Location? = null
            var bestTime = 0L
            
            for (provider in providers) {
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        val location = locationManager.getLastKnownLocation(provider)
                        if (location != null && isLocationValid(location)) {
                            if (location.time > bestTime) {
                                bestLocation = location
                                bestTime = location.time
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    android.util.Log.w("WeatherRepository", "Security exception for provider: $provider", e)
                } catch (e: Exception) {
                    android.util.Log.w("WeatherRepository", "Error getting location from provider: $provider", e)
                }
            }
            
            // If we have a location, return it
            if (bestLocation != null) {
                android.util.Log.d("WeatherRepository", "Found location: ${bestLocation.latitude}, ${bestLocation.longitude}")
                return@withContext bestLocation
            }
            
            // If no valid location found, try to request a single update
            android.util.Log.d("WeatherRepository", "No cached location found, requesting fresh location")
            return@withContext requestFreshLocation(locationManager)
        } catch (e: SecurityException) {
            android.util.Log.e("WeatherRepository", "Security exception getting location", e)
        } catch (e: Exception) {
            android.util.Log.e("WeatherRepository", "Error getting location", e)
        }
        return@withContext null
    }
    
    private suspend fun requestFreshLocation(locationManager: LocationManager): Location? = suspendCancellableCoroutine { continuation ->
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                android.util.Log.d("WeatherRepository", "Received fresh location: ${location.latitude}, ${location.longitude}")
                locationManager.removeUpdates(this)
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
            
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var timeoutRunnable: Runnable? = null
        
        continuation.invokeOnCancellation {
            try {
                locationManager.removeUpdates(locationListener)
                // Cancel timeout handler to prevent memory leak
                timeoutRunnable?.let { handler.removeCallbacks(it) }
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        try {
            // Try NETWORK_PROVIDER first (faster for TV devices)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(
                    LocationManager.NETWORK_PROVIDER,
                    locationListener,
                    android.os.Looper.getMainLooper()
                )
                // Set timeout - if no location received in 10 seconds, return null
                timeoutRunnable = Runnable {
                    if (continuation.isActive) {
                        android.util.Log.w("WeatherRepository", "Location request timeout")
                        try {
                            locationManager.removeUpdates(locationListener)
                        } catch (e: Exception) {
                            // Ignore
                        }
                        continuation.resume(null)
                    }
                }
                handler.postDelayed(timeoutRunnable!!, 10000)
            } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    locationListener,
                    android.os.Looper.getMainLooper()
                )
                timeoutRunnable = Runnable {
                    if (continuation.isActive) {
                        android.util.Log.w("WeatherRepository", "Location request timeout")
                        try {
                            locationManager.removeUpdates(locationListener)
                        } catch (e: Exception) {
                            // Ignore
                        }
                        continuation.resume(null)
                    }
                }
                handler.postDelayed(timeoutRunnable!!, 15000) // GPS may take longer
            } else {
                android.util.Log.w("WeatherRepository", "No location providers enabled")
                continuation.resume(null)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("WeatherRepository", "Security exception requesting location", e)
            timeoutRunnable?.let { handler.removeCallbacks(it) }
            continuation.resume(null)
        } catch (e: Exception) {
            android.util.Log.e("WeatherRepository", "Error requesting location", e)
            timeoutRunnable?.let { handler.removeCallbacks(it) }
            continuation.resume(null)
        }
    }
    
    private fun isLocationValid(location: Location): Boolean {
        // Location is valid if it's less than 24 hours old (more lenient for TV devices)
        val maxAge = 24 * 3600000L // 24 hours
        return System.currentTimeMillis() - location.time < maxAge
    }
    
    private fun getCityFromLocation(location: Location): String {
        return try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.forLanguageTag("ru-RU"))
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    // Use new API for Android 13+ (async callback)
                    // Note: This is a simplified version - in production you'd want to use suspendCoroutine
                    // For now, we'll use the deprecated method with suppression
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.adminArea ?: "Неизвестно"
                } else {
                    // Use old API for older Android versions
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.adminArea ?: "Неизвестно"
                }
            } else {
                "Неизвестно"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Неизвестно"
        }
    }
}

