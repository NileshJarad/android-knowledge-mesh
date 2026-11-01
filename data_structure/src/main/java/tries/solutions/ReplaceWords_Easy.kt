/**
 * LeetCode 648: Replace Words
 * Difficulty: Easy
 * Pattern: Trie with prefix search
 */

class TrieNode {
    val children: MutableMap<Char, TrieNode> = mutableMapOf()
    val isEndOfWord: Boolean = false
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
     * Finds the shortest root (prefix) for a word.
     * Returns the root if found, otherwise returns the original word.
     */
    fun shortestRoot(word: String): String {
        var current: TrieNode = root
        val sb = StringBuilder()
        for (ch in word) {
            current = current.children[ch] ?: return word
            sb.append(ch)
            if (current.isEndOfWord) {
                return sb.toString()
            }
        }
        return word
    }
}

class Solution {
    fun replaceWords(dictionary: List<String>, sentence: String): String {
        val trie = Trie()

        // Insert all dictionary words into Trie
        for (word in dictionary) {
            trie.insert(word)
        }

        // Split sentence into words and replace each with its shortest root
        val words = sentence.split(" ")
        val result = mutableListOf<String>()

        for (word in words) {
            result.add(trie.shortestRoot(word))
        }

        return result.joinToString(" ")
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Given a dictionary of root words and a sentence, replace each word in
 * the sentence with its shortest prefix from the dictionary.
 *
 * EXAMPLE:
 * dictionary = ["cat", "bat", "rat"]
 * sentence = "the cattle was rattled by the battery"
 * Output: "the cat was rat by the bat"
 *
 * APPROACH:
 * 1. Build a Trie from all dictionary words (roots)
 * 2. Split sentence into words
 * 3. For each word, traverse Trie to find the shortest root
 *    - Stop at first node where isEndOfWord = true
 *    - If no root found, keep original word
 * 4. Join results back into sentence
 *
 * BRUTE FORCE: For each word in sentence, check each dictionary root
 *   - Time: O(S × D) where S = sentence words, D = dictionary size
 *   - For each word, iterate through all roots and check prefix
 *
 * BETTER: Hash set with prefix check
 *   - Time: O(S × L²) where L = max word length
 *   - For each word, try each possible prefix and check hash set
 *
 * OPTIMAL (Trie): O(S × L) where S = sentence words, L = max word length
 *   - Trie lookup finds shortest root in one pass through characters
 */

fun main() {
    val solution = Solution()

    val dictionary = listOf("cat", "bat", "rat")
    val sentence = "the cattle was rattled by the battery"

    println("Input dictionary: $dictionary")
    println("Input sentence: $sentence")
    println("Output: ${solution.replaceWords(dictionary, sentence)}")
    // Expected: "the cat was rat by the bat"
}