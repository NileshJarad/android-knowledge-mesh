/**
 * LeetCode 207: Course Schedule
 * Difficulty: Medium
 * Pattern: Cycle Detection in Directed Graph
 */

class Solution {
    /**
     * There are numCourses courses labeled from 0 to numCourses - 1.
     * Some courses may have prerequisites, given as [ai, bi] meaning
     * you must take course bi before course ai.
     *
     * Return true if all courses can be finished (no cycle in prerequisites graph).
     *
     * Time: O(N + P) where N = numCourses, P = prerequisites length
     * Space: O(N + P) for adjacency list + coloring array
     */
    fun canFinish(numCourses: Int, prerequisites: Array<IntArray>): Boolean {
        // Build adjacency list: bi -> ai (bi is prerequisite for ai)
        val adj = Array(numCourses) { mutableListOf<Int>() }
        for (req in prerequisites) {
            val course = req[0]
            val prereq = req[1]
            adj[prereq].add(course)
        }

        // Coloring: 0 = white (unvisited), 1 = gray (in progress), 2 = black (done)
        val color = IntArray(numCourses) // all 0 by default

        for (i in 0 until numCourses) {
            if (color[i] == 0) {
                if (hasCycle(i, adj, color)) {
                    return false // Cycle detected, cannot finish all courses
                }
            }
        }

        return true // No cycle, all courses can be finished
    }

    /**
     * DFS with coloring to detect cycle in directed graph.
     * Returns true if a cycle is found starting from node.
     */
    private fun hasCycle(
        node: Int,
        adj: Array<MutableList<Int>>,
        color: IntArray
    ): Boolean {
        color[node] = 1 // gray - currently in recursion stack

        for (neighbor in adj[node]) {
            when (color[neighbor]) {
                1 -> return true // gray node found = back edge = cycle
                0 -> { // white node = not visited yet
                    if (hasCycle(neighbor, adj, color)) return true
                }
                // 2 = black = already fully processed, skip
            }
        }

        color[node] = 2 // black - done processing this node
        return false
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Determine if all courses can be finished given prerequisites.
 *
 * EXAMPLE 1:
 * Input: numCourses = 2, prerequisites = [[1,0]]
 * Output: true
 * Explanation: Take course 0 first, then course 1. No cycle.
 *
 * EXAMPLE 2:
 * Input: numCourses = 2, prerequisites = [[1,0],[0,1]]
 * Output: false
 * Explanation: Course 1 needs course 0, and course 0 needs course 1. Cycle!
 *
 * BRUTE FORCE:
 * - Try all possible orderings of courses
 * - Time: O(N!) - factorial, not feasible
 *
 * BETTER (Kahn's Algorithm / BFS):
 * - Compute indegree of each course
 * - Process courses with indegree 0, reduce indegree of dependents
 * - If all courses processed, no cycle
 * - Time: O(N + P), Space: O(N + P)
 *
 * OPTIMAL (DFS with Coloring):
 * - Use 3-color marking to detect back edges
 * - Gray node encountered again = cycle
 * - Time: O(N + P), Space: O(N + P)
 * - Single DFS pass, more intuitive for cycle detection
 *
 * KEY INSIGHT: Prerequisites form a directed graph. If there's a cycle,
 * you can never start any course in the cycle (each needs another first).
 * No cycle = topological ordering exists = all courses finishable.
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val n1 = 2
    val prereq1 = arrayOf(intArrayOf(1, 0))
    println("Test 1:")
    println("  numCourses = $n1, prerequisites = ${prereq1.map { it.contentToString() }.toList()}")
    println("  Can finish: ${solution.canFinish(n1, prereq1)}")  // Expected: true

    // Test case 2 - cycle
    val n2 = 2
    val prereq2 = arrayOf(intArrayOf(1, 0), intArrayOf(0, 1))
    println("\nTest 2:")
    println("  numCourses = $n2, prerequisites = ${prereq2.map { it.contentToString() }.toList()}")
    println("  Can finish: ${solution.canFinish(n2, prereq2)}")  // Expected: false

    // Test case 3 - no prerequisites
    val n3 = 3
    val prereq3 = arrayOf<IntArray>()
    println("\nTest 3:")
    println("  numCourses = $n3, prerequisites = []")
    println("  Can finish: ${solution.canFinish(n3, prereq3)}")  // Expected: true

    // Test case 4 - complex cycle
    val n4 = 4
    val prereq4 = arrayOf(
        intArrayOf(1, 0),
        intArrayOf(2, 1),
        intArrayOf(3, 2),
        intArrayOf(1, 3) // creates cycle: 0->1->2->3->1
    )
    println("\nTest 4:")
    println("  numCourses = $n4, prerequisites = ${prereq4.map { it.contentToString() }.toList()}")
    println("  Can finish: ${solution.canFinish(n4, prereq4)}")  // Expected: false
}