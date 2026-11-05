package com.adamekka.tamz

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamekka.tamz.ui.theme.TamzTheme
import java.io.File
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TamzTheme { SokobanApp() } }
    }
}

private enum class Screen(val title: String) {
    Game("Hra"), Levels("Úrovně"), Editor("Editor")
}

@Composable
fun SokobanApp() {
    val context = LocalContext.current
    val levels = remember {
        parseLevels(context.assets.open("levels.txt").bufferedReader().use { it.readText() }).getOrThrow()
    }
    val path = remember { File(context.filesDir, "sokoban.json") }
    var result by remember { mutableStateOf(GameStore.open(levels, path)) }
    result.fold(
        onSuccess = { SokobanScreens(it) },
        onFailure = { error ->
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("Uloženou hru nelze načíst. Pokud chyba trvá, vymažte data aplikace v nastavení telefonu.")
                if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) Text(error.toString())
                Button(onClick = { result = GameStore.open(levels, path) }) { Text("Zkusit znovu") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SokobanScreens(store: GameStore) {
    var screen by rememberSaveable { mutableStateOf(Screen.Game) }
    var draft by remember { mutableStateOf(EditorDraft.empty()) }
    var deleting by remember { mutableStateOf<LevelEntry?>(null) }
    var saveError by remember { mutableStateOf("") }
    val context = LocalContext.current

    fun save() {
        saveError = store.save().fold({ "" }, { error ->
            if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                "Hru se nepodařilo uložit.\n$error"
            } else "Hru se nepodařilo uložit."
        })
    }

    BackHandler(enabled = screen != Screen.Game) { screen = Screen.Game }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Sokoban") }) },
        bottomBar = {
            PrimaryTabRow(screen.ordinal, modifier = Modifier.navigationBarsPadding()) {
                Screen.entries.forEach { tab ->
                    Tab(selected = screen == tab, onClick = { screen = tab }, text = { Text(tab.title) })
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (saveError.isNotEmpty()) Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(saveError, Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = ::save) { Text("Zkusit znovu") }
            }
            when (screen) {
                Screen.Game -> {
                    val entry = store.levels.first { it.id == store.selectedId }
                    GameScreen(entry,
                        onMove = { if (store.move(it)) save() },
                        onRestart = { store.restart(); save() })
                }
                Screen.Levels -> LazyColumn(Modifier.testTag("level-list"), contentPadding = PaddingValues(16.dp)) {
                    items(store.levels, key = { it.id }) { entry ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                store.select(entry.id)
                                save()
                                screen = Screen.Game
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FittedBoard(entry.level.rows, Modifier.size(80.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.level.name, style = MaterialTheme.typography.titleMedium)
                                Text(entry.bestScore?.let { "Nejméně tahů: $it" } ?: "Dosud nesplněno")
                                if (entry.game.moves > 0 && entry.game.boxes != entry.level.goals) {
                                    Text("Rozehráno, tahy: ${entry.game.moves}")
                                }
                                if (entry.custom) Row {
                                    TextButton(onClick = {
                                        draft = EditorDraft(EditorTarget.Existing(entry.id), entry.level.name, entry.level.rows)
                                        screen = Screen.Editor
                                    }) { Text("Upravit") }
                                    TextButton(onClick = { deleting = entry }) { Text("Smazat") }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
                Screen.Editor -> EditorScreen(draft, onChange = { draft = it }, onSave = { level ->
                    store.saveLevel(draft.target, level)
                    draft = EditorDraft(EditorTarget.Existing(store.selectedId), level.name, level.rows)
                    save()
                    screen = Screen.Game
                })
            }
        }
    }
    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Smazat úroveň ${entry.level.name}?") },
            text = { Text("Smaže se také její rozehraná hra a skóre.") },
            confirmButton = {
                TextButton(onClick = {
                    store.deleteLevel(entry.id)
                    if (draft.target == EditorTarget.Existing(entry.id)) draft = EditorDraft.empty()
                    deleting = null
                    save()
                }) { Text("Smazat") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Zrušit") } }
        )
    }
}

@Composable
private fun GameScreen(entry: LevelEntry, onMove: (Direction) -> Unit, onRestart: () -> Unit) {
    val state = entry.game
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(entry.level.name, style = MaterialTheme.typography.titleLarge)
        Text("Tahy: ${state.moves}     Bedny v cíli: ${state.boxes.count { it in entry.level.goals }}/${state.boxes.size}")
        Board(entry.level.render(state), Modifier.widthIn(max = 360.dp).fillMaxWidth().testTag("game-board")
            .semantics { contentDescription = "Herní plocha" }
            .pointerInput(entry.id) {
                var drag = Offset.Zero
                detectDragGestures(
                    onDragStart = { drag = Offset.Zero },
                    onDrag = { change, amount -> change.consume(); drag += amount },
                    onDragEnd = {
                        if (maxOf(abs(drag.x), abs(drag.y)) > viewConfiguration.touchSlop * 2) {
                            onMove(if (abs(drag.x) > abs(drag.y)) {
                                if (drag.x > 0) Direction.Right else Direction.Left
                            } else if (drag.y > 0) Direction.Down else Direction.Up)
                        }
                    }
                )
            })
        if (state.boxes == entry.level.goals) Text("Úroveň splněna.")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedButton(onClick = { onMove(Direction.Up) }, modifier = Modifier.size(64.dp, 48.dp).semantics { contentDescription = "Nahoru" },
                contentPadding = PaddingValues(0.dp)) { Text("↑", fontSize = 24.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onMove(Direction.Left) }, modifier = Modifier.size(64.dp, 48.dp).semantics { contentDescription = "Doleva" },
                    contentPadding = PaddingValues(0.dp)) { Text("←", fontSize = 24.sp) }
                OutlinedButton(onClick = { onMove(Direction.Down) }, modifier = Modifier.size(64.dp, 48.dp).semantics { contentDescription = "Dolů" },
                    contentPadding = PaddingValues(0.dp)) { Text("↓", fontSize = 24.sp) }
                OutlinedButton(onClick = { onMove(Direction.Right) }, modifier = Modifier.size(64.dp, 48.dp).semantics { contentDescription = "Doprava" },
                    contentPadding = PaddingValues(0.dp)) { Text("→", fontSize = 24.sp) }
            }
        }
        TextButton(onClick = onRestart) { Text("Začít znovu") }
    }
}

@Composable
private fun EditorScreen(draft: EditorDraft, onChange: (EditorDraft) -> Unit, onSave: (Level) -> Unit) {
    var tool by rememberSaveable { mutableStateOf(EditorTool.Wall) }
    var error by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    fun change(next: EditorDraft) { error = ""; onChange(next) }
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (draft.target == EditorTarget.New) "Nová úroveň" else "Upravit úroveň", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { change(EditorDraft.empty()) }) { Text("Nová") }
        }
        OutlinedTextField(draft.name, onValueChange = { change(draft.copy(name = it)) }, Modifier.fillMaxWidth(),
            label = { Text("Název úrovně") }, singleLine = true)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorTool.entries.forEach { option ->
                FilterChip(selected = tool == option, onClick = { focus.clearFocus(); tool = option }, label = { Text(option.title) })
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Large maps scroll instead of shrinking editable cells below a finger-sized target.
            val edge = maxOf(48.dp, maxWidth / draft.rows.first().length)
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                draft.rows.forEachIndexed { y, row ->
                    Row {
                        row.forEachIndexed { x, tile ->
                            val label = when (tile) {
                                '#' -> "zeď"; '.' -> "cíl"; '$' -> "bedna"; '*' -> "bedna na cíli"
                                '@' -> "hráč"; '+' -> "hráč na cíli"; else -> "podlaha"
                            }
                            Tile(tile, Modifier.size(edge).testTag("editor-$x-$y")
                                .semantics { contentDescription = "Řádek ${y + 1}, sloupec ${x + 1}: $label" }
                                .clickable(role = Role.Button, onClickLabel = "Umístit: ${tool.title}") { change(draft.paint(Cell(x, y), tool)) })
                        }
                    }
                }
            }
        }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            focus.clearFocus()
            runCatching { Level(draft.name.trim(), draft.rows) }.fold(onSave, { error = it.message ?: error("Chybí zpráva validace úrovně.") })
        }, modifier = Modifier.fillMaxWidth()) { Text("Uložit a hrát") }
    }
}
