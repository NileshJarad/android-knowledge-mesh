package recursion_backtracking

/**
 * LeetCode: https://leetcode.com/problems/n-queens-ii/
 * Difficulty: Hard
 * Core pattern: Backtracking with column/diagonal constraint tracking
 */
fun nQueensII(n: Int): Int {
    var count = 0
    val cols = BooleanArray(n)   // Track occupied columns
    val upperDiag = BooleanArray(2 * n - 1)  // row - col + (n-1); range [0, 2n-2]
    val lowerDiag = BooleanArray(2 * n - 1)   // row + col; range [0, 2n-2]

    fun backtrack(row: Int) {
        if (row == n) {
            count++  // Found one complete valid board
            return
        }

        for (col in 0 until n) {
            val colIdx = col
            val upperIdx = row - col + (n - 1)
            val lowerIdx = row + col

            if (!cols[colIdx] && !upperDiag[upperIdx] && !lowerDiag[lowerIdx]) {
                cols[colIdx] = true
                upperDiag[upperIdx] = true
                lowerDiag[lowerIdx] = true

                backtrack(row + 1)  // Move to next row

                cols[colIdx] = false       // Undo column
                upperDiag[upperIdx] = false  // Undo diagonal
                lowerDiag[lowerIdx] = false  // Undo diagonal
            }
        }
    }

    backtrack(0)
    return count
}

// Alternative using sets for cleaner tracking
fun nQueensIISets(n: Int): Int {
    var count = 0
    val cols = mutableSetOf<Int>()
    val diag1 = mutableSetOf<Int>()  // row - col, range [-(n-1), n-1] → shift by n-1 to make indexable
    val diag2 = mutableSetOf<Int>()  // row + col, range [0, 2n-2]

    fun backtrack(row: Int) {
        if (row == n) {
            count++
            return
        }
        for (col in 0 until n) {
            val d1Idx = row - col + (n - 1)
            val d2Idx = row + col
            if (col !in cols && d1Idx !in diag1 && d2Idx !in diag2) {
                cols.add(col)
                diag1.add(d1Idx)
                diag2.add(d2Idx)
                backtrack(row + 1)
                cols.remove(col)
                diag1.remove(d1Idx)
                diag2.remove(d2Idx)
            }
        }
    }

    backtrack(0)
    return count
}
