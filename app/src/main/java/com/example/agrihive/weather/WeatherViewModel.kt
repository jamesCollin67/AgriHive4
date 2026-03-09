package com.example.agrihive.weather

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Weather ViewModel for managing weather data in the app
 */
class WeatherViewModel : ViewModel() {

    private val weatherService = WeatherService()

    private val _currentWeather = MutableLiveData<WeatherData?>()
    val currentWeather: LiveData<WeatherData?> = _currentWeather

    private val _weatherForecast = MutableLiveData<List<WeatherForecast>>()
    val weatherForecast: LiveData<List<WeatherForecast>> = _weatherForecast

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _rainfallWarning = MutableLiveData<RainfallWarning?>()
    val rainfallWarning: LiveData<RainfallWarning?> = _rainfallWarning

    private val _beekeepingAdvice = MutableLiveData<String>()
    val beekeepingAdvice: LiveData<String> = _beekeepingAdvice

    // Track previous warning to avoid duplicate notifications
    private var previousWarning: RainfallWarning = RainfallWarning.NONE

    /**
     * Fetch current weather by coordinates
     */
    fun fetchCurrentWeather(latitude: Double, longitude: Double, context: Context? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val weather = weatherService.getCurrentWeather(latitude, longitude)
                _currentWeather.value = weather
                
                weather?.let {
                    _rainfallWarning.value = it.rainfallWarning
                    _beekeepingAdvice.value = weatherService.getBeekeepingAdvice(it)
                    
                    // Send notification if rain is expected and warning level increased
                    context?.let { ctx ->
                        if (it.rainfallWarning.level > RainfallWarning.NONE.level && 
                            it.rainfallWarning.level > previousWarning.level) {
                            RainAlertNotification.showRainNotification(ctx, it.rainfallWarning)
                        }
                        previousWarning = it.rainfallWarning
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to fetch weather: ${e.message}"
                _currentWeather.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Fetch current weather by city name
     */
    fun fetchWeatherByCity(cityName: String, context: Context? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val weather = weatherService.getWeatherByCity(cityName)
                _currentWeather.value = weather
                
                weather?.let {
                    _rainfallWarning.value = it.rainfallWarning
                    _beekeepingAdvice.value = weatherService.getBeekeepingAdvice(it)
                    
                    // Send notification if rain is expected
                    context?.let { ctx ->
                        if (it.rainfallWarning.level > RainfallWarning.NONE.level) {
                            RainAlertNotification.showRainNotification(ctx, it.rainfallWarning)
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to fetch weather: ${e.message}"
                _currentWeather.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Fetch weather forecast and check for rain
     */
    fun fetchForecast(latitude: Double, longitude: Double, context: Context? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val forecast = weatherService.getForecast(latitude, longitude)
                _weatherForecast.value = forecast
                
                // Check forecast for rain and send notifications
                context?.let { ctx ->
                    for ((index, day) in forecast.withIndex()) {
                        if (index == 0) continue // Skip today
                        if (day.rainfallProbability >= 50) {
                            RainAlertNotification.showRainForecastNotification(
                                ctx, 
                                day.rainfallProbability, 
                                day.date
                            )
                            break // Only notify for first day with significant rain
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to fetch forecast: ${e.message}"
                _weatherForecast.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if there's a rainfall warning
     */
    fun hasRainfallWarning(): Boolean {
        val warning = _rainfallWarning.value
        return warning != null && warning.level > 0
    }

    /**
     * Get the current rainfall warning level
     */
    fun getCurrentWarningLevel(): RainfallWarning {
        return _rainfallWarning.value ?: RainfallWarning.NONE
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        // Default location - Cebu, Philippines
        const val DEFAULT_CITY = "Cebu City, PH"
        
        // Rainfall thresholds for notifications
        const val NOTIFICATION_THRESHOLD = 2 // Start notifying from moderate rain
    }
}
