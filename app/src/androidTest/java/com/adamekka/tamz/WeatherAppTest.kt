package com.adamekka.tamz

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.adamekka.tamz.ui.theme.TamzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class WeatherAppTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyCityDisablesRefreshAndTypingEnablesIt() {
        val city = mutableStateOf("")
        var requests = 0
        compose.setContent { TamzTheme { WeatherApp(city.value, { city.value = it }, WeatherState.Empty, { requests++ }) } }
        compose.onNodeWithText("Aktualizovat").assertIsNotEnabled()
        compose.onNodeWithText("Město").performTextInput("Ostrava")
        compose.onNodeWithText("Aktualizovat").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, requests) }
    }

    @Test
    fun showsWeatherAndForecastWithCityLocalDateAndTime() {
        val reading = WeatherReading(Instant.parse("2026-11-07T23:00:00Z"), 7.47, "zataženo", R.drawable.weather_04d)
        val report = WeatherReport("Ostrava, CZ", ZoneOffset.ofHours(1), reading, listOf(reading))
        compose.setContent { TamzTheme { WeatherApp("Ostrava", {}, WeatherState.Ready(report), {}) } }
        compose.onNodeWithText("7,5 °C").assertIsDisplayed()
        compose.onNodeWithContentDescription("zataženo").assertIsDisplayed()
        compose.onNodeWithText("Předpověď").performClick()
        compose.onNodeWithText("8. 11. 2026 · 00:00").assertIsDisplayed()
        compose.onNodeWithText("7,5 °C · zataženo").assertIsDisplayed()
        compose.onNodeWithText("Aktuálně").performClick()
        compose.onNodeWithText("Teplota").assertIsDisplayed()
    }

    @Test
    fun loadingDisablesDuplicateRequests() {
        compose.setContent { TamzTheme { WeatherApp("Ostrava", {}, WeatherState.Loading, {}) } }
        compose.onNodeWithText("Načítání počasí…").assertIsDisplayed()
        compose.onNodeWithText("Aktualizovat").assertIsNotEnabled()
    }

    @Test
    fun failureAllowsRetryAndCityCorrection() {
        var requests = 0
        compose.setContent { TamzTheme {
            WeatherApp("Ostravaa", {}, WeatherState.Failed("Město nebylo nalezeno. Zkontrolujte název.", "HTTP 404"), { requests++ })
        } }
        compose.onNodeWithText("Město nebylo nalezeno. Zkontrolujte název.").assertIsDisplayed()
        compose.onNodeWithText("Město").assertIsEnabled()
        compose.onNodeWithText("Aktualizovat").performClick()
        compose.runOnIdle { assertEquals(1, requests) }
    }
}
