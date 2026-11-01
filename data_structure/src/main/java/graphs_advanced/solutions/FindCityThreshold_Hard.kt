/**
 * LeetCode 1334: Find the City With the Smallest Number of Neighbors at a Threshold Distance
 * Difficulty: Hard
 * Pattern: All-Pairs Shortest Paths (Floyd-Warshall)
 */

class Solution {
    /**
     * Find the city with the smallest number of reachable cities within threshold distance.
     * If multiple cities have the same number, return the largest city number.
     *
     * Time: O(N³) using Floyd-Warshall
     * Space: O(N²) for distance matrix
     */
    fun findTheCity(n: Int, edges: Array<IntArray>, distanceThreshold: Int): Int {
        // Initialize distance matrix
        val dist = Array(n) { IntArray(n) { Int.MAX_VALUE } }

        // Distance to self is 0
        for (i in 0 until n) {
            dist[i][i] = 0
        }

        // Add direct edges
        for (edge in edges) {
            val u = edge[0]
            val v = edge[1]
            val w = edge[2]
            dist[u][v] = w
            dist[v][u] = w // undirected
        }

        // Floyd-Warshall algorithm
        for (k in 0 until n) {
            for (i in 0 until n) {
                if (dist[i][k] == Int.MAX_VALUE) continue
                for (j in 0 until n) {
                    if (dist[k][j] == Int.MAX_VALUE) continue
                    if (dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j]
                    }
                }
            }
        }

        // Count reachable cities within threshold for each city
        var minCount = n
        var resultCity = -1

        for (i in 0 until n) {
            var count = 0
            for (j in 0 until n) {
                if (i != j && dist[i][j] <= distanceThreshold) {
                    count++
                }
            }

            // If this city has fewer or equal neighbors, update result
            // We want smallest count, and largest city number on tie
            if (count <= minCount) {
                minCount = count
                resultCity = i
            }
        }

        return resultCity
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Find city with smallest number of reachable cities within threshold distance.
 *
 * EXAMPLE 1:
 * Input: n = 4, edges = [[0,1,3],[1,2,1],[1,3,4],[2,3,1]], distanceThreshold = 2
 * Output: 3
 * Explanation: City 3 can reach 0 cities within threshold 2.
 *
 * EXAMPLE 2:
 * Input: n = 4, edges = [[0,1,2],[0,2,1],[1,2,1],[2,3,1]], distanceThreshold = 1
 * Output: 0
 * Explanation: City 0 can reach 0 cities within threshold 1.
 *
 * BRUTE FORCE:
 * - For each city, run BFS/DFS to find all reachable cities
 * - Time: O(N × (N + E)) - too slow for large N
 *
 * BETTER (Dijkstra from each city):
 * - Run Dijkstra from each city to find distances
 * - Time: O(N × (E log V)) - better but still slow
 *
 * OPTIMAL (Floyd-Warshall):
 * - Compute all-pairs shortest paths in O(N³)
 * - Then count reachable cities for each city in O(N²)
 * - Total: O(N³) - good for N ≤ 100
 *
 * KEY INSIGHT: Floyd-Warshall is perfect for all-pairs shortest paths.
 * It works by considering each vertex as intermediate point between all pairs.
 * After computing all distances, count cities within threshold for each city.
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val n1 = 4
    val edges1 = arrayOf(
        intArrayOf(0, 1, 3),
        intArrayOf(1, 2, 1),
        intArrayOf(1, 3, 4),
        intArrayOf(2, 3, 1)
    )
    val threshold1 = 2
    println("Test 1:")
    println("  n = $n1, edges = ${edges1.map { it.contentToString() }.toList()}")
    println("  threshold = $threshold1")
    println("  Result: ${solution.findTheCity(n1, edges1, threshold1)}")  // Expected: 3

    // Test case 2
    val n2 = 4
    val edges2 = arrayOf(
        intArrayOf(0, 1, 2),
        intArrayOf(0, 2, 1),
        intArrayOf(1, 2, 1),
        intArrayOf(2, 3, 1)
    )
    val threshold2 = 1
    println("\nTest 2:")
    println("  n = $n2, edges = ${edges2.map { it.contentToString() }.toList()}")
    println("  threshold = $threshold2")
    println("  Result: ${solution.findTheCity(n2, edges2, threshold2)}")  // Expected: 0

    // Test case 3 - single city
    val n3 = 1
    val edges3 = arrayOf<IntArray>()
    val threshold3 = 10
    println("\nTest 3:")
    println("  n = $n3, edges = []")
    println("  threshold = $threshold3")
    println("  Result: ${solution.findTheCity(n3, edges3, threshold3)}")  // Expected: 0
}