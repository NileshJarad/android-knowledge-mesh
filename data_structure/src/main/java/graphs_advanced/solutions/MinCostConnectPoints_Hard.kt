/**
 * LeetCode 1584: Min Cost to Connect All Points
 * Difficulty: Hard
 * Pattern: MST with Euclidean Distance Calculation
 */

class Solution {
    /**
     * You are given an array points representing integer coordinates of points in a 2D plane.
     * Cost to connect two points = Manhattan distance: |xi - xj| + |yi - yj|.
     *
     * Return the minimum cost to make all points connected. All points are connected
     * if and only if there is exactly one simple path between any two points.
     *
     * Time: O(N²) using Prim's with array (dense graph)
     *       O(N² log N) using Prim's with heap
     *       O(N² log N) using Kruskal's with sorting
     * Space: O(N²) for all edges, or O(N) for optimized Prim's
     */
    fun minCostConnectPoints(points: Array<IntArray>): Int {
        val n = points.size
        if (n <= 1) return 0

        // Prim's Algorithm - optimized for dense graph using array
        // This is O(N²) instead of O(N² log N) with heap
        val minDist = IntArray(n) { Int.MAX_VALUE }
        val visited = BooleanArray(n)
        minDist[0] = 0

        var totalCost = 0

        for (i in 0 until n) {
            // Find unvisited vertex with minimum distance
            var u = -1
            var minVal = Int.MAX_VALUE
            for (j in 0 until n) {
                if (!visited[j] && minDist[j] < minVal) {
                    minVal = minDist[j]
                    u = j
                }
            }

            if (u == -1) break // no more reachable vertices

            visited[u] = true
            totalCost += minVal

            // Update distances to neighbors
            for (v in 0 until n) {
                if (!visited[v]) {
                    val cost = manhattan(points[u], points[v])
                    if (cost < minDist[v]) {
                        minDist[v] = cost
                    }
                }
            }
        }

        return totalCost
    }

    private fun manhattan(a: IntArray, b: IntArray): Int {
        return abs(a[0] - b[0]) + abs(a[1] - b[1])
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Connect all points with minimum cost where cost = Manhattan distance.
 *
 * EXAMPLE 1:
 * Input: points = [[0,0],[2,2],[3,10],[5,2],[7,0]]
 * Output: 20
 * Explanation: MST edges with total weight 20.
 *
 * EXAMPLE 2:
 * Input: points = [[3,12],[-2,5],[-4,1]]
 * Output: 18
 * Explanation: Connect -4,1 to -2,5 (cost 4), then -2,5 to 3,12 (cost 14).
 *
 * BRUTE FORCE:
 * - Try all possible spanning trees
 * - Number of spanning trees = N^(N-2) by Cayley's formula
 * - Time: O(N^N) - completely infeasible
 *
 * BETTER (Kruskal):
 * - Generate all O(N²) edges
 * - Sort edges by weight
 * - Union-Find to add edges
 * - Time: O(N² log N²) = O(N² log N)
 *
 * OPTIMAL (Prim's for dense graphs):
 * - Since complete graph (every point connected to every other),
 *   use Prim's algorithm with array instead of heap
 * - Array-based Prim's is O(N²) - faster than O(N² log N)
 * - Keep track of minimum distance from MST to each unvisited vertex
 * - Time: O(N²), Space: O(N)
 *
 * KEY INSIGHT: Complete graph with N vertices has N(N-1)/2 edges.
 * For dense graphs, Prim's with array (O(N²)) beats Kruskal (O(N² log N)).
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val points1 = arrayOf(
        intArrayOf(0, 0),
        intArrayOf(2, 2),
        intArrayOf(3, 10),
        intArrayOf(5, 2),
        intArrayOf(7, 0)
    )
    println("Test 1:")
    println("  points = ${points1.map { it.contentToString() }.toList()}")
    println("  Min cost: ${solution.minCostConnectPoints(points1)}")  // Expected: 20

    // Test case 2
    val points2 = arrayOf(
        intArrayOf(3, 12),
        intArrayOf(-2, 5),
        intArrayOf(-4, 1)
    )
    println("\nTest 2:")
    println("  points = ${points2.map { it.contentToString() }.toList()}")
    println("  Min cost: ${solution.minCostConnectPoints(points2)}")  // Expected: 18

    // Test case 3 - two points
    val points3 = arrayOf(intArrayOf(0, 0), intArrayOf(1, 1))
    println("\nTest 3:")
    println("  points = ${points3.map { it.contentToString() }.toList()}")
    println("  Min cost: ${solution.minCostConnectPoints(points3)}")  // Expected: 2
}