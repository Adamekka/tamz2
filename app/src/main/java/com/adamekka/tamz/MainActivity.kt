package com.adamekka.tamz

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adamekka.tamz.ui.theme.TamzTheme
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TamzTheme { BarcodeApp() }
        }
    }
}

private enum class Screen(val title: String) {
    Generate("Vytvořit"),
    Scan("Načíst")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.Generate) }
    var input by rememberSaveable { mutableStateOf("") }
    var inputError by rememberSaveable { mutableStateOf<String?>(null) }
    var barcode by rememberSaveable { mutableStateOf<UpcA?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scanner = remember(context) {
        GmsBarcodeScanning.getClient(
            context,
            GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_UPC_A)
                .enableAutoZoom()
                .build()
        )
    }

    fun generate() {
        when (val result = UpcA.parse(input)) {
            is UpcResult.Valid -> {
                barcode = result.barcode
                inputError = null
                keyboard?.hide()
            }
            is UpcResult.Invalid -> {
                barcode = null
                inputError = result.message
            }
        }
    }

    fun errorMessage(message: String, error: Throwable): String =
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            "$message\n$error"
        } else {
            message
        }

    BackHandler(enabled = screen == Screen.Scan) { screen = Screen.Generate }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Čárový kód UPC-A") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            PrimaryTabRow(
                selectedTabIndex = screen.ordinal,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Screen.entries.forEach { destination ->
                    Tab(
                        selected = screen == destination,
                        onClick = { screen = destination },
                        text = { Text(destination.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (screen) {
                Screen.Generate -> {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            inputError = null
                            barcode = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("12místný kód UPC-A") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = inputError != null,
                        supportingText = { inputError?.let { Text(it) } }
                    )
                    Button(onClick = { generate() }) { Text("Vykreslit") }
                    val displayed = barcode
                    if (displayed != null) {
                        BarcodeView(displayed)
                        Button(
                            enabled = !saving,
                            onClick = {
                                saving = true
                                scope.launch {
                                    val result = try {
                                        withContext(Dispatchers.IO) {
                                            saveBarcode(context, displayed)
                                        }
                                    } finally {
                                        saving = false
                                    }
                                    result.fold(
                                        onSuccess = { snackbar.showSnackbar("Uloženo do galerie.") },
                                        onFailure = {
                                            snackbar.showSnackbar(
                                                errorMessage("Obrázek se nepodařilo uložit. Zkuste to znovu.", it)
                                            )
                                        }
                                    )
                                }
                            }
                        ) { Text(if (saving) "Ukládání…" else "Uložit do galerie") }
                    }
                }
                Screen.Scan -> {
                    Button(
                        enabled = !scanning,
                        onClick = {
                            scanning = true
                            scope.launch {
                                try {
                                    // Await the Play services task so late results cannot update a disposed screen.
                                    val scanned = try {
                                        suspendCancellableCoroutine<Barcode> { continuation ->
                                            scanner.startScan()
                                                .addOnSuccessListener { continuation.resume(it) }
                                                .addOnFailureListener {
                                                    // Play services can report user cancellation as an error rather than a cancelled task.
                                                    if (it is MlKitException && it.errorCode == MlKitException.CODE_SCANNER_CANCELLED) {
                                                        continuation.cancel()
                                                    } else {
                                                        continuation.resumeWithException(it)
                                                    }
                                                }
                                                .addOnCanceledListener { continuation.cancel() }
                                        }
                                    } finally {
                                        scanning = false
                                    }
                                    val value = scanned.rawValue
                                    if (value == null) {
                                        snackbar.showSnackbar("Kód se nepodařilo přečíst. Zkuste to znovu.")
                                    } else {
                                        input = value
                                        generate()
                                        screen = Screen.Generate
                                    }
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (error: Exception) {
                                    snackbar.showSnackbar(
                                        errorMessage(
                                            "Skener nelze otevřít. Zkontrolujte Služby Google Play a zkuste to znovu.",
                                            error
                                        )
                                    )
                                }
                            }
                        }
                    ) { Text(if (scanning) "Načítání…" else "Načíst UPC-A fotoaparátem") }
                }
            }
        }
    }
}
