package com.adamekka.tamz

enum class EditorTool(val title: String, val tile: Char) {
    Wall("Zeď", '#'), Floor("Podlaha", '-'), Goal("Cíl", '.'), Box("Bedna", '$'), Player("Hráč", '@')
}

data class EditorDraft(val target: EditorTarget, val name: String, val rows: List<String>) {
    fun paint(cell: Cell, tool: EditorTool): EditorDraft {
        require(cell.y in rows.indices && cell.x in rows.first().indices)
        val grid = rows.map { it.toCharArray() }
        if (tool == EditorTool.Player) {
            grid.forEach { row ->
                row.indices.forEach { x ->
                    row[x] = when (row[x]) { '@' -> '-'; '+' -> '.'; else -> row[x] }
                }
            }
        }
        val previous = grid[cell.y][cell.x]
        grid[cell.y][cell.x] = when (tool) {
            EditorTool.Goal -> when (previous) { '$', '*' -> '*'; '@', '+' -> '+'; else -> '.' }
            EditorTool.Box -> if (previous in ".+*") '*' else '$'
            EditorTool.Player -> if (previous in ".+*") '+' else '@'
            else -> tool.tile
        }
        return copy(rows = grid.map { it.concatToString() })
    }

    companion object {
        fun empty(): EditorDraft = EditorDraft(EditorTarget.New, "", List(8) { y ->
            CharArray(8) { x -> if (x == 0 || y == 0 || x == 7 || y == 7) '#' else '-' }.concatToString()
        })
    }
}
