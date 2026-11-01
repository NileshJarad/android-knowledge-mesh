/**
 * LeetCode 684: Redundant Connection
 * Difficulty: Easy
 * Pattern: Cycle Detection using Union-Find
 */

class Solution {
    /**
     * In this problem, a tree is an undirected graph that is connected and has no cycles.
     * The given input is a graph that started as a tree with n nodes labeled from 1 to n,
     * with one additional edge added. The added edge has two different vertices chosen
     * from 1 to n, and was not an edge that already existed.
     *
     * Return an edge that can be removed so that the resulting graph is a tree of n nodes.
     * If there are multiple answers, return the answer that occurs last in the input.
     *
     * Time: O(N × α(N)) where N = number of edges
     * Space: O(N) for Union-Find
     */
    fun findRedundantConnection(edges: Array<IntArray>): IntArray {
        val n = edges.size
        val uf = UnionFind(n + 1) // 1-indexed

        for (edge in edges) {
            val u = edge[0]
            val v = edge[1]
            // If u and v are already in same component, this edge creates cycle
            if (!uf.union(u, v)) {
                return edge
            }
        }

        return intArrayOf()
    }
}

class UnionFind(var n: Int) {
    val parent = IntArray(n) { it }
    val rank = IntArray(n) { 0 }

    fun find(x: Int): Int {
        if (parent[x] != x) {
            parent[x] = find(parent[x]) // path compression
        }
        return parent[x]
    }

    fun union(x: Int, y: Int): Boolean {
        val rx = find(x)
        val ry = find(y)
        if (rx == ry) return false // already connected - would create cycle

        if (rank[rx] < rank[ry]) {
            parent[rx] = ry
        } else if (rank[rx] > rank[ry]) {
            parent[ry] = rx
        } else {
            parent[ry] = rx
            rank[rx]++
        }
        return true
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Find the redundant edge in a tree + 1 extra edge.
 *
 * EXAMPLE 1:
 * Input: edges = [[1,2],[1,3],[2,3]]
 * Output: [2,3]
 * Explanation: Edge [2,3] creates cycle 1-2-3-1.
 *
 * EXAMPLE 2:
 * Input: edges = [[1,2],[2,3],[3,4],[1,4],[1,5]]
 * Output: [1,4]
 * Explanation: Edge [1,4] creates cycle 1-2-3-4-1.
 *
 * BRUTE FORCE:
 * - For each edge, temporarily remove it and check if remaining graph is a tree
 * - Tree check: connected + no cycles
 * - Time: O(N²) or O(N³) - too slow
 *
 * BETTER (DFS Cycle Detection):
 * - Build graph incrementally
 * - For each edge, check if adding it creates a cycle
 * - Time: O(N²)
 *
 * OPTIMAL (Union-Find):
 * - Process edges one by one
 * - For each edge, try to union its endpoints
 * - If already in same set, this edge creates a cycle
 * - Time: O(N × α(N)) ≈ O(N)
 * - Space: O(N)
 *
 * KEY INSIGHT: Union-Find is perfect for detecting cycles in undirected graphs.
 * When we try to union two nodes that already have the same root,
 * adding this edge would create a cycle.
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val edges1 = arrayOf(intArrayOf(1, 2), intArrayOf(1, 3), intArrayOf(2, 3))
    println("Test 1:")
    println("  edges = ${edges1.map { it.contentToString() }.toList()}")
    println("  Redundant: ${solution.findRedundantConnection(edges1).contentToString()}")  // Expected: [2, 3]

    // Test case 2
    val edges2 = arrayOf(
        intArrayOf(1, 2),
        intArrayOf(2, 3),
        intArrayOf(3, 4),
        intArrayOf(1, 4),
        intArrayOf(1, 5)
    )
    println("\nTest 2:")
    println("  edges = ${edges2.map { it.contentToString() }.toList()}")
    println("  Redundant: ${solution.findRedundantConnection(edges2).contentToString()}")  // Expected: [1, 4]

    // Test case 3
    val edges3 = arrayOf(intArrayOf(1, 2), intArrayOf(1, 3), intArrayOf(1, 4))
    println("\nTest 3:")
    println("  edges = ${edges3.map { it.contentToString() }.toList()}")
    println("  Redundant: ${solution.findRedundantConnection(edges3).contentToString()}")  // Expected: [] (no cycle, but input guarantees one)
}