/**
 * LeetCode 1971: Find if Path Exists in Graph
 * Difficulty: Easy
 * Pattern: BFS/DFS connectivity check
 */

class Solution {
    /**
     * Given a bi-directional graph with n vertices and edges list,
     * determine if there is a valid path from source to destination.
     *
     * Time: O(N + E) where N = vertices, E = edges
     * Space: O(N + E) for adjacency list + visited set
     */
    fun validPath(n: Int, edges: Array<IntArray>, source: Int, destination: Int): Boolean {
        // Edge case: source equals destination
        if (source == destination) return true

        // Build adjacency list
        val adj = Array(n) { mutableListOf<Int>() }
        for (edge in edges) {
            val u = edge[0]
            val v = edge[1]
            adj[u].add(v)
            adj[v].add(u) // bi-directional
        }

        // BFS from source
        val visited = BooleanArray(n)
        val queue = java.util.ArrayDeque<Int>()
        visited[source] = true
        queue.add(source)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node == destination) return true

            for (neighbor in adj[node]) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true
                    queue.add(neighbor)
                }
            }
        }

        return false
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Determine if there's a valid path from source to destination in an undirected graph.
 *
 * EXAMPLE 1:
 * Input: n = 3, edges = [[0,1],[1,2],[2,0]], source = 0, destination = 2
 * Output: true
 * Explanation: Path 0 -> 1 -> 2 or 0 -> 2 exists.
 *
 * EXAMPLE 2:
 * Input: n = 6, edges = [[0,1],[0,2],[3,5],[5,4],[4,3]], source = 0, destination = 5
 * Output: false
 * Explanation: Nodes 0,1,2 are in one component; nodes 3,4,5 are in another.
 *
 * BRUTE FORCE:
 * - Try all possible paths from source using backtracking
 * - Time: O(V!) in worst case - exponential
 * - Not feasible for large graphs
 *
 * BETTER (DFS Recursive):
 * - Recursively explore all neighbors
 * - Time: O(N + E), Space: O(N) for recursion stack
 * - Risk of stack overflow for deep graphs
 *
 * OPTIMAL (BFS Iterative):
 * - Use queue for level-by-level exploration
 * - Time: O(N + E), Space: O(N + E)
 * - No stack overflow risk, finds shortest path too
 *
 * KEY INSIGHT: BFS/DFS naturally handles disconnected graphs — if destination
 * is not reachable from source, BFS will exhaust the component and return false.
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val n1 = 3
    val edges1 = arrayOf(intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 0))
    println("Test 1:")
    println("  n = $n1, edges = ${edges1.map { it.contentToString() }.toList()}")
    println("  source = 0, destination = 2")
    println("  Path exists: ${solution.validPath(n1, edges1, 0, 2)}")  // Expected: true

    // Test case 2
    val n2 = 6
    val edges2 = arrayOf(
        intArrayOf(0, 1), intArrayOf(0, 2),
        intArrayOf(3, 5), intArrayOf(5, 4), intArrayOf(4, 3)
    )
    println("\nTest 2:")
    println("  n = $n2, edges = ${edges2.map { it.contentToString() }.toList()}")
    println("  source = 0, destination = 5")
    println("  Path exists: ${solution.validPath(n2, edges2, 0, 5)}")  // Expected: false

    // Test case 3 - source equals destination
    val n3 = 5
    val edges3 = arrayOf(intArrayOf(0, 1), intArrayOf(1, 2))
    println("\nTest 3:")
    println("  n = $n3, edges = ${edges3.map { it.contentToString() }.toList()}")
    println("  source = 0, destination = 0")
    println("  Path exists: ${solution.validPath(n3, edges3, 0, 0)}")  // Expected: true

    // Test case 4 - no edges
    val n4 = 4
    val edges4 = arrayOf<IntArray>()
    println("\nTest 4:")
    println("  n = $n4, edges = []")
    println("  source = 0, destination = 3")
    println("  Path exists: ${solution.validPath(n4, edges4, 0, 3)}")  // Expected: false
}