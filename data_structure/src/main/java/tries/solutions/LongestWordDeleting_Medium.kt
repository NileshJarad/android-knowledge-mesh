/**
 * LeetCode 524: Longest Word in Dictionary Through Deleting
 * Difficulty: Medium
 * Pattern: Trie optimization for longest valid subsequence
 */

class TrieNode {
    val children: MutableMap<Char, TrieNode> = mutableMapOf()
    var isEndOfWord: Boolean = false
}

class Trie {
    private val root = TrieNode()

    fun insert(word: String) {
        var current: TrieNode = root
        for (ch in word) {
            current = current.children.getOrPut(ch) { TrieNode() }
        }
        current.isEndOfWord = true
    }

    /**
     * Checks if word exists in trie.
     */
    fun search(word: String): Boolean {
        var current: TrieNode = root
        for (ch in word) {
            current = current.children[ch] ?: return false
        }
        return current.isEndOfWord
    }
}

class Solution {
    /**
     * Finds the longest word in dictionary that can be formed by deleting
     * characters from string s.
     *
     * Time: O(D × L) where D = dict size, L = max word length
     * Space: O(T) for Trie where T = total chars in dictionary
     */
    fun findLongestWord(s: String, dictionary: List<String>): String {
        val trie = Trie()
        for (word in dictionary) {
            trie.insert(word)
        }

        var longest = ""

        for (word in dictionary) {
            // Check if word is a subsequence of s
            if (isSubsequence(word, s)) {
                // Update longest word if this one is longer
                if (word.length > longest.length ||
                    (word.length == longest.length && word < longest)) {
                    longest = word
                }
            }
        }

        return longest
    }

    /**
     * Checks if word can be formed by deleting characters from s.
     * This means word is a subsequence of s.
     * Time: O(L + N) where L = word length, N = s length
     */
    private fun isSubsequence(word: String, s: String): Boolean {
        var wi = 0
        for (si in s.indices) {
            if (wi < word.length && word[wi] == s[si]) {
                wi++
                if (wi == word.length) return true
            }
        }
        return false
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Given a string s and a dictionary of words, find the longest word
 * in the dictionary that can be formed by deleting some characters from s.
 * If there are multiple words of the same length, return the lexicographically
 * smallest one.
 *
 * EXAMPLE:
 * s = "abpcplea", dictionary = ["ale", "aloe", "apple"]
 * Output: "apple"
 * Explanation: "apple" can be formed by deleting "b", "p", "p", "l", "e" from s.
 *
 * BRUTE FORCE:
 * - For each word in dictionary, try all possible deletion combinations
 * - Time: O(2^N × D) where N = s length, D = dict size
 * - Exponential time, not feasible for large inputs
 *
 * BETTER (Two pointers):
 * - For each word, check if it's a subsequence of s using two pointers
 * - Time: O(D × N) where D = dict size, N = s length
 * - Space: O(1)
 *
 * OPTIMAL (Trie + Two pointers):
 * - Build Trie from dictionary for fast prefix checks
 * - For each word, check if it's a subsequence of s
 * - Use Trie to potentially optimize the subsequence check
 * - Time: O(D × N) but with early termination
 * - Space: O(T) for Trie
 *
 * KEY INSIGHT: The problem reduces to finding the longest word in dictionary
 * that is a subsequence of s. Trie helps us organize dictionary words for
 * efficient prefix-based checks.
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val s1 = "abpcplea"
    val dict1 = listOf("ale", "aloe", "apple")
    println("Test 1:")
    println("  String: $s1")
    println("  Dict: $dict1")
    println("  Result: ${solution.findLongestWord(s1, dict1)}")  // Expected: "apple"

    // Test case 2
    val s2 = "abcd"
    val dict2 = listOf("ab", "ac", "abc", "adc", "d")
    println("\nTest 2:")
    println("  String: $s2")
    println("  Dict: $dict2")
    println("  Result: ${solution.findLongestWord(s2, dict2)}")  // Expected: "abc"
}