/**
 * LeetCode 139: Word Break
 * Difficulty: Medium
 * Pattern: Trie + Dynamic Programming
 */

class TrieNode {
    val children: MutableMap<Char, TrieNode> = mutableMapOf()
    var isEndOfWord: Boolean = false
}

class Trie {
    private val root = TrieNode()
    var maxWordLength = 0

    fun insert(word: String) {
        var current: TrieNode = root
        for (ch in word) {
            current = current.children.getOrPut(ch) { TrieNode() }
        }
        current.isEndOfWord = true
        maxWordLength = maxOf(maxWordLength, word.length)
    }

    /**
     * Checks if a word exists in the trie.
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
     * Determines if a string can be segmented into dictionary words.
     *
     * Time: O(N × L) where N = length of s, L = max word length in dict
     * Space: O(N) for DP array + O(T) for Trie where T = total chars in dict
     */
    fun wordBreak(s: String, wordDict: List<String>): Boolean {
        val trie = Trie()
        for (word in wordDict) {
            trie.insert(word)
        }

        val n = s.length
        // dp[i] = true if s[0..i-1] can be segmented
        val dp = BooleanArray(n + 1)
        dp[0] = true // Empty string is always valid

        for (i in 1..n) {
            // Try all possible splits ending at position i
            // Optimization: only check up to max word length back
            var j = i - 1
            while (j >= 0 && (i - j) <= trie.maxWordLength) {
                if (dp[j] && trie.search(s.substring(j, i))) {
                    dp[i] = true
                    break
                }
                j--
            }
        }

        return dp[n]
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Given a string s and a dictionary of words wordDict, determine if s
 * can be segmented into a space-separated sequence of one or more dictionary words.
 *
 * EXAMPLE:
 * s = "leetcode", wordDict = ["leet", "code"]
 * Output: true
 * Explanation: Return true because "leetcode" can be segmented as "leet code".
 *
 * EXAMPLE 2:
 * s = "applepenapple", wordDict = ["apple", "pen"]
 * Output: true
 * Explanation: Return true because "applepenapple" can be segmented as
 * "apple pen apple".
 *
 * BRUTE FORCE (Recursive):
 * - Try all possible splits recursively
 * - Time: O(2^N) exponential - explores all combinations
 * - Space: O(N) call stack depth
 *
 * BETTER (DP without Trie):
 * - dp[i] = true if s[0..i-1] can be segmented
 * - dp[0] = true (empty string)
 * - For each i from 1 to n: dp[i] = any(dp[j] && s[j..i] in dict) for j < i
 * - Time: O(N²) where N = length of s
 * - Space: O(N) for dp array
 *
 * OPTIMAL (Trie + DP):
 * - Build Trie from dictionary (O(T) time, T = total dict chars)
 * - dp[i] = true if s[0..i-1] can be segmented using Trie lookup
 * - When checking dp[j] && s[j..i], use Trie instead of hash set
 * - Optimization: limit j to i - maxWordLength to avoid unnecessary checks
 * - Time: O(N × L) where L = max word length in dictionary
 * - Space: O(N + T)
 *
 * KEY INSIGHT: Trie allows us to efficiently check prefixes during the DP loop.
 * Instead of extracting substring and checking hash set (O(L) for substring + O(1) hash),
 * we traverse Trie character by character, potentially stopping early if path doesn't exist.
 */

fun main() {
    val solution = Solution()

    // Test case 1
    val s1 = "leetcode"
    val dict1 = listOf("leet", "code")
    println("Test 1:")
    println("  String: $s1")
    println("  Dict: $dict1")
    println("  Result: ${solution.wordBreak(s1, dict1)}")  // Expected: true

    // Test case 2
    val s2 = "applepenapple"
    val dict2 = listOf("apple", "pen")
    println("\nTest 2:")
    println("  String: $s2")
    println("  Dict: $dict2")
    println("  Result: ${solution.wordBreak(s2, dict2)}")  // Expected: true

    // Test case 3
    val s3 = "catsandog"
    val dict3 = listOf("cats", "dog", "sand", "and", "cat")
    println("\nTest 3:")
    println("  String: $s3")
    println("  Dict: $dict3")
    println("  Result: ${solution.wordBreak(s3, dict3)}")  // Expected: false
}