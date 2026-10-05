package com.adamekka.tamz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adamekka.tamz.ui.theme.TamzTheme
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import kotlin.math.pow
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TamzTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SavingsCalculator(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SavingsCalculator(modifier: Modifier = Modifier) {
    var deposit by rememberSaveable {
        mutableDoubleStateOf(50_000.0)
    }

    var interestRate by rememberSaveable {
        mutableDoubleStateOf(5.0)
    }

    var periods by rememberSaveable {
        mutableIntStateOf(5)
    }

    val total = deposit * (1.0 + interestRate / 100.0).pow(periods)
    val earnedInterest = total - deposit

    val chartData =
        remember(deposit, earnedInterest) {
            listOf(
                Bars(
                    label = "Vklad / Úroky",
                    values =
                        listOf(
                            Bars.Data(
                                id = 0,
                                label = "Vklad",
                                value = deposit,
                                color = SolidColor(Color(0xFFD0FFF1)),
                                animator = Animatable(1f)
                            ),
                            Bars.Data(
                                id = 1,
                                label = "Úroky",
                                value = earnedInterest,
                                color = SolidColor(Color(0xFF9BD9CC)),
                                animator = Animatable(1f)
                            )
                        )
                )
            )
        }

    val chartTextStyle =
        MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Naspořená suma: ${total.roundToInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Z toho úroky: ${earnedInterest.roundToInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            ColumnChart(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(16.dp),
                data = chartData,
                barProperties =
                    BarProperties(
                        thickness = 64.dp,
                        spacing = 16.dp
                    ),
                labelProperties =
                    LabelProperties(
                        enabled = true,
                        textStyle = chartTextStyle
                    ),
                indicatorProperties =
                    HorizontalIndicatorProperties(
                        textStyle = chartTextStyle
                    ),
                labelHelperProperties =
                    LabelHelperProperties(
                        textStyle = chartTextStyle
                    ),
                animationMode = AnimationMode.None,
                animationDelay = 0,
                minValue = 0.0,
                maxValue = maxOf(deposit, earnedInterest).coerceAtLeast(1.0)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Vklad: ${deposit.roundToInt()}")

                Slider(
                    value = deposit.toFloat(),
                    onValueChange = { newValue ->
                        deposit = newValue.toDouble()
                    },
                    valueRange = 0f..500_000f,
                    steps = 49
                )

                Text("Úrok: ${interestRate.roundToInt()} %")

                Slider(
                    value = interestRate.toFloat(),
                    onValueChange = { interestRate = it.toDouble() },
                    valueRange = 0f..10f,
                    steps = 9
                )

                Text("Období: $periods")

                Slider(
                    value = periods.toFloat(),
                    onValueChange = { periods = it.roundToInt() },
                    valueRange = 0f..30f,
                    steps = 29
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TamzTheme {
        SavingsCalculator()
    }
}
