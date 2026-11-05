package com.adamekka.tamz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class LevelEntry(val id: String, val level: Level, val custom: Boolean, val game: GameState, val bestScore: Int?)

sealed interface EditorTarget {
    data object New : EditorTarget
    data class Existing(val id: String) : EditorTarget
}

class GameStore private constructor(private val file: File, bundled: List<LevelEntry>) {
    var levels by mutableStateOf(bundled)
        private set
    var selectedId by mutableStateOf(bundled.first().id)
        private set

    fun select(id: String) {
        val entry = levels.first { it.id == id }
        selectedId = id
        if (entry.game.boxes == entry.level.goals) restart()
    }

    fun restart() {
        levels = levels.map { if (it.id == selectedId) it.copy(game = it.level.initial) else it }
    }

    fun move(direction: Direction): Boolean {
        val index = levels.indexOfFirst { it.id == selectedId }
        check(index >= 0) { "Vybraná úroveň neexistuje." }
        val entry = levels[index]
        val moved = entry.level.move(entry.game, direction)
        if (moved == entry.game) return false
        val solved = moved.boxes == entry.level.goals
        val best = if (solved) minOf(entry.bestScore ?: moved.moves, moved.moves) else entry.bestScore
        levels = levels.map { if (it.id == entry.id) it.copy(game = moved, bestScore = best) else it }
        if (solved && index < levels.lastIndex) select(levels[index + 1].id)
        return true
    }

    fun saveLevel(target: EditorTarget, level: Level) {
        when (target) {
            EditorTarget.New -> {
                val id = "custom-${UUID.randomUUID()}"
                levels = levels + LevelEntry(id, level, true, level.initial, null)
                selectedId = id
            }
            is EditorTarget.Existing -> {
                val old = levels.first { it.id == target.id }
                check(old.custom) { "Vestavěnou úroveň nelze přepsat." }
                // A renamed board keeps its records; a changed puzzle starts a separate attempt.
                val updated = if (old.level.rows == level.rows) old.copy(level = level)
                    else old.copy(level = level, game = level.initial, bestScore = null)
                levels = levels.map { if (it.id == target.id) updated else it }
                selectedId = target.id
            }
        }
    }

    fun deleteLevel(id: String) {
        check(levels.first { it.id == id }.custom) { "Vestavěnou úroveň nelze smazat." }
        levels = levels.filterNot { it.id == id }
        if (selectedId == id) selectedId = levels.first().id
    }

    fun save(): Result<Unit> = runCatching {
        val custom = JSONArray()
        val savedGames = JSONObject()
        val scores = JSONObject()
        levels.forEach { entry ->
            if (entry.custom) custom.put(JSONObject().put("id", entry.id).put("name", entry.level.name)
                .put("rows", JSONArray(entry.level.rows)))
            val state = entry.game
            val boxes = JSONArray()
            state.boxes.forEach { boxes.put(JSONArray(listOf(it.x, it.y))) }
            savedGames.put(entry.id, JSONObject().put("x", state.player.x).put("y", state.player.y)
                .put("boxes", boxes).put("moves", state.moves))
            entry.bestScore?.let { scores.put(entry.id, it) }
        }
        // The save is small; a direct write finishes before the next input or activity reload.
        file.writeText(JSONObject().put("version", 1).put("selected", selectedId)
            .put("custom", custom).put("games", savedGames).put("scores", scores).toString())
    }

    companion object {
        fun open(bundled: List<Level>, path: File): Result<GameStore> = runCatching {
            require(bundled.isNotEmpty()) { "Chybí vestavěné úrovně." }
            val entries = bundled.map { level ->
                // Board-based IDs keep progress stable when titles or collection order change.
                val digest = MessageDigest.getInstance("SHA-256")
                    .digest(level.rows.joinToString("\n").toByteArray(Charsets.UTF_8))
                LevelEntry("builtin-${digest.joinToString("") { "%02x".format(it) }}", level, false, level.initial, null)
            }
            require(entries.map { it.id }.distinct().size == entries.size) { "Vestavěné úrovně se opakují." }
            val store = GameStore(path, entries)
            // A missing save is the explicit first-run case. Corrupt saves are reported, never replaced.
            if (!path.exists()) return@runCatching store
            val json = JSONObject(path.readText())
            require(json.getInt("version") == 1) { "Nepodporovaná verze uložených dat." }
            val custom = json.getJSONArray("custom")
            for (index in 0 until custom.length()) {
                val entry = custom.getJSONObject(index)
                val rows = entry.getJSONArray("rows")
                val level = Level(entry.getString("name"), List(rows.length()) { rows.getString(it) })
                store.levels = store.levels + LevelEntry(
                    entry.getString("id"), level, true, level.initial, null
                )
            }
            require(store.levels.map { it.id }.distinct().size == store.levels.size) { "Opakované ID úrovně v uložených datech." }
            val savedGames = json.getJSONObject("games")
            val savedScores = json.getJSONObject("scores")
            store.levels = store.levels.map { entry ->
                val state = if (savedGames.has(entry.id)) {
                    val saved = savedGames.getJSONObject(entry.id)
                    val boxes = saved.getJSONArray("boxes")
                    entry.level.restore(Cell(saved.getInt("x"), saved.getInt("y")),
                        (0 until boxes.length()).map {
                            val cell = boxes.getJSONArray(it)
                            Cell(cell.getInt(0), cell.getInt(1))
                        }.toSet(), saved.getInt("moves"))
                } else {
                    // Newly bundled levels have no previous attempt to restore.
                    entry.level.initial
                }
                val best = if (savedScores.has(entry.id)) savedScores.getInt(entry.id).also {
                    require(it > 0) { "Neplatný počet tahů v uloženém skóre." }
                } else null
                entry.copy(game = state, bestScore = best)
            }
            store.selectedId = json.getString("selected")
            require(store.levels.any { it.id == store.selectedId }) { "Uložená úroveň již neexistuje." }
            store
        }
    }
}
