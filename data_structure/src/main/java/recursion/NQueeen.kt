package recursion

import java.util.*


fun solveNQueens(n: Int): List<List<String>> {
    val board = Array(n) { CharArray(n){'.'} }
//    for (i in 0 until n) for (j in 0 until n) board[i][j] = '.'
    val res: MutableList<List<String>> = ArrayList()
    dfs(0, board, res)
    return res
}

fun validate(board: Array<CharArray>, rowArg: Int, colArg: Int): Boolean {
    var row = rowArg
    var col = colArg
    while (row >= 0 && col >= 0) {
        if (board[row][col] == 'Q') return false
        row--
        col--
    }
    row = rowArg
    col = colArg
    while (col >= 0) {
        if (board[row][col] == 'Q') return false
        col--
    }
    row = rowArg
    col = colArg
    while (col >= 0 && row < board.size) {
        if (board[row][col] == 'Q') return false
        col--
        row++
    }
    return true
}

fun dfs(col: Int, board: Array<CharArray>, res: MutableList<List<String>>) {
    if (col == board.size) {
        res.add(construct(board))
        return
    }
    for (row in board.indices) {
        if (validate(board, row, col)) {
            board[row][col] = 'Q'
            dfs(col + 1, board, res)
            board[row][col] = '.'
        }
    }
}

fun construct(board: Array<CharArray>): List<String> {
    val res: MutableList<String> = LinkedList()
    for (i in board.indices) {
        val s = String(board[i])
        res.add(s)
    }
    return res
}


fun main() {
    val N = 4
    val queen = solveNQueens(N)
    var i = 1
    for (it in queen) {
        println("Arrangement $i")
        for (s in it) {
            println(s)
        }
        println()
        i += 1
    }
}
