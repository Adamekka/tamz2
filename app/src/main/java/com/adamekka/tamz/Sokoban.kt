package com.adamekka.tamz

data class Cell(val x: Int, val y: Int)

enum class Direction(val dx: Int, val dy: Int) {
    Up(0, -1), Down(0, 1), Left(-1, 0), Right(1, 0)
}

data class GameState(val player: Cell, val boxes: Set<Cell>, val moves: Int) {
    init {
        require(moves >= 0 && player !in boxes) { "Neplatný stav hry." }
    }
}

class Level(val name: String, source: List<String>) {
    val width = source.maxOfOrNull { it.length } ?: 0
    val height = source.size
    val walls: Set<Cell>
    val floor: Set<Cell>
    val goals: Set<Cell>
    val initial: GameState
    val rows: List<String>

    init {
        require(name.isNotBlank()) { "Zadejte název úrovně." }
        require(width in 3..40 && height in 3..40) { "Úroveň musí mít 3 až 40 řádků a sloupců." }
        val padded = source.map { it.padEnd(width) }
        require(padded.all { row -> row.all { it in "# @$.+*-_" } }) {
            "Neznámý znak v úrovni. Použijte #, @, $, ., +, *, mezeru, - nebo _."
        }

        val wallCells = mutableSetOf<Cell>()
        val goalCells = mutableSetOf<Cell>()
        val boxes = mutableSetOf<Cell>()
        val players = mutableListOf<Cell>()
        for (y in 0 until height) for (x in 0 until width) {
            val cell = Cell(x, y)
            val tile = padded[y][x]
            if (tile == '#') wallCells.add(cell)
            if (tile in ".+*") goalCells.add(cell)
            if (tile in "$*") boxes.add(cell)
            if (tile in "@+") players.add(cell)
        }
        require(players.size == 1) { "Umístěte právě jednoho hráče." }
        require(boxes.isNotEmpty() && boxes.size == goalCells.size) {
            "Počet beden a cílů musí být stejný a alespoň jeden."
        }
        // Starting inside the room skips exterior padding in irregular text maps.
        val floorCells = mutableSetOf(players.single())
        val queue = ArrayDeque<Cell>()
        queue.add(players.single())
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            Direction.entries.forEach { direction ->
                val next = Cell(cell.x + direction.dx, cell.y + direction.dy)
                require(next.x in 0 until width && next.y in 0 until height) { "Uzavřete herní plochu zdmi." }
                if (padded[next.y][next.x] != '#' && floorCells.add(next)) queue.add(next)
            }
        }
        require(floorCells.containsAll(boxes + goalCells)) { "Všechny bedny a cíle musí být ve stejné místnosti jako hráč." }
        require(boxes != goalCells) { "Alespoň jedna bedna musí být mimo cíl." }
        walls = wallCells.toSet()
        floor = floorCells.toSet()
        goals = goalCells.toSet()
        initial = GameState(players.single(), boxes.toSet(), 0)
        rows = render(initial)
    }

    private fun validate(state: GameState) {
        require(state.player in floor && state.boxes.size == initial.boxes.size && floor.containsAll(state.boxes)) {
            "Uložená hra neodpovídá úrovni $name."
        }
    }

    fun restore(player: Cell, boxes: Set<Cell>, moves: Int): GameState =
        GameState(player, boxes.toSet(), moves).also { validate(it) }

    fun move(state: GameState, direction: Direction): GameState {
        validate(state)
        // The final completed board stays visible until the player restarts or selects a level.
        if (state.boxes == goals) return state
        val next = Cell(state.player.x + direction.dx, state.player.y + direction.dy)
        if (next !in floor) return state
        if (next !in state.boxes) return state.copy(player = next, moves = state.moves + 1)
        val beyond = Cell(next.x + direction.dx, next.y + direction.dy)
        if (beyond !in floor || beyond in state.boxes) return state
        return GameState(next, state.boxes - next + beyond, state.moves + 1)
    }

    fun render(state: GameState): List<String> {
        validate(state)
        return List(height) { y ->
            CharArray(width) { x ->
                val cell = Cell(x, y)
                when {
                    cell == state.player -> if (cell in goals) '+' else '@'
                    cell in state.boxes -> if (cell in goals) '*' else '$'
                    cell in goals -> '.'
                    cell in walls -> '#'
                    cell in floor -> '-'
                    else -> ' '
                }
            }.concatToString()
        }
    }
}

fun parseLevels(text: String): Result<List<Level>> = runCatching {
    val levels = mutableListOf<Level>()
    val rows = mutableListOf<String>()
    var title = ""
    fun finish() {
        if (rows.isEmpty()) return
        levels.add(Level(title.ifBlank { "Úroveň ${levels.size + 1}" }, rows.toList()))
        rows.clear()
        title = ""
    }
    text.removePrefix("\uFEFF").lineSequence().forEach { line ->
        when {
            line.isBlank() -> finish()
            line.trimStart().startsWith(';') -> {
                finish()
                title = line.trimStart().removePrefix(";").trim()
            }
            else -> rows.add(line)
        }
    }
    finish()
    require(levels.isNotEmpty()) { "Soubor neobsahuje žádnou úroveň." }
    levels
}
