/**
 * LeetCode 208: Implement Trie (Prefix Tree)
 * Difficulty: Easy
 * Pattern: Basic Trie design
 */

class TrieNode {
    val children: MutableMap<Char, TrieNode> = mutableMapOf()
    val isEndOfWord: Boolean = false
}

class Trie {
    private val root = TrieNode()

    /** Inserts a word into the trie. */
    fun insert(word: String) {
        var current: TrieNode = root
        for (ch in word) {
            current = current.children.getOrPut(ch) { TrieNode() }
        }
        current.isEndOfWord = true
    }

    /** Returns if the word is in the trie. */
    fun search(word: String): Boolean {
        var current: TrieNode = root
        for (ch in word) {
            current = current.children[ch] ?: return false
        }
        return current.isEndOfWord
    }

    /** Returns if there is any word in the trie that starts with the given prefix. */
    fun startsWith(prefix: String): Boolean {
        var current: TrieNode = root
        for (ch in prefix) {
            current = current.children[ch] ?: return false
        }
        return true
    }
}

/**
 * SOLUTION EXPLANATION:
 *
 * The Trie data structure uses a tree where each node represents a character.
 * - `children` map: Each node has a map of child nodes keyed by character
 * - `isEndOfWord`: Boolean flag marking the end of a complete word
 *
 * INSERT operation (O(L) time, O(L) space where L = word length):
 * 1. Start at root node
 * 2. For each character in the word:
 *    - If child node exists for that character, move to it
 *    - Otherwise, create new node and move to it
 * 3. After processing all characters, mark the final node as end of word
 *
 * SEARCH operation (O(L) time, O(1) space):
 * 1. Start at root node
 * 2. For each character in the word:
 *    - If no child exists for that character, word doesn't return false
 * 3. After processing all characters, return isEndOfWord flag
 *
 * STARTSWITH operation (O(P) time where P = prefix length):
 * 1. Same traversal as search, but return true if all prefix characters found
 * 2. We don't need to check isEndOfWord - any path represents a prefix
 */

fun main() {
    val trie = Trie()

    // Insert words
    trie.insert("apple")
    trie.insert("app")
    trie.insert("application")
    trie.insert("banana")

    // Search operations
    println("apple exists: ${trie.search("apple")}")  // true
    println("app exists: ${trie.search("app")}")      // true
    println("application exists: ${trie.search("application")}")  // true
    println("apply exists: ${trie.search("apply")}")    // false
    println("banana exists: ${trie.search("banana")}")  // true
    println("band exists: ${trie.search("band")}")      // false

    // Prefix search
    println("Words starting with 'app':")
    // Note: Trie doesn't have built-in getWordsWithPrefix, but we can implement it
    println("startsWith 'app': ${trie.startsWith("app")}")   // true
    println("startsWith 'appl': ${trie.startsWith("appl")}") // true
    println("startsWith 'bat': ${trie.startsWith("bat")}")   // false
    println("startsWith '': ${trie.startsWith("")}")         // true (empty prefix)
}