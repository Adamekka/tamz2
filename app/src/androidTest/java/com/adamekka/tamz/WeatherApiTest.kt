package com.adamekka.tamz

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WeatherApiTest {
    private fun current() = JSONObject("""{
        "name":"Ostrava","sys":{"country":"CZ"},"dt":1794092400,
        "main":{"temp":7.47},"weather":[{"description":"zataženo","icon":"04n"}]
    }""")

    private fun forecast() = JSONObject("""{
        "city":{"timezone":3600},"list":[
            {"dt":1794096000,"main":{"temp":6.7},"weather":[{"description":"jasno","icon":"01n"}]},
            {"dt":1794106800,"main":{"temp":5.4},"weather":[{"description":"déšť","icon":"10d"}]}
        ]
    }""")

    @Test
    fun parsesBothResponsesAndCityTimeZone() {
        val report = parseWeatherReport(current(), forecast())
        assertEquals("Ostrava, CZ", report.city)
        assertEquals(7.47, report.current.temperature, 0.001)
        assertEquals("zataženo", report.current.description)
        assertEquals(R.drawable.weather_04d, report.current.icon)
        assertEquals(2, report.forecast.size)
        assertEquals(Instant.ofEpochSecond(1794096000), report.forecast[0].time)
        assertEquals(3600, report.offset.totalSeconds)
        assertEquals(R.drawable.weather_01n, report.forecast[0].icon)
        assertEquals(R.drawable.weather_10d, report.forecast[1].icon)
    }

    @Test
    fun rejectsMissingTemperatureInsteadOfShowingZero() {
        val current = current()
        current.getJSONObject("main").remove("temp")
        assertThrows(JSONException::class.java) { parseWeatherReport(current, forecast()) }
    }

    @Test
    fun rejectsEmptyForecast() {
        val forecast = forecast().put("list", org.json.JSONArray())
        assertThrows(JSONException::class.java) { parseWeatherReport(current(), forecast) }
    }

    @Test
    fun rejectsUnknownIconsInsteadOfShowingWrongWeather() {
        val current = current()
        current.getJSONArray("weather").getJSONObject(0).put("icon", "99x")
        assertThrows(JSONException::class.java) { parseWeatherReport(current, forecast()) }
    }

    @Test
    fun requiresExplicitKeyAndCity() {
        assertThrows(IllegalArgumentException::class.java) { WeatherApi("") }
        assertThrows(IllegalArgumentException::class.java) { WeatherApi("test-key").load(" ") }
    }
}
