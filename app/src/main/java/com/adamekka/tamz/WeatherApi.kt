package com.adamekka.tamz

import androidx.annotation.DrawableRes
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneOffset

internal data class WeatherReading(val time: Instant, val temperature: Double, val description: String, @DrawableRes val icon: Int)
internal data class WeatherReport(val city: String, val offset: ZoneOffset, val current: WeatherReading, val forecast: List<WeatherReading>)
internal class WeatherRequestException(val userMessage: String, diagnostic: String) : IOException(diagnostic)

internal class WeatherApi(private val apiKey: String) {
    init {
        require(apiKey.isNotBlank()) { "OPENWEATHER_API_KEY is required." }
    }

    fun load(city: String): WeatherReport {
        require(city.isNotBlank())

        fun request(endpoint: String): JSONObject {
            val connection = URL("https://api.openweathermap.org/data/2.5/$endpoint&units=metric&lang=cz&appid=$apiKey")
                .openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                // Do not forward the API key if the service redirects to another host.
                connection.instanceFollowRedirects = false
                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    val message = when (status) {
                        401, 403 -> "Přístup k počasí není dostupný. Ověřte platnost a aktivaci API klíče."
                        404 -> "Město nebylo nalezeno. Zkontrolujte název."
                        429 -> "Příliš mnoho požadavků. Zkuste to později."
                        else -> "Počasí se nepodařilo načíst. Zkuste to později."
                    }
                    throw WeatherRequestException(message, "OpenWeatherMap: HTTP $status")
                }
                return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            } finally {
                connection.disconnect()
            }
        }

        val current = request("weather?q=${URLEncoder.encode(city, "UTF-8")}")
        val coordinates = current.getJSONObject("coord")
        // Reuse the resolved coordinates so both screens always refer to the same city.
        val forecast = request("forecast?lat=${coordinates.getDouble("lat")}&lon=${coordinates.getDouble("lon")}")
        return parseWeatherReport(current, forecast)
    }
}

internal fun parseWeatherReport(current: JSONObject, forecast: JSONObject): WeatherReport {
    fun reading(json: JSONObject): WeatherReading {
        val weather = json.getJSONArray("weather").getJSONObject(0)
        val temperature = json.getJSONObject("main").getDouble("temp")
        val description = weather.getString("description")
        if (!temperature.isFinite() || description.isBlank()) throw JSONException("Invalid weather values")
        // These cloud, snow, rainstorm and mist images are identical for day and night.
        val icon = when (val code = weather.getString("icon")) {
            "01d" -> R.drawable.weather_01d
            "01n" -> R.drawable.weather_01n
            "02d" -> R.drawable.weather_02d
            "02n" -> R.drawable.weather_02n
            "03d", "03n" -> R.drawable.weather_03d
            "04d", "04n" -> R.drawable.weather_04d
            "09d", "09n" -> R.drawable.weather_09d
            "10d" -> R.drawable.weather_10d
            "10n" -> R.drawable.weather_10n
            "11d", "11n" -> R.drawable.weather_11d
            "13d", "13n" -> R.drawable.weather_13d
            "50d", "50n" -> R.drawable.weather_50d
            else -> throw JSONException("Unknown weather icon: $code")
        }
        return WeatherReading(Instant.ofEpochSecond(json.getLong("dt")), temperature, description, icon)
    }

    val entries = forecast.getJSONArray("list")
    if (entries.length() == 0) throw JSONException("Empty forecast")
    val city = current.getString("name")
    if (city.isBlank()) throw JSONException("Empty city name")
    return WeatherReport(
        "$city, ${current.getJSONObject("sys").getString("country")}",
        ZoneOffset.ofTotalSeconds(forecast.getJSONObject("city").getInt("timezone")),
        reading(current),
        List(entries.length()) { reading(entries.getJSONObject(it)) }
    )
}
