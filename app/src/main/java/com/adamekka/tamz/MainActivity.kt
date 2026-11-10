package com.adamekka.tamz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.adamekka.tamz.ui.theme.TamzTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider(this)[WeatherViewModel::class.java]
        setContent {
            TamzTheme { WeatherApp(model.city, { model.city = it }, model.state, model::refresh) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeatherApp(city: String, onCityChange: (String) -> Unit, state: WeatherState, onRefresh: () -> Unit) {
    var forecast by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val dateFormat = remember { DateTimeFormatter.ofPattern("d. M. yyyy · HH:mm", Locale.forLanguageTag("cs-CZ")) }
    val canRefresh = city.isNotBlank() && state != WeatherState.Loading

    fun refresh() {
        focus.clearFocus()
        onRefresh()
    }

    BackHandler(enabled = forecast) { forecast = false }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Počasí") }) },
        bottomBar = {
            PrimaryTabRow(if (forecast) 1 else 0, Modifier.navigationBarsPadding()) {
                Tab(!forecast, onClick = { forecast = false }, text = { Text("Aktuálně") })
                Tab(forecast, onClick = { focus.clearFocus(); forecast = true }, text = { Text("Předpověď") })
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize().imePadding(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state) {
                WeatherState.Empty -> if (forecast) item {
                    TextButton(onClick = { forecast = false }) { Text("Zadat město") }
                }
                WeatherState.Loading -> item {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Načítání počasí…", Modifier.padding(top = 8.dp))
                }
                is WeatherState.Failed -> item {
                    Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        if (BuildConfig.DEBUG) Text(state.detail, style = MaterialTheme.typography.bodySmall)
                        if (forecast) TextButton(onClick = { forecast = false }) { Text("Zpět na město") }
                    }
                }
                is WeatherState.Ready -> {
                    val report = state.report
                    item { Text(report.city, style = MaterialTheme.typography.titleLarge) }
                    if (forecast) {
                        items(report.forecast) { entry ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Image(painterResource(entry.icon), contentDescription = null, Modifier.size(56.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(dateFormat.format(entry.time.atOffset(report.offset)), style = MaterialTheme.typography.titleMedium)
                                        Text("${formatTemperature(entry.temperature)} · ${entry.description}")
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Teplota", style = MaterialTheme.typography.labelLarge)
                                    Text(formatTemperature(report.current.temperature), style = MaterialTheme.typography.headlineLarge)
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Popis počasí", style = MaterialTheme.typography.labelLarge)
                                    Text(report.current.description, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Ikona počasí", style = MaterialTheme.typography.labelLarge)
                                    Image(painterResource(report.current.icon), contentDescription = report.current.description, Modifier.size(80.dp))
                                }
                            }
                        }
                    }
                    item { Text("Zdroj: OpenWeatherMap", style = MaterialTheme.typography.bodySmall) }
                }
            }
            if (!forecast) {
                item {
                    OutlinedTextField(
                        value = city, onValueChange = onCityChange, label = { Text("Město") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = state != WeatherState.Loading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { if (canRefresh) refresh() })
                    )
                }
                item {
                    Button(onClick = ::refresh, enabled = canRefresh, modifier = Modifier.fillMaxWidth()) { Text("Aktualizovat") }
                }
            }
        }
    }
}

private fun formatTemperature(temperature: Double): String =
    String.format(Locale.forLanguageTag("cs-CZ"), "%.1f °C", temperature)
