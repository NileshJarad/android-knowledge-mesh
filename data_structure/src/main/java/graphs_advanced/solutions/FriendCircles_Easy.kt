/**
 * LeetCode 547: Number of Provinces
 * Difficulty: Easy
 * Pattern: Connected Components in Undirected Graph
 */

class Solution {
    /**
     * Count the number of provinces (connected components) in a country.
     * n cities where ith city and cities connected by friendships form a province.
     *
     * Time: O(N + E) using DFS/BFS or O(N × α(N)) using Union-Find
     * Space: O(N + E) for adjacency list
     */
    fun findCircleNum(friendship: Array<IntArray>): Int {
        val n = friendship.size
        val adj = Array(n) { mutableListOf<Int>() }

        // Build adjacency list
        for (i in 0 until n) {
            for (j in 0 until n) {
                if (friendship[i][j] == 1 && i != j) {
                    adj[i].add(j)
                }
            }
        }

        val visited = BooleanArray(n)
        var provinces = 0

        for (i in 0 until n) {
            if (!visited[i]) {
                dfs(i, adj, visited)
                provinces++
            }
        }

        return provinces
    }

    private fun dfs(node: Int, adj: Array<MutableList<Int>>, visited: BooleanArray) {
        visited[node] = true
        for (neighbor in adj[node]) {
            if (!visited[neighbor]) {
                dfs(neighbor, adj, visited)
            }
        }
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Given an n x n matrix where matrix[i][j] = 1 if city i and city j are directly connected,
 * find the number of provinces (connected components).
 *
 * EXAMPLE 1:
 * Input: friendship = [[1,1,0],[1,1,0],[0,0,1]]
 * Output: 2
 * Explanation: Cities 0 and 1 are connected (form a province). City 2 is alone (another province).
 *
 * EXAMPLE 2:
 * Input: friendship = [[1,0,0],[0,1,0],[0,0,1]]
 * Output: 3
 * Explanation: No connections, each city is its own province.
 *
 * BRUTE FORCE:
 * - For each city, try all possible connections recursively
 * - Time: O(N! × N²) - completely infeasible
 *
 * BETTER (DFS/BFS):
 * - Build adjacency list from matrix
 * - Run DFS/BFS from each unvisited city
 * - Count connected components
 * - Time: O(N²) (matrix building) + O(N + E) (traversal)
 *
 * OPTIMAL (Union-Find):
 * - Process all friendship connections
 * - Union connected cities
 * - Count unique roots
 * - Time: O(N² × α(N)) for matrix processing
 *
 * KEY INSIGHT: This is essentially counting connected components in an undirected graph.
 * Each '1' in the matrix represents an edge between two cities.
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val friendship1 = arrayOf(
        intArrayOf(1, 1, 0),
        intArrayOf(1, 1, 0),
        intArrayOf(0, 0, 1)
    )
    println("Test 1:")
    println("  friendship matrix:")
    friendship1.forEach { row -> println("    ${row.contentToString()}") }
    println("  Provinces: ${solution.findCircleNum(friendship1)}")  // Expected: 2

    // Test case 2
    val friendship2 = arrayOf(
        intArrayOf(1, 0, 0),
        intArrayOf(0, 1, 0),
        intArrayOf(0, 0, 1)
    )
    println("\nTest 2:")
    println("  friendship matrix: all diagonal 1s")
    println("  Provinces: ${solution.findCircleNum(friendship2)}")  // Expected: 3

    // Test case 3 - larger example
    val friendship3 = arrayOf(
        intArrayOf(1, 1, 0, 1),
        intArrayOf(1, 1, 0, 0),
        intArrayOf(0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1)
    )
    println("\nTest 3:")
    println("  friendship matrix:")
    friendship3.forEach { row -> println("    ${row.contentToString()}") }
    println("  Provinces: ${solution.findCircleNum(friendship3)}")  // Expected: 1
}