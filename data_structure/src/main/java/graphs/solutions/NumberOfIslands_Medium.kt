/**
 * LeetCode 200: Number of Islands
 * Difficulty: Medium
 * Pattern: Connected Components on Grid (BFS/DFS)
 */

class Solution {
    /**
     * Given an m x n grid of '1's (land) and '0's (water),
     * count the number of islands. An island is surrounded by water
     * and is formed by connecting adjacent lands horizontally or vertically.
     *
     * Time: O(M × N) where M = rows, N = cols
     * Space: O(M × N) for visited set (or modify grid in-place)
     */
    fun numIslands(grid: Array<CharArray>): Int {
        if (grid.isEmpty()) return 0

        val m = grid.size
        val n = grid[0].size
        var islands = 0

        // Directions: up, down, left, right
        val dirs = arrayOf(
            intArrayOf(-1, 0), intArrayOf(1, 0),
            intArrayOf(0, -1), intArrayOf(0, 1)
        )

        for (i in 0 until m) {
            for (j in 0 until n) {
                if (grid[i][j] == '1') {
                    islands++
                    // BFS to mark all connected land as visited (sink the island)
                    bfs(grid, i, j, m, n, dirs)
                }
            }
        }

        return islands
    }

    private fun bfs(
        grid: Array<CharArray>,
        startRow: Int,
        startCol: Int,
        m: Int,
        n: Int,
        dirs: Array<IntArray>
    ) {
        val queue = java.util.ArrayDeque<Pair<Int, Int>>()
        grid[startRow][startCol] = '0' // mark as visited by sinking
        queue.add(Pair(startRow, startCol))

        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()

            for (dir in dirs) {
                val nr = r + dir[0]
                val nc = c + dir[1]

                // Check bounds and if it's land
                if (nr >= 0 && nr < m && nc >= 0 && nc < n && grid[nr][nc] == '1') {
                    grid[nr][nc] = '0' // mark visited
                    queue.add(Pair(nr, nc))
                }
            }
        }
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Count number of islands in a 2D grid where '1' is land and '0' is water.
 *
 * EXAMPLE:
 * Input:
 * grid = [
 *   ["1","1","1","1","0"],
 *   ["1","1","0","1","0"],
 *   ["1","1","0","0","0"],
 *   ["0","0","0","0","0"]
 * ]
 * Output: 1
 *
 * EXAMPLE 2:
 * Input:
 * grid = [
 *   ["1","1","0","0","0"],
 *   ["1","1","0","0","0"],
 *   ["0","0","1","0","0"],
 *   ["0","0","0","1","1"]
 * ]
 * Output: 3
 *
 * BRUTE FORCE:
 * - For each '1' cell, try all possible paths to find connected component
 * - Time: O((M×N)²) - very inefficient
 *
 * BETTER (DFS Recursive):
 * - For each unvisited '1', DFS to mark all connected '1's
 * - Time: O(M × N), Space: O(M × N) recursion stack
 * - Risk of stack overflow on large grids
 *
 * OPTIMAL (BFS Iterative + In-place modification):
 * - For each unvisited '1', BFS to mark all connected '1's as '0' (sink island)
 * - Modify grid in-place to avoid extra visited set
 * - Time: O(M × N), Space: O(min(M, N)) for queue
 *
 * KEY INSIGHT: This is Connected Components on a grid. Each '1' cell is a node,
 * adjacent '1's are edges. We count connected components of '1's.
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val grid1 = arrayOf(
        charArrayOf('1', '1', '1', '1', '0'),
        charArrayOf('1', '1', '0', '1', '0'),
        charArrayOf('1', '1', '0', '0', '0'),
        charArrayOf('0', '0', '0', '0', '0')
    )
    println("Test 1:")
    println("  Grid:")
    grid1.forEach { println("    ${it.contentToString()}") }
    println("  Islands: ${solution.numIslands(grid1)}")  // Expected: 1

    // Test case 2
    val grid2 = arrayOf(
        charArrayOf('1', '1', '0', '0', '0'),
        charArrayOf('1', '1', '0', '0', '0'),
        charArrayOf('0', '0', '1', '0', '0'),
        charArrayOf('0', '0', '0', '1', '1')
    )
    println("\nTest 2:")
    println("  Grid:")
    grid2.forEach { println("    ${it.contentToString()}") }
    println("  Islands: ${solution.numIslands(grid2)}")  // Expected: 3

    // Test case 3 - empty grid
    val grid3 = arrayOf<CharArray>()
    println("\nTest 3:")
    println("  Grid: []")
    println("  Islands: ${solution.numIslands(grid3)}")  // Expected: 0

    // Test case 4 - all water
    val grid4 = arrayOf(
        charArrayOf('0', '0'),
        charArrayOf('0', '0')
    )
    println("\nTest 4:")
    println("  Grid: all water")
    println("  Islands: ${solution.numIslands(grid4)}")  // Expected: 0
}