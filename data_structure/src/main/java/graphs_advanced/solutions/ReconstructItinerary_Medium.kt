/**
 * LeetCode 332: Reconstruct Itinerary
 * Difficulty: Medium
 * Pattern: Hierholzer's Algorithm (Eulerian Path in Directed Graph)
 */

import java.util.PriorityQueue

class Solution {
    /**
     * Find the itinerary in order and return it.
     * All of the tickets belong to a man who departs from "JFK".
     * Thus, the itinerary must begin with "JFK". If there are multiple valid itineraries,
     * return the itinerary that has the smallest lexical order when read as a single string.
     *
     * Time: O(E log E) where E = number of tickets (edges)
     * Space: O(E) for adjacency list
     */
    fun findItinerary(tickets: ArrayList<ArrayList<String>>): List<String> {
        // Build adjacency list: JFK -> [LAX, ATL], ATL -> [SFO], LAX -> [ATL], SFO -> [JFK]
        val adj = mutableMapOf<String, PriorityQueue<String>>() // lexical order

        for (ticket in tickets) {
            val from = ticket[0]
            val to = ticket[1]
            if (!adj.containsKey(from)) {
                adj[from] = PriorityQueue() // min-heap for lexical order
            }
            adj[from].offer(to)
        }

        val result = mutableListOf<String>()
        dfs("JFK", adj, result)
        return result.reversed() // post-order traversal gives lexical order
    }

    private fun dfs(airport: String, adj: MutableMap<String, PriorityQueue<String>>, result: MutableList<String>) {
        // Get all neighbors in lexical order
        val neighbors = adj[airport] ?: return

        // Use all edges (use stack)
        while (neighbors.isNotEmpty()) {
            val next = neighbors.poll() // lex smallest available
            dfs(next, adj, result)
        }

        result.add(airport) // post-order: visit neighbors first, then add airport
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Reconstruct itinerary from list of tickets.
 * Find valid itinerary starting from JFK that uses all tickets exactly once.
 *
 * EXAMPLE 1:
 * Input: tickets = [["MUC", "LHR"], ["JFK", "MUC"], ["SFO", "SJC"], ["LHR", "SFO"]]
 * Output: ["JFK", "MUC", "LHR", "SFO", "SJC"]
 *
 * EXAMPLE 2:
 * Input: tickets = [["JFK","SFO"],["JFK","ATL"],["SFO","ATL"],["ATL","JFK"],["ATL","SFO"]]
 * Output: ["JFK","ATL","SFO","JFK","ATL","SFO"]
 *
 * BRUTE FORCE:
 * - Try all permutations of tickets
 * - Time: O(N!) - factorial, completely infeasible
 *
 * BETTER (DFS):
 * - Build graph and try all paths
 * - Backtrack when stuck
 * - Time: O(N! × N) still too slow
 *
 * OPTIMAL (Hierholzer's Algorithm):
 * - Finds Eulerian path in directed graph
 * - If graph has Eulerian circuit, use Hierholzer's
 * - Algorithm: Start at JFK, do DFS until stuck, then backtrack
 * - Add nodes in post-order to get correct sequence
 * - Time: O(E log E) for sorting + O(E) for traversal
 *
 * KEY INSIGHT: This is finding an Eulerian path in directed graph.
 * The route must use all edges exactly once.
 * Hierholzer's algorithm efficiently finds such path.
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val tickets1 = arrayListOf(
        arrayListOf("MUC", "LHR"),
        arrayListOf("JFK", "MUC"),
        arrayListOf("SFO", "SJC"),
        arrayListOf("LHR", "SFO")
    )
    println("Test 1:")
    println("  tickets = ${tickets1.map { it.toList() }}")
    println("  Itinerary: ${solution.findItinerary(tickets1)}")  // Expected: ["JFK", "MUC", "LHR", "SFO", "SJC"]

    // Test case 2
    val tickets2 = arrayListOf(
        arrayListOf("JFK", "SFO"),
        arrayListOf("JFK", "ATL"),
        arrayListOf("SFO", "ATL"),
        arrayListOf("ATL", "JFK"),
        arrayListOf("ATL", "SFO")
    )
    println("\nTest 2:")
    println("  tickets = ${tickets2.map { it.toList() }}")
    println("  Itinerary: ${solution.findItinerary(tickets2)}")  // Expected: ["JFK", "ATL", "SFO", "JFK", "ATL", "SFO"]

    // Test case 3 - more complex
    val tickets3 = arrayListOf(
        arrayListOf("JFK", "AAA"),
        arrayListOf("JFK", "BBB"),
        arrayListOf("BBB", "JFK"),
        arrayListOf("AAA", "JFK")
    )
    println("\nTest 3:")
    println("  tickets = ${tickets3.map { it.toList() }}")
    println("  Itinerary: ${solution.findItinerary(tickets3)}")  // One valid: ["JFK", "AAA", "JFK", "BBB", "JFK"]
}