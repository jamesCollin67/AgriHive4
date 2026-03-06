package com.example.agrihive.weather

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Weather data model
 */
data class WeatherData(
    val location: String,
    val temperature: Double,
    val temperatureUnit: String = "°C",
    val humidity: Int,
    val description: String,
    val iconCode: String,
    val rainfall: Double, // mm/h
    val rainfallWarning: RainfallWarning,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Rainfall warning levels for beekeepers
 */
enum class RainfallWarning(val level: Int, val displayName: String, val message: String) {
    NONE(0, "No Warning", "No rainfall expected. Bees can forage normally."),
    LOW(1, "Light Rain Expected", "Light rain may occur. Ensure hive entrances are clear."),
    MODERATE(2, "Moderate Rain Warning", "Moderate rain expected. Consider providing supplemental feeding."),
    HIGH(3, "Heavy Rain Alert", "Heavy rain expected! Prepare emergency feeding for your bees."),
    SEVERE(4, "Severe Weather Alert", "Severe weather conditions! Take immediate action to protect your hives.")
}

/**
 * Weather forecast for upcoming days
 */
data class WeatherForecast(
    val date: String,
    val temperature: Double,
    val rainfallProbability: Int, // percentage
    val rainfallWarning: RainfallWarning,
    val description: String
)

/**
 * Weather service for monitoring rainfall and providing warnings for beekeepers
 * Uses OpenWeatherMap API for weather data
 */
class WeatherService {

    companion object {
        // OpenWeatherMap API base URL
        const val WEATHER_API_BASE = "https://api.openweathermap.org/data/2.5"
        
        // Replace with your actual OpenWeatherMap API key
        // Get your free API key at: https://openweathermap.org/api
        const val API_KEY = "YOUR_OPENWEATHERMAP_API_KEY"
        
        // Rainfall thresholds (mm/h) for warning levels
        const val RAINFALL_THRESHOLD_LOW = 0.5
        const val RAINFALL_THRESHOLD_MODERATE = 2.5
        const val RAINFALL_THRESHOLD_HIGH = 7.5
        const val RAINFALL_THRESHOLD_SEVERE = 15.0
        
        // Rain probability thresholds for warnings
        const val RAIN_PROBABILITY_LOW = 30
        const val RAIN_PROBABILITY_MODERATE = 50
        const val RAIN_PROBABILITY_HIGH = 70
    }

    /**
     * Get current weather data for a location
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @return WeatherData object or null if request fails
     */
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$WEATHER_API_BASE/weather?lat=$latitude&lon=$longitude&appid=$API_KEY&units=metric")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    parseWeatherResponse(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Get 5-day weather forecast for a location
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @return List of WeatherForecast or empty list if request fails
     */
    suspend fun getForecast(latitude: Double, longitude: Double): List<WeatherForecast> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$WEATHER_API_BASE/forecast?lat=$latitude&lon=$longitude&appid=$API_KEY&units=metric")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    parseForecastResponse(response)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Get weather data by city name
     * @param cityName City name (e.g., "Manila, PH")
     * @return WeatherData object or null if request fails
     */
    suspend fun getWeatherByCity(cityName: String): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedCity = java.net.URLEncoder.encode(cityName, "UTF-8")
                val url = URL("$WEATHER_API_BASE/weather?q=$encodedCity&appid=$API_KEY&units=metric")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    parseWeatherResponse(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Parse weather API response
     */
    private fun parseWeatherResponse(response: String): WeatherData? {
        return try {
            val json = JSONObject(response)
            val main = json.getJSONObject("main")
            val weather = json.getJSONArray("weather").getJSONObject(0)
            
            // Get rainfall data if available
            var rainfall = 0.0
            if (json.has("rain")) {
                val rain = json.getJSONObject("rain")
                rainfall = if (rain.has("1h")) rain.getDouble("1h") else 0.0
            }
            
            // Get location name
            val location = if (json.has("name")) json.getString("name") else "Unknown"
            
            val temperature = main.getDouble("temp")
            val humidity = main.getInt("humidity")
            val description = weather.getString("description")
            val iconCode = weather.getString("icon")
            
            // Determine rainfall warning based on rainfall amount
            val warning = getRainfallWarning(rainfall)
            
            WeatherData(
                location = location,
                temperature = temperature,
                humidity = humidity,
                description = description,
                iconCode = iconCode,
                rainfall = rainfall,
                rainfallWarning = warning
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parse forecast API response
     */
    private fun parseForecastResponse(response: String): List<WeatherForecast> {
        val forecasts = mutableListOf<WeatherForecast>()
        
        try {
            val json = JSONObject(response)
            val list = json.getJSONArray("list")
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            
            // Group by date and get one forecast per day
            val dailyForecasts = mutableMapOf<String, MutableList<JSONObject>>()
            
            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val dt = item.getLong("dt") * 1000
                val date = dateFormat.format(Date(dt))
                
                if (!dailyForecasts.containsKey(date)) {
                    dailyForecasts[date] = mutableListOf()
                }
                dailyForecasts[date]?.add(item)
            }
            
            // Process each day (limit to 5 days)
            var dayCount = 0
            for ((date, items) in dailyForecasts) {
                if (dayCount >= 5) break
                
                // Get the forecast with highest rain probability for the day
                val maxRainProbItem = items.maxByOrNull { 
                    val main = it.optJSONObject("main")
                    val pop = it.optDouble("pop", 0.0)
                    pop
                } ?: continue
                
                val main = maxRainProbItem.getJSONObject("main")
                val weather = maxRainProbItem.getJSONArray("weather").getJSONObject(0)
                val pop = maxRainProbItem.optDouble("pop", 0.0) * 100 // Convert to percentage
                
                val temperature = main.getDouble("temp")
                val description = weather.getString("description")
                
                // Get rain probability for warning
                val rainProbability = pop.toInt()
                val warning = getRainfallWarningFromProbability(rainProbability)
                
                val parsedDate = dateFormat.parse(date)
                val displayDate = parsedDate?.let { outputFormat.format(it) } ?: date
                
                forecasts.add(
                    WeatherForecast(
                        date = displayDate,
                        temperature = temperature,
                        rainfallProbability = rainProbability,
                        rainfallWarning = warning,
                        description = description
                    )
                )
                
                dayCount++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return forecasts
    }

    /**
     * Determine rainfall warning based on rainfall amount
     */
    fun getRainfallWarning(rainfall: Double): RainfallWarning {
        return when {
            rainfall >= RAINFALL_THRESHOLD_SEVERE -> RainfallWarning.SEVERE
            rainfall >= RAINFALL_THRESHOLD_HIGH -> RainfallWarning.HIGH
            rainfall >= RAINFALL_THRESHOLD_MODERATE -> RainfallWarning.MODERATE
            rainfall >= RAINFALL_THRESHOLD_LOW -> RainfallWarning.LOW
            else -> RainfallWarning.NONE
        }
    }

    /**
     * Determine rainfall warning based on rain probability
     */
    fun getRainfallWarningFromProbability(probability: Int): RainfallWarning {
        return when {
            probability >= RAIN_PROBABILITY_HIGH -> RainfallWarning.HIGH
            probability >= RAIN_PROBABILITY_MODERATE -> RainfallWarning.MODERATE
            probability >= RAIN_PROBABILITY_LOW -> RainfallWarning.LOW
            else -> RainfallWarning.NONE
        }
    }

    /**
     * Get bee-keeping advice based on weather conditions
     */
    fun getBeekeepingAdvice(weatherData: WeatherData): String {
        val warning = weatherData.rainfallWarning
        val baseAdvice = when (warning) {
            RainfallWarning.NONE -> "Weather conditions are ideal for bee activity. "
            RainfallWarning.LOW -> "Light rain may reduce foraging. "
            RainfallWarning.MODERATE -> "Rain may prevent bees from foraging. "
            RainfallWarning.HIGH -> "Heavy rain expected - bees will stay in hive. "
            RainfallWarning.SEVERE -> "Severe weather conditions! "
        }
        
        val feedingAdvice = when (warning) {
            RainfallWarning.NONE -> "No supplemental feeding needed."
            RainfallWarning.LOW -> "Consider checking food stores."
            RainfallWarning.MODERATE -> "Consider syrup or fondant."
            RainfallWarning.HIGH -> "Provide emergency feeding immediately."
            RainfallWarning.SEVERE -> "Take immediate action to protect hives and provide food."
        }
        
        val temperatureAdvice = when {
            weatherData.temperature < 10 -> "Temperature is low - ensure hives are properly insulated."
            weatherData.temperature > 35 -> "High temperature - ensure adequate ventilation and water source."
            else -> ""
        }
        
        return "$baseAdvice$feedingAdvice $temperatureAdvice"
    }
}
