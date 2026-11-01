/**
 * LeetCode 269: Alien Dictionary
 * Difficulty: Hard
 * Pattern: Topological Sort with graph building from strings
 */

class Solution {
    /**
     * There is a new alien language that uses lowercase English letters.
     * However, the order of letters is unknown to you.
     *
     * You are given a list of strings words from the alien language's dictionary,
     * sorted lexicographically by the rules of this new language.
     *
     * Return a string of the unique letters in the new alien language sorted
     * in lexicographically increasing order by the new language's rules.
     * If no valid order exists, return "".
     * If multiple valid orders exist, return any of them.
     *
     * Time: O(C) where C = total characters in all words
     * Space: O(1) - at most 26 letters + adjacency list
     */
    fun alienOrder(words: Array<String>): String {
        val n = words.size

        // Step 1: Build adjacency list and indegree for all unique characters
        val adj = mutableMapOf<Char, MutableSet<Char>>()
        val indegree = mutableMapOf<Char, Int>()

        // Initialize all characters
        for (word in words) {
            for (ch in word) {
                if (!adj.containsKey(ch)) {
                    adj[ch] = mutableSetOf()
                }
                if (!indegree.containsKey(ch)) {
                    indegree[ch] = 0
                }
            }
        }

        // Step 2: Build graph by comparing adjacent words
        for (i in 0 until n - 1) {
            val word1 = words[i]
            val word2 = words[i + 1]

            // Check for invalid case: word1 is prefix of word2 and word1 is longer
            // e.g., ["abc", "ab"] is invalid because "ab" should come before "abc"
            val minLen = minOf(word1.length, word2.length)
            if (word1.substring(0, minLen) == word2.substring(0, minLen) &&
                word1.length > word2.length) {
                return ""
            }

            // Find first differing character
            for (j in 0 until minLen) {
                if (word1[j] != word2[j]) {
                    val u = word1[j] // comes before
                    val v = word2[j] // comes after
                    // Add edge u -> v if not already present
                    if (adj[u]!!.add(v)) {
                        indegree[v] = indegree.getOrDefault(v, 0) + 1
                    }
                    break // Only first differing character matters
                }
            }
        }

        // Step 3: Kahn's Algorithm for topological sort
        val queue = java.util.ArrayDeque<Char>()
        for ((ch, deg) in indegree) {
            if (deg == 0) {
                queue.add(ch)
            }
        }

        val result = StringBuilder()
        while (queue.isNotEmpty()) {
            val ch = queue.removeFirst()
            result.append(ch)

            for (next in adj[ch]!!) {
                indegree[next] = indegree.getOrDefault(next, 0) - 1
                if (indegree[next] == 0) {
                    queue.add(next)
                }
            }
        }

        // Check if all characters are included (no cycle)
        return if (result.length == indegree.size) result.toString() else ""
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Given sorted words in alien language, determine character order.
 *
 * EXAMPLE 1:
 * Input: words = ["wrt","wrf","er","ett","rftt"]
 * Output: "wertf"
 * Explanation: w->e (from wrt/wrf), e->r (from wrf/er), r->t (from er/ett), t->f (from ett/rftt)
 *
 * EXAMPLE 2:
 * Input: words = ["z","x"]
 * Output: "zx"
 * Explanation: z comes before x.
 *
 * EXAMPLE 3:
 * Input: words = ["z","x","z"]
 * Output: ""
 * Explanation: Invalid ordering (cycle: z->x->z).
 *
 * BRUTE FORCE:
 * - Try all permutations of unique characters (up to 26!)
 * - Check if each permutation satisfies word ordering constraints
 * - Time: O(26! × C) - completely infeasible
 *
 * BETTER (Graph building + DFS topological sort):
 * - Build graph from adjacent word comparisons
 * - DFS-based topological sort with cycle detection
 * - Time: O(C + V + E) where V ≤ 26, E ≤ V²
 * - More complex cycle detection
 *
 * OPTIMAL (Kahn's Algorithm):
 * - Build adjacency list and indegree from adjacent word pairs
 * - Use BFS (Kahn's) for topological sort
 * - Time: O(C), Space: O(1) - bounded by 26 letters
 * - Naturally detects cycles
 *
 * KEY INSIGHT: Comparing adjacent words reveals character ordering:
 * - First differing character between word1 and word2 tells us word1[char] < word2[char]
 * - Build directed edges, then topological sort gives the character order
 * - Edge case: if word1 is longer prefix of word2, ordering is invalid
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val words1 = arrayOf("wrt", "wrf", "er", "ett", "rftt")
    println("Test 1:")
    println("  words = ${words1.toList()}")
    println("  Order: ${solution.alienOrder(words1)}")  // Expected: "wertf" or similar valid order

    // Test case 2
    val words2 = arrayOf("z", "x")
    println("\nTest 2:")
    println("  words = ${words2.toList()}")
    println("  Order: ${solution.alienOrder(words2)}")  // Expected: "zx"

    // Test case 3 - cycle
    val words3 = arrayOf("z", "x", "z")
    println("\nTest 3:")
    println("  words = ${words3.toList()}")
    println("  Order: '${solution.alienOrder(words3)}'")  // Expected: ""

    // Test case 4 - single word
    val words4 = arrayOf("abc")
    println("\nTest 4:")
    println("  words = ${words4.toList()}")
    println("  Order: ${solution.alienOrder(words4)}")  // Expected: "abc" (any order of a,b,c)
}