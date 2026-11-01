/**
 * LeetCode 997: Find the Town Judge
 * Difficulty: Easy
 * Pattern: Graph indegree/outdegree (Connected components variant)
 */

class Solution {
    /**
     * In a town of N people labeled 1 to N, there is a rumor that one of them
     * is secretly the town judge.
     *
     * The town judge:
     * 1. Trusts nobody.
     * 2. Is trusted by everybody else (N-1 people).
     * 3. There is exactly one person satisfying properties 1 and 2.
     *
     * Given trust array where trust[i] = [a, b] means person a trusts person b,
     * return the label of the town judge, or -1 if no judge exists.
     *
     * Time: O(N + Trust.length)
     * Space: O(N)
     */
    fun findJudge(n: Int, trust: Array<IntArray>): Int {
        // indegree[i] = how many people trust person i
        // outdegree[i] = how many people person i trusts
        val indegree = IntArray(n + 1) // 1-indexed
        val outdegree = IntArray(n + 1) // 1-indexed

        for (t in trust) {
            val a = t[0] // person who trusts
            val b = t[1] // person being trusted
            outdegree[a]++
            indegree[b]++
        }

        // Judge: trusts nobody (outdegree = 0) AND trusted by everyone else (indegree = n-1)
        for (i in 1..n) {
            if (outdegree[i] == 0 && indegree[i] == n - 1) {
                return i
            }
        }

        return -1
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Find the town judge who trusts nobody but is trusted by everybody else.
 *
 * EXAMPLE 1:
 * Input: n = 2, trust = [[1,2]]
 * Output: 2
 * Explanation: Person 1 trusts person 2. Person 2 trusts nobody. Judge is person 2.
 *
 * EXAMPLE 2:
 * Input: n = 3, trust = [[1,3],[2,3]]
 * Output: 3
 * Explanation: Persons 1 and 2 trust person 3. Person 3 trusts nobody. Judge is person 3.
 *
 * EXAMPLE 3:
 * Input: n = 3, trust = [[1,3],[2,3],[3,1]]
 * Output: -1
 * Explanation: Person 3 trusts person 1, so person 3 is not the judge. No judge exists.
 *
 * BRUTE FORCE:
 * - For each person i from 1 to N:
 *   - Check if i trusts nobody (scan trust array for i as trustor)
 *   - Check if everybody except i trusts i (scan trust array for i as trustee)
 * - Time: O(N × T) where T = trust array length
 *
 * BETTER (HashMap):
 * - Use two hash maps to count indegree and outdegree
 * - Time: O(T), Space: O(N)
 *
 * OPTIMAL (Array-based counting):
 * - Use two arrays of size N+1 for indegree and outdegree
 * - Single pass through trust array to populate counts
 * - Second pass to find judge
 * - Time: O(N + T), Space: O(N)
 *
 * KEY INSIGHT: This is essentially a graph problem where:
 * - Each person is a node
 * - Each trust relationship is a directed edge (a → b means a trusts b)
 * - Judge is a node with outdegree = 0 and indegree = N-1
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val n1 = 2
    val trust1 = arrayOf(intArrayOf(1, 2))
    println("Test 1:")
    println("  n = $n1, trust = ${trust1.map { it.contentToString() }.toList()}")
    println("  Judge: ${solution.findJudge(n1, trust1)}")  // Expected: 2

    // Test case 2
    val n2 = 3
    val trust2 = arrayOf(intArrayOf(1, 3), intArrayOf(2, 3))
    println("\nTest 2:")
    println("  n = $n2, trust = ${trust2.map { it.contentToString() }.toList()}")
    println("  Judge: ${solution.findJudge(n2, trust2)}")  // Expected: 3

    // Test case 3
    val n3 = 3
    val trust3 = arrayOf(intArrayOf(1, 3), intArrayOf(2, 3), intArrayOf(3, 1))
    println("\nTest 3:")
    println("  n = $n3, trust = ${trust3.map { it.contentToString() }.toList()}")
    println("  Judge: ${solution.findJudge(n3, trust3)}")  // Expected: -1

    // Test case 4 - single person
    val n4 = 1
    val trust4 = arrayOf<IntArray>()
    println("\nTest 4:")
    println("  n = $n4, trust = []")
    println("  Judge: ${solution.findJudge(n4, trust4)}")  // Expected: 1 (only person is judge)
}