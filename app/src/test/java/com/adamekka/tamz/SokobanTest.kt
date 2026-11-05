package com.adamekka.tamz

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SokobanTest {
    @Test
    fun wallsAndTwoBoxesBlockMovementWithoutCountingMoves() {
        val level = Level("Sklad", listOf("########", "#@$$ ..#", "#      #", "########"))
        assertEquals(level.initial, level.move(level.initial, Direction.Up))
        assertEquals(level.initial, level.move(level.initial, Direction.Right))
        val down = level.move(level.initial, Direction.Down)
        assertEquals(Cell(1, 2), down.player)
        assertEquals(1, down.moves)
        assertEquals(level.initial.boxes, down.boxes)
    }

    @Test
    fun pushesOneBoxAndDoesNotPullItWhenWalkingAway() {
        val level = Level("Bedna", listOf("#######", "#@ $ .#", "#     #", "#######"))
        val walked = level.move(level.initial, Direction.Right)
        val pushed = level.move(walked, Direction.Right)
        assertEquals(setOf(Cell(4, 1)), pushed.boxes)
        assertEquals(2, pushed.moves)
        val away = level.move(pushed, Direction.Left)
        assertEquals(pushed.boxes, away.boxes)
        assertEquals(Cell(2, 1), away.player)
        val won = level.move(level.move(away, Direction.Right), Direction.Right)
        assertEquals(level.goals, won.boxes)
        assertEquals('*', level.render(won)[1][5])
        assertEquals(won, level.move(won, Direction.Left))
    }

    @Test
    fun parserSupportsSeveralRaggedMapsTitlesAndCombinedGoalSymbols() {
        val levels = parseLevels("\uFEFF; První\r\n#####\r\n#@$.#\r\n#####\r\n\r\n; Druhá\r\n  #####\r\n### + #\r\n# $ * #\r\n#     #\r\n#######\r\n").getOrThrow()
        assertEquals(listOf("První", "Druhá"), levels.map { it.name })
        assertEquals(listOf(5, 7), levels.map { it.width })
        assertFalse(Cell(0, 0) in levels[1].floor)
        assertEquals(2, levels[1].goals.size)
        assertEquals('+', levels[1].rows[1][4])
        assertEquals('*', levels[1].rows[2][4])
    }

    @Test
    fun malformedLevelsProduceExplicitFailures() {
        listOf(
            "",
            "#####\n# $.#\n#####",
            "######\n#@@$.#\n######",
            "#####\n#@$. \n#####",
            "######\n#@$$.#\n######",
            "#####\n#@x.#\n#####",
            "#######\n#@# $.#\n#######",
            "#####\n#@* #\n#####"
        ).forEach { text -> assertTrue(text, parseLevels(text).isFailure) }
    }

    @Test
    fun restoringRejectsStatesOutsideTheBoardOrWithMissingBoxes() {
        val level = Level("Bedna", listOf("#####", "#@$.#", "#####"))
        assertTrue(runCatching { level.restore(Cell(0, 0), level.initial.boxes, 2) }.isFailure)
        assertTrue(runCatching { level.restore(level.initial.player, emptySet(), 2) }.isFailure)
        assertTrue(runCatching { level.restore(level.initial.player, level.initial.boxes, -1) }.isFailure)
    }

    @Test
    fun editorPreservesGoalsAndKeepsExactlyOnePlayerWhenMovingIt() {
        var draft = EditorDraft.empty().paint(Cell(2, 2), EditorTool.Goal)
        draft = draft.paint(Cell(2, 2), EditorTool.Player)
        assertEquals('+', draft.rows[2][2])
        draft = draft.paint(Cell(3, 2), EditorTool.Player)
        assertEquals('.', draft.rows[2][2])
        assertEquals('@', draft.rows[2][3])
        draft = draft.paint(Cell(2, 2), EditorTool.Box)
        assertEquals('*', draft.rows[2][2])
        draft = draft.paint(Cell(2, 2), EditorTool.Floor)
        assertEquals('-', draft.rows[2][2])
    }

    @Test
    fun everyBundledLevelCanBeSolved() {
        val levels = parseLevels(File("src/main/assets/levels.txt").readText()).getOrThrow()
        assertEquals(8, levels.size)
        levels.forEach { level ->
            val queue = ArrayDeque<GameState>()
            val visited = mutableSetOf(level.initial.player to level.initial.boxes)
            queue.add(level.initial)
            var solved = false
            while (queue.isNotEmpty()) {
                val state = queue.removeFirst()
                if (state.boxes == level.goals) { solved = true; break }
                Direction.entries.forEach { direction ->
                    val next = level.move(state, direction)
                    if (visited.add(next.player to next.boxes)) queue.add(next)
                }
            }
            assertTrue("Úroveň ${level.name} musí být řešitelná.", solved)
        }
    }
}
