package com.adamekka.tamz

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adamekka.tamz.ui.theme.TamzTheme
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Pie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TamzTheme {
                SavingsCalculator()
            }
        }
    }
}

private enum class Screen(val title: String, val tabTitle: String) {
    Calculator("Spořicí kalkulačka", "Spoření"),
    ChartType("Typ grafu", "Graf"),
    Settings("Nastavení", "Nastavení"),
    History("Historie", "Historie")
}

private enum class ChartType(val title: String) {
    Bar("Sloupcový"),
    Donut("Prstencový")
}

private enum class ChartColor(val title: String, val deposit: Color, val interest: Color) {
    Green("Zelená", Color(0xFF98D9C8), Color(0xFF247A65)),
    Blue("Modrá", Color(0xFF9FC9F5), Color(0xFF2864AA)),
    Purple("Fialová", Color(0xFFCEB1EB), Color(0xFF7946A5))
}

private sealed interface HistoryState {
    data object Loading : HistoryState
    data class Ready(val entries: List<SavedCalculation>) : HistoryState
    data class Failed(val cause: Throwable) : HistoryState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsCalculator() {
    var screen by rememberSaveable { mutableStateOf(Screen.Calculator) }
    var chartType by rememberSaveable { mutableStateOf(ChartType.Bar) }
    var chartColor by rememberSaveable { mutableStateOf(ChartColor.Green) }
    var deposit by rememberSaveable { mutableDoubleStateOf(50_000.0) }
    var monthlyDeposit by rememberSaveable { mutableDoubleStateOf(0.0) }
    var interestRate by rememberSaveable { mutableDoubleStateOf(5.0) }
    var years by rememberSaveable { mutableDoubleStateOf(5.0) }
    var taxRate by rememberSaveable { mutableDoubleStateOf(0.0) }
    var inflation by rememberSaveable { mutableDoubleStateOf(0.0) }
    var depositMaximum by rememberSaveable { mutableIntStateOf(500_000) }
    var depositIncrement by rememberSaveable { mutableIntStateOf(10_000) }
    var monthlyMaximum by rememberSaveable { mutableIntStateOf(10_000) }
    var monthlyIncrement by rememberSaveable { mutableIntStateOf(100) }
    var interestMaximum by rememberSaveable { mutableIntStateOf(10) }
    var interestIncrement by rememberSaveable { mutableIntStateOf(1) }
    var yearsMaximum by rememberSaveable { mutableIntStateOf(30) }
    var yearsIncrement by rememberSaveable { mutableIntStateOf(1) }
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("savings", Context.MODE_PRIVATE)
    }
    var history by remember { mutableStateOf<HistoryState>(HistoryState.Loading) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    suspend fun loadHistory() {
        history = HistoryState.Loading
        history = withContext(Dispatchers.IO) { readSavingsHistory(preferences) }.fold(
            onSuccess = { HistoryState.Ready(it) },
            onFailure = { HistoryState.Failed(it) }
        )
    }

    fun errorMessage(message: String, cause: Throwable): String =
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            "$message\n$cause"
        } else {
            message
        }

    LaunchedEffect(Unit) { loadHistory() }

    val input = SavingsInput(deposit, monthlyDeposit, interestRate, years.roundToInt(), taxRate, inflation)
    val result = calculateSavings(input)
    val scrollState = key(screen) { rememberScrollState() }
    val money = remember {
        NumberFormat.getIntegerInstance(Locale.forLanguageTag("cs-CZ"))
    }

    BackHandler(enabled = screen != Screen.Calculator) { screen = Screen.Calculator }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = { Text(screen.title) })
        },
        bottomBar = {
            PrimaryTabRow(
                selectedTabIndex = screen.ordinal,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Screen.entries.forEach { destination ->
                    Tab(
                        selected = screen == destination,
                        onClick = { screen = destination },
                        text = { Text(destination.tabTitle) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (screen) {
                Screen.Calculator -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Naspořená suma: ${money.format(result.total)} Kč",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text("Z toho úroky po dani: ${money.format(result.interest)} Kč")
                            Text("Hodnota po inflaci: ${money.format(result.inflationAdjusted)} Kč")
                            when (val state = history) {
                                is HistoryState.Ready -> Button(
                                    enabled = !saving,
                                    onClick = {
                                        val entries = listOf(SavedCalculation(System.currentTimeMillis(), input)) +
                                            state.entries
                                        saving = true
                                        scope.launch {
                                            val saved = withContext(Dispatchers.IO) {
                                                writeSavingsHistory(preferences, entries)
                                            }
                                            saving = false
                                            saved.fold(
                                                onSuccess = {
                                                    history = HistoryState.Ready(entries)
                                                    snackbar.showSnackbar("Výpočet uložen.")
                                                },
                                                onFailure = {
                                                    snackbar.showSnackbar(
                                                        errorMessage("Výpočet se nepodařilo uložit. Zkuste to znovu.", it)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                ) { Text(if (saving) "Ukládání…" else "Uložit") }
                                HistoryState.Loading -> Text("Načítání historie…")
                                is HistoryState.Failed -> TextButton(onClick = { screen = Screen.History }) {
                                    Text("Historii nelze načíst")
                                }
                            }
                        }
                    }
                    SavingsChart(result.contributions, result.interest, chartType, chartColor)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            NumberSlider(
                                "Počáteční vklad: ${money.format(deposit)} Kč", deposit,
                                SliderRange(depositMaximum, depositIncrement)
                            ) { deposit = it }
                            NumberSlider(
                                "Měsíční vklad: ${money.format(monthlyDeposit)} Kč", monthlyDeposit,
                                SliderRange(monthlyMaximum, monthlyIncrement)
                            ) { monthlyDeposit = it }
                            NumberSlider(
                                "Roční úrok: ${interestRate.roundToInt()} %", interestRate,
                                SliderRange(interestMaximum, interestIncrement)
                            ) { interestRate = it }
                            NumberSlider(
                                "Doba spoření: ${years.roundToInt()} let", years,
                                SliderRange(yearsMaximum, yearsIncrement)
                            ) { years = it }
                            NumberSlider(
                                "Daň z úroků: ${taxRate.roundToInt()} %", taxRate, SliderRange(100, 1)
                            ) { taxRate = it }
                            NumberSlider(
                                "Roční inflace: ${inflation.roundToInt()} %", inflation, SliderRange(20, 1)
                            ) { inflation = it }
                        }
                    }
                }

                Screen.ChartType -> {
                    Choices("Typ grafu", ChartType.entries, chartType, { it.title }) {
                        chartType = it
                    }
                    SavingsChart(result.contributions, result.interest, chartType, chartColor)
                }

                Screen.Settings -> {
                    // Settings changes also snap the current value to the new slider's ticks.
                    Choices(
                        "Maximální vklad", listOf(100_000, 500_000, 1_000_000),
                        depositMaximum, { "${money.format(it)} Kč" }
                    ) {
                        depositMaximum = it
                        deposit = SliderRange(it, depositIncrement).snap(deposit)
                    }
                    Choices(
                        "Krok vkladu", listOf(1_000, 5_000, 10_000),
                        depositIncrement, { "${money.format(it)} Kč" }
                    ) {
                        depositIncrement = it
                        deposit = SliderRange(depositMaximum, it).snap(deposit)
                    }
                    Choices(
                        "Maximální měsíční vklad", listOf(10_000, 50_000, 100_000),
                        monthlyMaximum, { "${money.format(it)} Kč" }
                    ) {
                        monthlyMaximum = it
                        monthlyDeposit = SliderRange(it, monthlyIncrement).snap(monthlyDeposit)
                    }
                    Choices(
                        "Krok měsíčního vkladu", listOf(100, 500, 1_000),
                        monthlyIncrement, { "${money.format(it)} Kč" }
                    ) {
                        monthlyIncrement = it
                        monthlyDeposit = SliderRange(monthlyMaximum, it).snap(monthlyDeposit)
                    }
                    Choices(
                        "Maximální úrok", listOf(10, 20, 30),
                        interestMaximum, { "$it %" }
                    ) {
                        interestMaximum = it
                        interestRate = SliderRange(it, interestIncrement).snap(interestRate)
                    }
                    Choices("Krok úroku", listOf(1, 2, 5), interestIncrement, { "$it %" }) {
                        interestIncrement = it
                        interestRate = SliderRange(interestMaximum, it).snap(interestRate)
                    }
                    Choices(
                        "Maximální doba spoření", listOf(10, 30, 50),
                        yearsMaximum, { "$it let" }
                    ) {
                        yearsMaximum = it
                        years = SliderRange(it, yearsIncrement).snap(years)
                    }
                    Choices("Krok doby spoření", listOf(1, 2, 5), yearsIncrement, { "$it let" }) {
                        yearsIncrement = it
                        years = SliderRange(yearsMaximum, it).snap(years)
                    }
                    Choices("Barva grafu", ChartColor.entries, chartColor, { it.title }) {
                        chartColor = it
                    }
                }

                Screen.History -> {
                    when (val state = history) {
                        HistoryState.Loading -> Text("Načítání historie…")
                        is HistoryState.Failed -> {
                            Text(errorMessage("Historii se nepodařilo načíst. Zkuste to znovu.", state.cause))
                            TextButton(onClick = { scope.launch { loadHistory() } }) {
                                Text("Zkusit znovu")
                            }
                        }
                        is HistoryState.Ready -> {
                            if (state.entries.isEmpty()) {
                                Text("Uložte výpočet tlačítkem Uložit v kalkulačce.")
                            }
                            val dateFormat = remember {
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            }
                            state.entries.forEach { entry ->
                                val savedResult = calculateSavings(entry.input)
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(dateFormat.format(Date(entry.savedAt)))
                                        Text(
                                            "Naspořená suma: ${money.format(savedResult.total)} Kč",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text("Z toho úroky po dani: ${money.format(savedResult.interest)} Kč")
                                        Text("Hodnota po inflaci: ${money.format(savedResult.inflationAdjusted)} Kč")
                                        Text("Počáteční vklad: ${money.format(entry.input.initialDeposit)} Kč")
                                        Text("Měsíční vklad: ${money.format(entry.input.monthlyDeposit)} Kč")
                                        Text("Roční úrok: ${entry.input.annualInterest.roundToInt()} %")
                                        Text("Doba spoření: ${entry.input.years} let")
                                        Text("Daň z úroků: ${entry.input.taxRate.roundToInt()} %")
                                        Text("Roční inflace: ${entry.input.annualInflation.roundToInt()} %")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberSlider(
    title: String,
    value: Double,
    range: SliderRange,
    onValueChange: (Double) -> Unit
) {
    Text(title)
    Slider(
        modifier = Modifier.semantics { contentDescription = title },
        value = value.toFloat(),
        onValueChange = { onValueChange(range.snap(it.toDouble())) },
        valueRange = 0f..range.maximum.toFloat(),
        steps = range.maximum / range.increment - 1
    )
}

@Composable
private fun <T> Choices(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
}

@Composable
private fun SavingsChart(
    deposit: Double,
    earnedInterest: Double,
    type: ChartType,
    color: ChartColor
) {
    val textStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurface
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        if (deposit + earnedInterest == 0.0) {
            // Neither chart has a meaningful scale or share when the total is zero.
            Text("Zadejte vklad pro zobrazení grafu.", modifier = Modifier.padding(16.dp))
        } else {
            when (type) {
                ChartType.Bar -> {
                    val data = remember(deposit, earnedInterest, color) {
                        listOf(
                            Bars(
                                label = "Vklady / Úroky",
                                values = listOf(
                                    Bars.Data(
                                        id = 0, label = "Vklady", value = deposit,
                                        color = SolidColor(color.deposit), animator = Animatable(1f)
                                    ),
                                    Bars.Data(
                                        id = 1, label = "Úroky", value = earnedInterest,
                                        color = SolidColor(color.interest), animator = Animatable(1f)
                                    )
                                )
                            )
                        )
                    }
                    ColumnChart(
                        modifier = Modifier.fillMaxWidth().height(260.dp).padding(16.dp),
                        data = data,
                        barProperties = BarProperties(thickness = 48.dp, spacing = 16.dp),
                        labelProperties = LabelProperties(enabled = true, textStyle = textStyle),
                        indicatorProperties = HorizontalIndicatorProperties(textStyle = textStyle),
                        labelHelperProperties = LabelHelperProperties(textStyle = textStyle),
                        animationMode = AnimationMode.None,
                        animationDelay = 0,
                        minValue = 0.0,
                        maxValue = maxOf(deposit, earnedInterest)
                    )
                }

                ChartType.Donut -> {
                    val data = remember(deposit, earnedInterest, color) {
                        listOf(
                            Pie(label = "Vklady", data = deposit, color = color.deposit),
                            Pie(label = "Úroky", data = earnedInterest, color = color.interest)
                        )
                    }
                    PieChart(
                        modifier = Modifier.fillMaxWidth().height(260.dp).padding(16.dp),
                        data = data,
                        style = Pie.Style.Stroke(width = 48.dp),
                        labelHelperProperties = LabelHelperProperties(textStyle = textStyle)
                    )
                    val percent = remember {
                        NumberFormat.getPercentInstance(Locale.forLanguageTag("cs-CZ")).apply {
                            maximumFractionDigits = 1
                        }
                    }
                    Text(
                        "Vklady: ${percent.format(deposit / (deposit + earnedInterest))}   " +
                            "Úroky: ${percent.format(earnedInterest / (deposit + earnedInterest))}",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SavingsCalculatorPreview() {
    TamzTheme { SavingsCalculator() }
}
