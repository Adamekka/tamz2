package com.adamekka.tamz

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameStoreTest {
    private lateinit var directory: File
    private lateinit var path: File
    private val levels = listOf(
        Level("První", listOf("#####", "#@$.#", "#####")),
        Level("Druhá", listOf("#######", "#@ $ .#", "#     #", "#######"))
    )

    @Before
    fun setup() {
        directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "store-test-${System.nanoTime()}")
        check(directory.mkdirs())
        path = File(directory, "game.json")
    }

    @After
    fun cleanup() { directory.deleteRecursively() }

    @Test
    fun completionAdvancesAutomaticallyAndKeepsTheBestScore() {
        val store = GameStore.open(levels, path).getOrThrow()
        val first = store.selectedId
        assertTrue(store.move(Direction.Right))
        val second = store.selectedId
        assertTrue(first != second)
        assertEquals(1, store.levels[0].bestScore)
        repeat(3) { store.move(Direction.Right) }
        assertEquals(second, store.selectedId)
        assertEquals(3, store.levels[1].bestScore)
        assertFalse(store.move(Direction.Down))
        store.select(second)
        listOf(Direction.Down, Direction.Right, Direction.Up, Direction.Right, Direction.Right).forEach { store.move(it) }
        assertEquals(3, store.levels[1].bestScore)
        store.save().getOrThrow()
        val restored = GameStore.open(levels, path).getOrThrow()
        assertEquals(store.levels.map { it.bestScore }, restored.levels.map { it.bestScore })
        assertEquals(store.levels.map { it.game }, restored.levels.map { it.game })
        assertEquals(second, restored.selectedId)
    }

    @Test
    fun switchingLevelsAndReopeningRestoresEachAttempt() {
        val store = GameStore.open(levels, path).getOrThrow()
        val first = store.selectedId
        val second = store.levels[1].id
        store.select(second)
        repeat(2) { store.move(Direction.Right) }
        val attempt = store.levels[1].game
        store.select(first)
        store.save().getOrThrow()
        val restored = GameStore.open(levels, path).getOrThrow()
        assertEquals(first, restored.selectedId)
        restored.select(second)
        assertEquals(attempt, restored.levels[1].game)
        restored.restart()
        assertEquals(levels[1].initial, restored.levels[1].game)
    }

    @Test
    fun customLevelsCanBeCreatedRenamedChangedAndDeleted() {
        val store = GameStore.open(levels, path).getOrThrow()
        val custom = Level("Vlastní", levels[1].rows)
        store.saveLevel(EditorTarget.New, custom)
        val id = store.selectedId
        repeat(3) { store.move(Direction.Right) }
        store.save().getOrThrow()
        val restored = GameStore.open(levels, path).getOrThrow()
        assertTrue(restored.levels.last().custom)
        assertEquals("Vlastní", restored.levels.last().level.name)
        restored.saveLevel(EditorTarget.Existing(id), Level("Přejmenovaná", custom.rows))
        assertEquals(3, restored.levels.last().bestScore)
        restored.saveLevel(EditorTarget.Existing(id), Level("Změněná", levels[0].rows))
        assertNull(restored.levels.last().bestScore)
        assertEquals(0, restored.levels.last().game.moves)
        restored.deleteLevel(id)
        restored.save().getOrThrow()
        val deleted = GameStore.open(levels, path).getOrThrow()
        assertEquals(2, deleted.levels.size)
        assertFalse(deleted.levels.any { it.id == id })
        assertEquals(deleted.levels.first().id, deleted.selectedId)
    }

    @Test
    fun corruptDataIsReportedAndNotOverwritten() {
        path.writeText("broken save")
        assertTrue(GameStore.open(levels, path).isFailure)
        assertEquals("broken save", path.readText())
    }

    @Test
    fun writingToAnUnavailableDirectoryReturnsAnExplicitFailure() {
        val parentFile = File(directory, "not-a-directory").apply { writeText("occupied") }
        val store = GameStore.open(levels, File(parentFile, "game.json")).getOrThrow()
        assertTrue(store.save().isFailure)
        assertEquals("occupied", parentFile.readText())
    }
}
