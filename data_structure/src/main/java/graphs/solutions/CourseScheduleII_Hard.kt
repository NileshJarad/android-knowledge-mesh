/**
 * LeetCode 210: Course Schedule II
 * Difficulty: Hard
 * Pattern: Topological Sort (Kahn's Algorithm)
 */

class Solution {
    /**
     * There are numCourses courses labeled from 0 to numCourses - 1.
     * Some courses may have prerequisites, given as [ai, bi] meaning
     * you must take course bi before course ai.
     *
     * Return the ordering of courses to finish all courses.
     * If impossible, return empty array.
     *
     * Time: O(N + P) where N = numCourses, P = prerequisites length
     * Space: O(N + P) for adjacency list + indegree array
     */
    fun findOrder(numCourses: Int, prerequisites: Array<IntArray>): IntArray {
        // Build adjacency list: bi -> ai (bi is prerequisite for ai)
        val adj = Array(numCourses) { mutableListOf<Int>() }
        val indegree = IntArray(numCourses)

        for (req in prerequisites) {
            val course = req[0]
            val prereq = req[1]
            adj[prereq].add(course)
            indegree[course]++
        }

        // Kahn's Algorithm: start with courses having no prerequisites
        val queue = java.util.ArrayDeque<Int>()
        for (i in 0 until numCourses) {
            if (indegree[i] == 0) {
                queue.add(i)
            }
        }

        val result = IntArray(numCourses)
        var index = 0

        while (queue.isNotEmpty()) {
            val course = queue.removeFirst()
            result[index++] = course

            for (next in adj[course]) {
                indegree[next]--
                if (indegree[next] == 0) {
                    queue.add(next)
                }
            }
        }

        // If we couldn't process all courses, there's a cycle
        return if (index == numCourses) result else IntArray(0)
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Return ordering of courses to finish all courses given prerequisites.
 *
 * EXAMPLE 1:
 * Input: numCourses = 2, prerequisites = [[1,0]]
 * Output: [0,1]
 * Explanation: Take course 0 first (no prerequisites), then course 1.
 *
 * EXAMPLE 2:
 * Input: numCourses = 4, prerequisites = [[1,0],[2,0],[3,1],[3,2]]
 * Output: [0,2,1,3] or [0,1,2,3]
 * Explanation: Course 0 first, then 1 and 2 (in any order), then 3.
 *
 * EXAMPLE 3:
 * Input: numCourses = 2, prerequisites = [[1,0],[0,1]]
 * Output: []
 * Explanation: Cycle exists, impossible to finish.
 *
 * BRUTE FORCE:
 * - Generate all permutations of courses
 * - Check each permutation for validity
 * - Time: O(N!) - factorial, not feasible
 *
 * BETTER (DFS-based Topological Sort):
 * - DFS from each node, add to result after processing all neighbors
 * - Reverse result for topological order
 * - Time: O(N + P), Space: O(N + P)
 * - Need cycle detection too
 *
 * OPTIMAL (Kahn's Algorithm - BFS):
 * - Compute indegree for each course
 * - Queue courses with indegree 0 (no prerequisites)
 * - Process course, reduce indegree of dependents
 * - If dependent's indegree becomes 0, add to queue
 * - Time: O(N + P), Space: O(N + P)
 * - Naturally detects cycle (if result size < N)
 *
 * KEY INSIGHT: Kahn's algorithm processes courses in topological order.
 * Courses with no prerequisites come first. After taking a course,
 * dependents that had only this course as remaining prerequisite
 * become available.
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val n1 = 2
    val prereq1 = arrayOf(intArrayOf(1, 0))
    println("Test 1:")
    println("  numCourses = $n1, prerequisites = ${prereq1.map { it.contentToString() }.toList()}")
    println("  Order: ${solution.findOrder(n1, prereq1).contentToString()}")  // Expected: [0, 1]

    // Test case 2
    val n2 = 4
    val prereq2 = arrayOf(
        intArrayOf(1, 0),
        intArrayOf(2, 0),
        intArrayOf(3, 1),
        intArrayOf(3, 2)
    )
    println("\nTest 2:")
    println("  numCourses = $n2, prerequisites = ${prereq2.map { it.contentToString() }.toList()}")
    println("  Order: ${solution.findOrder(n2, prereq2).contentToString()}")  // Expected: [0, 1, 2, 3] or [0, 2, 1, 3]

    // Test case 3 - cycle
    val n3 = 2
    val prereq3 = arrayOf(intArrayOf(1, 0), intArrayOf(0, 1))
    println("\nTest 3:")
    println("  numCourses = $n3, prerequisites = ${prereq3.map { it.contentToString() }.toList()}")
    println("  Order: ${solution.findOrder(n3, prereq3).contentToString()}")  // Expected: []

    // Test case 4 - no prerequisites
    val n4 = 3
    val prereq4 = arrayOf<IntArray>()
    println("\nTest 4:")
    println("  numCourses = $n4, prerequisites = []")
    println("  Order: ${solution.findOrder(n4, prereq4).contentToString()}")  // Expected: [0, 1, 2]
}