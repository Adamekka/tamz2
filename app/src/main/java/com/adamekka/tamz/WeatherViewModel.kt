package com.adamekka.tamz

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException
import java.time.DateTimeException

internal sealed interface WeatherState {
    data object Empty : WeatherState
    data object Loading : WeatherState
    data class Ready(val report: WeatherReport) : WeatherState
    data class Failed(val message: String, val detail: String) : WeatherState
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("weather", Context.MODE_PRIVATE)
    private val api = WeatherApi(BuildConfig.OPENWEATHER_API_KEY)
    var city by mutableStateOf(preferences.getString("city", "").orEmpty())
    internal var state: WeatherState by mutableStateOf(WeatherState.Empty)
        private set

    init {
        if (city.isNotBlank()) refresh()
    }

    fun refresh() {
        val query = city.trim()
        require(query.isNotEmpty()) { "A city is required to request weather." }
        check(state != WeatherState.Loading) { "A weather request is already running." }
        state = WeatherState.Loading
        viewModelScope.launch {
            try {
                val report = withContext(Dispatchers.IO) { api.load(query) }
                preferences.edit().putString("city", query).apply()
                state = WeatherState.Ready(report)
            } catch (error: WeatherRequestException) {
                state = WeatherState.Failed(error.userMessage, error.toString())
            } catch (error: IOException) {
                state = WeatherState.Failed("Nelze se připojit. Zkontrolujte internet a zkuste to znovu.",
                    error.toString().replace(BuildConfig.OPENWEATHER_API_KEY, "[redacted]"))
            } catch (error: JSONException) {
                state = WeatherState.Failed("Služba vrátila neplatná data. Zkuste to znovu později.", error.toString())
            } catch (error: DateTimeException) {
                state = WeatherState.Failed("Služba vrátila neplatné datum. Zkuste to znovu později.", error.toString())
            }
        }
    }
}
