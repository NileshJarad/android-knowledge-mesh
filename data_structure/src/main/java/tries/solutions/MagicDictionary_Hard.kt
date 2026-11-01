/**
 * LeetCode 676: Implement Magic Dictionary
 * Difficulty: Hard
 * Pattern: Trie with flexible matching (one character different)
 */

class TrieNode {
    val children: MutableMap<Char, TrieNode> = mutableMapOf()
    var isEndOfWord: Boolean = false
}

class MagicDictionary {
    private val trie = Trie()

    /** Initialize your data structure here. */
    fun MagicDictionary() {
        // Constructor
    }

    /**
     * Builds the dictionary.
     * Time: O(T) where T = total characters in dictionary
     * Space: O(T)
     */
    fun buildDict(dictionary: Array<String>) {
        for (word in dictionary) {
            trie.insert(word)
        }
    }

    /**
     * Returns if there is any word in the trie that equals to the given word
     * after modifying exactly one character.
     *
     * Approach: For each position in the word, try all 26 lowercase letters
     * as a replacement and check if the resulting word exists in trie.
     *
     * Time: O(L²) where L = word length (L positions × 26 replacements × L search)
     * Space: O(1) extra
     */
    fun search(searchWord: String): Boolean {
        val n = searchWord.length

        // Try changing each position to every possible letter
        for (i in 0 until n) {
            for (c in 'a'..'z') {
                // Skip if same character - we need EXACTLY one change
                if (c == searchWord[i]) continue

                // Build word with one character changed
                val candidate = searchWord.substring(0, i) + c + searchWord.substring(i + 1)

                // Check if this modified word exists in dictionary
                if (trie.search(candidate)) {
                    return true
                }
            }
        }

        return false
    }

    private fun insert(word: String) {
        var current: TrieNode = trie.root
        for (ch in word) {
            current = current.children.getOrPut(ch) { TrieNode() }
        }
        current.isEndOfWord = true
    }

    private fun search(word: String): Boolean {
        var current: TrieNode = trie.root
        for (ch in word) {
            current = current.children[ch] ?: return false
        }
        return current.isEndOfWord
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * PROBLEM: Build a dictionary, then check if a query word matches any dictionary
 * word after exactly one character change.
 *
 * EXAMPLE:
 * buildDict(["hello", "hallo", "leetcode"])
 * search("hello") -> true (hello -> hallo by changing 'e' to 'a')
 * search("hhllo") -> true (hhllo -> hello by changing 'h' to 'e')
 * search("hell") -> false
 * search("leetcoded") -> false
 *
 * BRUTE FORCE:
 * - For each search, compare with every dictionary word character by character
 * - Time: O(D × L) where D = dict size, L = word length
 *
 * BETTER (Hash set + one char variation):
 * - Store dictionary in hash set
 * - For search word, generate all possible 1-char variations (26 × L)
 * - Check each in hash set
 * - Time: O(L²) per search
 * - Space: O(D × L) for hash set
 *
 * OPTIMAL (Trie with one char variation):
 * - Store dictionary in Trie
 * - For search word, for each position try all 26 letters
 * - Search modified word in Trie
 * - Time: O(L²) per search, but with early termination via Trie
 * - Space: O(T) for Trie where T = total dict chars
 *
 * KEY INSIGHT: Trie allows efficient prefix checking. When we change a character,
 * we only need to search from that point in the Trie. However, since we must
 * change exactly one character and check if ANY dictionary word matches, the
 * simplest optimal approach is to generate all 26×L variations and check Trie.
 */

class MagicDictionaryOptimized {
    private val trie = Trie()
    private var maxWordLength = 0

    fun buildDict(dictionary: Array<String>) {
        for (word in dictionary) {
            trie.insert(word)
            maxWordLength = maxOf(maxWordLength, word.length)
        }
    }

    fun search(searchWord: String): Boolean {
        // Quick length check - if no word of this length in dict, return false
        // (optional optimization - we'd need to track lengths per word)

        for (i in 0 until searchWord.length) {
            // Early termination: if remaining length > max dict word length - i
            // but we don't track per-length, skip

            for (c in 'a'..'z') {
                if (c == searchWord[i]) continue

                val candidate = searchWord.substring(0, i) + c + searchWord.substring(i + 1)
                if (trie.search(candidate)) {
                    return true
                }
            }
        }

        return false
    }
}

/**
 * ALTERNATIVE TRIE-BASED SEARCH (single traversal):
 *
 * We can traverse Trie with searchWord, allowing exactly ONE mismatch.
 * This is more complex but avoids generating all 26×L candidates.
 *
 * Time: O(26 × L) in worst case, but often faster with early pruning
 * Space: O(L) for recursion stack
 */
class MagicDictionarySinglePass {
    private val trie = Trie()

    fun buildDict(dictionary: Array<String>) {
        for (word in dictionary) {
            trie.insert(word)
        }
    }

    fun search(searchWord: String): Boolean {
        return searchWithOneMismatch(trie.root, searchWord, 0, false)
    }

    /**
     * Searches Trie allowing at most one character mismatch.
     * @param node Current Trie node
     * @param word Search word
     * @param index Current character index in word
     * @param mismatched Whether we've already used our one allowed mismatch
     */
    private fun searchWithOneMismatch(
        node: TrieNode,
        word: String,
        index: Int,
        mismatched: Boolean
    ): Boolean {
        if (index == word.length) {
            // End of word - must have used exactly one mismatch
            return node.isEndOfWord && mismatched
        }

        val ch = word[index]

        // Try exact match
        if (node.children.containsKey(ch)) {
            if (searchWithOneMismatch(node.children[ch]!!, word, index + 1, mismatched)) {
                return true
            }
        }

        // Try mismatch (if we haven't used it yet)
        if (!mismatched) {
            for ((childChar, childNode) in node.children) {
                if (childChar != ch) {
                    if (searchWithOneMismatch(childNode, word, index + 1, true)) {
                        return true
                    }
                }
            }
        }

        return false
    }
}

fun main() {
    val magicDict = MagicDictionary()

    // Build dictionary
    magicDict.buildDict(arrayOf("hello", "hallo", "leetcode"))

    // Test searches
    println("Test searches:")
    println("search('hello'): ${magicDict.search("hello")}")   // true (hallo)
    println("search('hhllo'): ${magicDict.search("hhllo")}")   // true (hello)
    println("search('hell'): ${magicDict.search("hell")}")     // false
    println("search('leetcoded'): ${magicDict.search("leetcoded")}")  // false

    // Test optimized version
    val optimized = MagicDictionaryOptimized()
    optimized.buildDict(arrayOf("hello", "hallo", "leetcode"))
    println("\nOptimized version:")
    println("search('hello'): ${optimized.search("hello")}")
    println("search('hhllo'): ${optimized.search("hhllo")}")

    // Test single-pass version
    val singlePass = MagicDictionarySinglePass()
    singlePass.buildDict(arrayOf("hello", "hallo", "leetcode"))
    println("\nSingle-pass Trie version:")
    println("search('hello'): ${singlePass.search("hello")}")
    println("search('hhllo'): ${singlePass.search("hhllo")}")
}