/**
 * LeetCode 1293: Shortest Path in a Grid with Obstacles Elimination
 * Difficulty: Hard
 * Pattern: BFS + Trie for state tracking (optional optimization)
 *
 * Note: This problem is typically solved with BFS + visited state tracking.
 * The Trie pattern mentioned in the teaching material is an advanced
 * optimization for state deduplication across multiple queries.
 */

import java.util.*

class Solution {
    /**
     * Finds shortest path from top-left to bottom-right in grid
     * where we can eliminate at most k obstacles.
     *
     * Time: O(m × n × k) where m, n = grid dimensions, k = max eliminations
     * Space: O(m × n × k) for visited states
     */
    fun shortestPath(grid: Array<IntArray>, k: Int): Int {
        val m = grid.size
        val n = grid[0].size

        // Quick check: if k >= (m-1 + n-1), we can just go straight
        if (k >= m + n - 2) {
            return m + n - 2
        }

        // Directions: right, down, left, up
        val dirs = arrayOf(intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(-1, 0))

        // Visited states: [row][col][remaining k] = visited
        // Using 3D boolean array for O(1) lookup
        val visited = Array(m) { Array(n) { BooleanArray(k + 1) } }

        // BFS queue: (row, col, remaining eliminations)
        val queue: ArrayDeque<Triple<Int, Int, Int>> = ArrayDeque()
        queue.add(Triple(0, 0, k))
        visited[0][0][k] = true

        var steps = 0

        while (queue.isNotEmpty()) {
            val size = queue.size
            repeat(size) {
                val (r, c, remK) = queue.removeFirst()

                // Check if reached destination
                if (r == m - 1 && c == n - 1) {
                    return steps
                }

                // Try all 4 directions
                for (dir in dirs) {
                    val nr = r + dir[0]
                    val nc = c + dir[1]

                    // Check bounds
                    if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue

                    // Calculate new remaining eliminations
                    val newRemK = remK - grid[nr][nc]

                    // If obstacle and no eliminations left, skip
                    if (newRemK < 0) continue

                    // If not visited with this remaining k, add to queue
                    if (!visited[nr][nc][newRemK]) {
                        visited[nr][nc][newRemK] = true
                        queue.add(Triple(nr, nc, newRemK))
                    }
                }
            }
            steps++
        }

        return -1
    }
}

/**
 * TRIE-BASED STATE OPTIMIZATION (Advanced):
 *
 * For multiple queries on the same grid, we can build a Trie where:
 * - Each level represents a step in the path
 * - Nodes represent (row, col, remaining k) states
 * - This allows sharing common path prefixes across queries
 *
 * However, for single query, BFS with 3D visited array is optimal.
 * The Trie approach shines when:
 * 1. Multiple queries on same grid
 * 2. Need to find ALL paths or count paths
 * 3. Path reconstruction needed
 */

class TrieStateOptimizer {
    /**
     * Trie node representing a grid state
     */
    data class StateNode(
        val row: Int,
        val col: Int,
        val remainingK: Int,
        val children: MutableMap<Char, StateNode> = mutableMapOf(),
        var isEnd: Boolean = false
    )

    private val root = StateNode(-1, -1, -1)

    /**
     * Inserts a path into the state trie
     * Path encoded as string of moves: "RDLU" etc.
     */
    fun insertPath(path: String, states: List<Triple<Int, Int, Int>>) {
        var current = root
        for (i in path.indices) {
            val move = path[i]
            val (r, c, remK) = states[i]
            current = current.children.getOrPut(move) {
                StateNode(r, c, remK)
            }
        }
        current.isEnd = true
    }

    /**
     * Checks if a state sequence exists in the trie
     */
    fun hasPath(path: String): Boolean {
        var current = root
        for (move in path) {
            current = current.children[move] ?: return false
        }
        return current.isEnd
    }
}

/**
 * ALTERNATIVE SOLUTION: A* with Trie-based heuristic
 *
 * If we need to find shortest path with heuristic, we can use A*
 * with Trie storing optimal paths from different start positions.
 * This is overkill for the standard problem but useful for variants.
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val grid1 = arrayOf(
        intArrayOf(0, 0, 0),
        intArrayOf(1, 1, 0),
        intArrayOf(0, 0, 0),
        intArrayOf(0, 1, 1),
        intArrayOf(0, 0, 0)
    )
    val k1 = 1
    println("Test 1:")
    println("Grid: ${grid1.joinToString("\n      ") { it.contentToString() }}")
    println("k = $k1")
    println("Shortest path: ${solution.shortestPath(grid1, k1)}")  // Expected: 6

    // Test case 2
    val grid2 = arrayOf(
        intArrayOf(0, 1, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 0)
    )
    val k2 = 1
    println("\nTest 2:")
    println("Grid: ${grid2.joinToString("\n      ") { it.contentToString() }}")
    println("k = $k2")
    println("Shortest path: ${solution.shortestPath(grid2, k2)}")  // Expected: -1

    // Test case 3 - large k (straight path)
    val grid3 = arrayOf(
        intArrayOf(0, 1, 0),
        intArrayOf(1, 1, 0),
        intArrayOf(0, 0, 0)
    )
    val k3 = 10
    println("\nTest 3:")
    println("Grid: ${grid3.joinToString("\n      ") { it.contentToString() }}")
    println("k = $k3")
    println("Shortest path: ${solution.shortestPath(grid3, k3)}")  // Expected: 4
}