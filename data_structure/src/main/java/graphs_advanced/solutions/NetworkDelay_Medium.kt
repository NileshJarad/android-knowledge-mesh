/**
 * LeetCode 743: Network Delay Time
 * Difficulty: Medium
 * Pattern: Shortest Path (Dijkstra) + Global Optimization
 */

class Solution {
    /**
     * There are n network nodes labeled 1 to n. Given travelTimes as travelTimes[i] = [ui, vi, wi],
     * where ui is source node, vi is target node, and wi is time it takes for a signal to travel
     * from source to target.
     *
     * Send signal from node k. Return minimum time for signal to reach all nodes.
     * If impossible, return -1.
     *
     * Time: O(E log V) using Dijkstra with priority queue
     * Space: O(V + E) for adjacency list
     */
    fun networkDelayTime(times: Array<IntArray>, n: Int, k: Int): Int {
        // Build adjacency list
        val adj = Array(n + 1) { mutableListOf<Pair<Int, Int>>() } // (neighbor, time)
        for (t in times) {
            adj[t[0]].add(Pair(t[1], t[2]))
        }

        // Dijkstra's algorithm from node k
        val dist = IntArray(n + 1) { Int.MAX_VALUE }
        val pq = java.util.PriorityQueue<Pair<Int, Int>> { a, b -> a.first - b.first }

        dist[k] = 0
        pq.offer(Pair(0, k))

        while (pq.isNotEmpty()) {
            val (d, u) = pq.poll()
            if (d > dist[u]) continue

            for ((v, w) in adj[u]) {
                val newDist = dist[u] + w
                if (newDist < dist[v]) {
                    dist[v] = newDist
                    pq.offer(Pair(newDist, v))
                }
            }
        }

        // Find maximum distance among all reachable nodes
        var maxTime = 0
        for (i in 1..n) {
            if (dist[i] == Int.MAX_VALUE) {
                return -1 // Not all nodes reachable
            }
            maxTime = maxOf(maxTime, dist[i])
        }

        return maxTime
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Find minimum time for signal to reach all nodes from source k.
 *
 * EXAMPLE 1:
 * Input: times = [[2,1,1],[2,3,1],[3,4,1]], n = 4, k = 2
 * Output: 2
 * Explanation: Signal reaches node 1 at time 1, node 3 at time 1, node 4 at time 2.
 *
 * EXAMPLE 2:
 * Input: times = [[1,2,1]], n = 2, k = 1
 * Output: 1
 * Explanation: Signal reaches node 2 at time 1.
 *
 * EXAMPLE 3:
 * Input: times = [[1,2,1]], n = 2, k = 2
 * Output: -1
 * Explanation: Signal starts at node 2, cannot reach node 1.
 *
 * BRUTE FORCE:
 * - Try all possible paths from source to each node
 * - Time: O(V! × E) - exponential, infeasible
 *
 * BETTER (BFS):
 * - BFS from source, tracking distances
 * - Only works for unweighted graphs
 * - Time: O(V + E) but doesn't handle weighted edges correctly
 *
 * OPTIMAL (Dijkstra):
 * - Use priority queue to always process closest unreached node
 * - Guarantees shortest path with non-negative weights
 * - After Dijkstra, max distance = minimum time for all nodes to receive signal
 * - Time: O(E log V), Space: O(V + E)
 *
 * KEY INSIGHT: This is Dijkstra's algorithm. We find shortest path from source
 * to all nodes, then take the maximum (since we need ALL nodes to receive signal).
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val times1 = arrayOf(intArrayOf(2, 1, 1), intArrayOf(2, 3, 1), intArrayOf(3, 4, 1))
    println("Test 1:")
    println("  times = ${times1.map { it.contentToString() }.toList()}, n = 4, k = 2")
    println("  Result: ${solution.networkDelayTime(times1, 4, 2)}")  // Expected: 2

    // Test case 2
    val times2 = arrayOf(intArrayOf(1, 2, 1))
    println("\nTest 2:")
    println("  times = ${times2.map { it.contentToString() }.toList()}, n = 2, k = 1")
    println("  Result: ${solution.networkDelayTime(times2, 2, 1)}")  // Expected: 1

    // Test case 3 - unreachable
    val times3 = arrayOf(intArrayOf(1, 2, 1))
    println("\nTest 3:")
    println("  times = ${times3.map { it.contentToString() }.toList()}, n = 2, k = 2")
    println("  Result: ${solution.networkDelayTime(times3, 2, 2)}")  // Expected: -1
}