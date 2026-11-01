# Topic 12 - Tries (Trie)

**Difficulty:** Medium-Hard  
**Estimated study time:** 2-3 hours  

---

## 1. Concept explanation

### What a Trie is
A Trie (also called prefix tree) is a tree-like data structure used to store a dynamic set of keys (typically strings) for efficient retrieval. Think of it as a dictionary where each node represents a character, and paths from root to nodes represent words.

### How it works internally
- **Structure**: Each node represents a single character
- **Root**: Empty node (represents start of string)
- **Children**: Edges to next characters
- **Terminal nodes**: Mark end of valid words with `isEndOfWord = true`

### Real-life analogy
A Trie is like a phone book where contacts are organized by their first letters. When you search for "Alex", it goes to 'A' -> 'l' -> 'e' -> 'x' to find if the word exists, without checking entire dictionary.

### Time and space complexity

| Operation | Time Complexity | Space Complexity | Notes |
|-----------|----------------|------------------|-------|
| Insert | O(L) where L = length of word | O(Total chars) | Create nodes for each character |
| Search | O(L) | O(1) | Only traverse word path |
| Delete | O(L) | O(1) | Mark or remove nodes |
| StartsWith(prefix) | O(P) where P = prefix length | O(1) | Partial search |

### Common mistakes and edge cases
1. **Memory vs Speed**: Tries use more memory but offer faster lookups
2. **Empty strings**: Handle empty word insertion/search
3. **Unicode**: Special characters and Unicode handling
4. **Duplicate words**: Decide whether to allow or track frequency
5. **Character encoding**: ASCII vs Unicode in implementations

### When to use in interviews
- **Autocomplete systems**: Finding all words with a given prefix
- **Spell checkers**: Checking if word exists
- **Search suggestions**: Providing search completions
- **Routing tables**: Network routing with IP prefixes
- **Text processing**: Finding all occurrences of a pattern

---

## 2. Important patterns

### Pattern 1: Basic Trie Operations
**Common in interviews for fundamental understanding**.

**Example**: Build a Trie and check if word exists

**Brute force**: Linear scan all words (O(N×L) time)

**Better approach**: Hash set of words (O(1) lookup but O(N) space)

**Optimal approach**: Trie (O(L) lookup, O(Total chars) space)

### Pattern 2: Autocomplete/Prefix Search
**Pattern for finding all words with a given prefix**.

**Use case**: Search suggestions while typing

**Approach**:
1. Insert all words into Trie
2. Find node for prefix
3. DFS/BFS to collect all words under that node

**Example**: Given words ["apple", "app", "application", "banana"], prefix "app" → ["apple", "app", "application"]

### Pattern 3: Word Break Problem
**Given a string and dictionary, check if string can be segmented**.

**Brute force**: Try all possible splits (exponential time)

**Better approach**: DP with hash set (O(N²) time)

**Optimal approach**: Trie + DP (O(N×L) where L is max word length)

### Pattern 4: Longest Word in Dictionary
**Find longest word that can be built from other words**.

**Example**: Given ["w", "wo", "wor", "worl", "world"], return "world"

**Approach**: Insert all words, then for each word check if its path exists in Trie

### Pattern 5: Replace Words
**Given a dictionary of words and a sentence, replace prefixes with shortest root**.

**Example**: Dictionary ["cat", "bat"], sentence "the cattle was rattled" → "the cat was bat"

**Approach**: Build Trie, split sentence into words, check prefixes in Trie

---

## 3. Dry run

Let's walk through implementing `insert()` and `search()` operations with example:

**Initial Trie**: Empty

**Insert "apple"**:
- Start at root
- Create 'a' child if not exists
- Create 'p' child of 'a' 
- Create 'p' child of 'p'
- Create 'l' child of 'p'
- Create 'e' child of 'l'
- Mark 'e' node as isEndOfWord = true

**Search "app"**:
- Start at root
- Go to 'a' child ✓
- Go to 'p' child of 'a' ✓
- Go to 'p' child of 'p' ✓
- 'p' is NOT marked as isEndOfWord, so "app" doesn't exist

**Search "apple"**:
- Follow path 'a'→'p'→'p'→'l'→'e'
- All nodes exist and 'e' is marked isEndOfWord = true
- Return true

**Visualization**:
```
      (root)
        |
        a
        |
        p
        |
        p
        |
        l
        |
        e   (isEndOfWord = true)
```

---

## 4. Code template (Kotlin)

```kotlin
data class TrieNode(
    var children: MutableMap<Char, TrieNode> = mutableMapOf(),
    var isEndOfWord: Boolean = false
) {
    fun insert(word: String, index: Int = 0, current: TrieNode = this) {
        if (index >= word.length) {
            current.isEndOfWord = true
            return
        }
        
        val char = word[index]
        val child = current.children.getOrPut(char) { TrieNode() }
        child.insert(word, index + 1, current)
    }
    
    fun search(word: String, index: Int = 0, current: TrieNode = this): Boolean {
        if (index >= word.length) {
            return current.isEndOfWord
        }
        
        val char = word[index]
        return current.children.getOrDefault(char, TrieNode()).search(word, index + 1)
    }
    
    fun startsWith(prefix: String, index: Int = 0, current: TrieNode = this): Boolean {
        if (index >= prefix.length) {
            return true
        }
        
        val char = prefix[index]
        return current.children.getOrDefault(char, TrieNode()).startsWith(prefix, index + 1)
    }
    
    fun delete(word: String, index: Int = 0, current: TrieNode = this): Boolean {
        if (index >= word.length) {
            if (!current.isEndOfWord) {
                return false // Word not found
            }
            current.isEndOfWord = false
            return current.children.isEmpty()
        }
        
        val char = word[index]
        val child = current.children[char] ?: return false
        val shouldDeleteChild = child.delete(word, index + 1)
        
        if (shouldDeleteChild) {
            current.children.remove(char)
            // Only delete current node if it's not end of another word
            // and has no other children
            return !current.isEndOfWord && current.children.isEmpty()
        }
        return false
    }
    
    fun getWordsWithPrefix(prefix: String, current: TrieNode = this): List<String> {
        val result = mutableListOf<String>()
        
        if (prefix.isEmpty()) {
            // Collect all words starting from this node
            collectAllWords(current, prefix, result)
            return result
        }
        
        // Move to the node for the first character
        val firstChar = prefix[0]
        val child = current.children[firstChar] ?: return emptyList()
        
        // Recursively collect words
        return child.getWordsWithPrefix(prefix.substring(1), child)
    }
    
    private fun collectAllWords(node: TrieNode, prefix: String, result: MutableList<String>) {
        if (node.isEndOfWord) {
            result.add(prefix)
        }
        for ((char, child) in node.children) {
            collectAllWords(child, prefix + char, result)
        }
    }
}

class Trie {
    private val root = TrieNode()
    
    fun insert(word: String) {
        root.insert(word)
    }
    
    fun search(word: String): Boolean {
        return root.search(word)
    }
    
    fun startsWith(prefix: String): Boolean {
        return root.startsWith(prefix)
    }
    
    fun delete(word: String) {
        root.delete(word)
    }
    
    fun getWordsWithPrefix(prefix: String): List<String> {
        return root.getWordsWithPrefix(prefix)
    }
}

// Usage example
fun main() {
    val trie = Trie()
    
    // Insert words
    trie.insert("apple")
    trie.insert("app")
    trie.insert("application")
    trie.insert("banana")
    
    // Search operations
    println("apple exists: ${trie.search("apple")}")  // true
    println("apply exists: ${trie.search("apply")}")    // false
    
    // Prefix search
    println("Words starting with 'app': ${trie.getWordsWithPrefix("app")}")
    // Output: [app, apple, application]
}
```

**Line by line explanation**:

1. **TrieNode class**: Nested class representing each character node
   - `children`: Map storing next characters
   - `isEndOfWord`: Flag marking end of valid words

2. **Insert recursive function**: 
   - Base case: When at word end, mark node as terminal
   - Recursive step: Create/access child node for current character

3. **Search recursive function**:
   - Base case: At word end, return isEndOfWord status
   - Recursive step: Find child node for current character

4. **startsWith method**:
   - Similar to search but without requiring isEndOfWord

5. **Delete method**:
   - Remove terminal flag
   - Clean up unused nodes (optional optimization)

6. **getWordsWithPrefix**: 
   - Traverse to prefix node
   - DFS to collect all words under it

7. **Trie class**:
   - Public interface with convenience methods

8. **Main method**:
   - Shows usage examples

---

## 5. Practice problems

### Easy 1: Design Trie
**Problem**: Implement a Trie with insert, search, and startsWith methods.

**LeetCode**: [208. Implement Trie (Prefix Tree)](https://leetcode.com/problems/implement-trie-prefix-tree/)

**Core pattern**: Basic Trie operations

**Hint**: Focus on recursive implementation and proper node handling

**Expected time complexity**: O(L) per operation, where L is word/prefix length

### Easy 2: Replace Words
**Problem**: In a sentence, replace each word with its shortest prefix from a dictionary.

**LeetCode**: [648. Replace Words](https://leetcode.com/problems/replace-words/)

**Core pattern**: Trie with prefix search

**Hint**: Insert all dictionary words, then for each sentence word, find shortest prefix

**Expected time complexity**: O(N×L) where N is sentence length, L is max word length

### Medium 1: Word Break
**Problem**: Given a string and dictionary, determine if string can be segmented into dictionary words.

**LeetCode**: [139. Word Break](https://leetcode.com/problems/word-break/)

**Core pattern**: Trie + Dynamic Programming

**Hint**: Use Trie for dictionary lookup while DP tries different splits

**Expected time complexity**: O(N×L) where L is max word length in dictionary

### Medium 2: Longest Word in Dictionary Through Deleting
**Problem**: Given words and a target, find longest word in words that can be formed by deleting letters.

**LeetCode**: [524. Longest Word in Dictionary Through Deleting](https://leetcode.com/problems/longest-word-in-dictionary-through-deleting/)

**Core pattern**: Trie optimization

**Hint**: Build Trie from sorted words (shorter first), find longest valid path

**Expected time complexity**: O(N×L) where N is number of words

### Hard 1: Implement Magic Dictionary
**Problem**: Build a dictionary and check if a word matches except for exactly one character change.

**LeetCode**: [676. Implement Magic Dictionary](https://leetcode.com/problems/implement-magic-dictionary/)

**Core pattern**: Trie with flexible matching

**Hint**: For each word, check all possible single-character variations

**Expected time complexity**: O(N×L) where N is word length

### Hard 2: Shortest and Longest Paths in a Grid with Obstacles Elimination
**Problem**: Find shortest path in grid with obstacle elimination capabilities.

**LeetCode**: [1293. Shortest Path in a Grid with Obstacles Elimination](https://leetcode.com/problems/shortest-path-in-a-grid-with-obstacles-elimination/)

**Core pattern**: BFS + Trie optimization for state tracking

**Hint**: Use Trie to avoid revisiting same state (position + remaining eliminations)

**Expected time complexity**: O(m×n×k) where k is max eliminations allowed

---

**Next steps**: After completing Tries, move to topic 13: Graphs**

Do you want:

a) more practice problems,

b) a detailed solution for one problem,

c) a quiz to test my understanding,

or the next topic?

---

