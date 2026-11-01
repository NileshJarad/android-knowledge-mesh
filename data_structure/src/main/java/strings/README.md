# Strings

## Concept Explanation

### What is a String?
A string is a sequence of characters (characters from alphabet, digit, or any Unicode). Strings are the most common data structure in programming and are fundamental to many algorithms. 

**Key characteristics:**
- **Immutable**: In most languages (Java, Kotlin), strings are immutable — you can't change existing characters, only create new strings. This matters for performance when repeatedly modifying string content.
- **Ordered**: Each character has a position (index) starting from 0.
- **Indexed access**: `str[i]` gives the character at position i.

### String Internals
- **Character encoding**: UTF-8 (variable width), UTF-16 (used by Java/Kotlin), UTF-32
- **String comparison**: Lexicographic order based on Unicode values
- **Equality**: `==` compares memory references in Java, `equals()` compares contents in Kotlin
- **Length**: O(1) operation in most languages since length stored separately

### Real-Life Analogy
Think of a string like a **train of words on a conveyor belt**:
- Each word is a "character slot" at position 0, 1, 2, ...
- You read them left to right (left to right traversal)
- Adding a word at the end requires a new belt section (immutable)
- Comparing two trains means comparing them slot by slot

### Time and Space Complexity

| Operation | Complexity | Notes |
|-----------|------------|-------|
| Access (`charAt`) | O(1) | Direct index access (Java char[], Kotlin Array<Char>) |
| Length | O(1) | Stored internally |
| Substring extraction | O(1)-O(n) | Depends on language; creates new object |
| Character search | O(n) | Linear scan |
| Reverse | O(n) | Single pass swap |
| Check palindrome | O(n) | Read both ends toward center |
| Anagram check | O(n log n) sort / O(n) hashmap | |
| Regex match | O(n) average | Depends on pattern complexity |

### Common Mistakes and Edge Cases

1. **Immutability confusion**: Treating `++` on strings as modifying original (doesn't work in Java/Kotlin)
   - Solution: Use StringBuilder/MutableStringBuilder for modifications
   
2. **Index out of bounds**: `charAt(-1)` or beyond length throws exception
3. **Case sensitivity**: 'A' vs 'a', 'Z' vs 'z' — check if problem says case-insensitive
4. **Leading/trailing whitespace**: `trim()`, `strip()`, or explicit checks
5. **Unicode surrogate pairs**: Characters outside BMP (emoji, certain CJK) take 2 chars
6. **Regex special characters**: `.`, `*`, `+`, `?` etc. have special meaning in regex patterns
7. **Length confusion**: In Java use `.length`, in Kotlin use `.length` (same)
8. **Empty vs null**: Always handle `""` (empty) and may have `null` depending on language

### When to Use Strings in Interviews

| Scenario | Use Approach |
|----------|-------------|
| Reading input | `readLine()` / Scanner |
| Pattern matching | Regex or manual scan |
| String reversal | Recursion or iteration |
| Palindrome checking | Two pointers |
| Anagram detection | Sort + compare or hashmap |
| Substring search | KMP, Rabin-Karp, or built-in `contains` |
| Extraction substrings | Sliding window or index math |
| Frequency counting | Hashmap per character |

## Important Patterns

### 1. Two Pointers (Characters at ends of string)
- Compare characters from both ends moving inward
- Classic for palindrome checking, removing letters from both ends

### 2. Sliding Window (Substrings)
- Find substring satisfying condition with max/min length
- Expand right, shrink left based on condition
- Great for: longest substring without repeating chars, min window substring

### 3. Hash Map (Character frequency)
- Count frequency of each character for anagram problems
- Track last occurrence index to find first non-repeating character
- Key insight: characters in alphabet = 26 for lowercase English

### 4. KMP / Rabin-Karp Algorithm
- Find pattern in text efficiently (avoiding naive O(n×m))
- KMP: O(n + m) time using prefix function
- Rabin-Karp: O(n + m) expected average with rolling hash

### 5. String Splitting
- Split by delimiter: `substringBefore`, `substringAfter`, `split`
- Regex `split`: `Pattern.compile(...).matcher(str).results().map { it.value }`

### 6. StringBuilder Pattern
- For repeated concatenation: use `StringBuilder` instead of `+` or `+=`
- Why? `"a" + "b"` creates new String each time → O(n²); `StringBuilder` → O(n)

## Dry Run

### Example Problem: Longest Substring Without Repeating Characters

**Input**: `"abcabcbb"`

**Goal**: Find longest substring without repeating characters

**Brute force dry run** — check every possible substring:
```
Consider "abc": no repeats → len=3, max=3
Consider "abca": has repeat 'a' → skip
Consider "bca": no repeats → len=3, max=3  
...many more...
```
This would be O(n³) — too slow.

**Sliding window approach dry run:**

| Step | left | right | window | unique count | maxLen | Action |
|------|------|-------|--------|---------------|--------|---------|
| 0 | 0 | 0 | "a" | 1 | 1 | Add 'a' |
| 1 | 0 | 1 | "ab" | 2 | 2 | Add 'b' |
| 2 | 0 | 2 | "abc" | 3 | 3 | Add 'c' |
| 3 | 0 | 3 | "abca" | 3 | 3 | 'a' repeats → shrink left |
| 4 | 1 | 3 | "bca" | 3 | 3 | left=1, remove 'a' |
| 5 | 1 | 4 | "bcab" | 3 | 3 | 'b' repeats → shrink |
| 6 | 2 | 4 | "cab" | 3 | 3 | left=2 |
| 7 | 2 | 5 | "cabc" | 3 | 3 | 'c' repeats |
| 8 | 3 | 5 | "abc" | 3 | 3 | left=3 |
| 9 | 3 | 6 | "bc" | 2 | 3 | left=3, add 'b'? No, skip until valid |
| ... | ... | ... | ... | ... | 3 | Continue |
| Final | - | - | - | - | 3 | Window never exceeds 3 |

Wait, let me redo this more carefully with actual indices:

**Window dry run** for `"abcabcbb"`:

```
left=0, right=0: "a" → map={a:0}, maxLen=1, unique=1
right=1: "ab" → map={a:0, b:1}, maxLen=2, unique=2
right=2: "abc" → map={a:0,b:1,c:2}, maxLen=3, unique=3
right=3: "abca" → 'a' at 0 already exists, move left to 1
         "bcb"? No wait. left moves past duplicate 'a': left=1
         window="bca", unique=3, maxLen stays 3
right=4: "bcab" → 'b' at 1 exists, left=2, window="cab", unique=3, maxLen=3
right=5: "cabc" → 'c' at 2 exists, left=3, window="abc", unique=3, maxLen=3
right=6: "abcb" → 'b' at 4 exists, left=4, window="cbc", unique=2, maxLen=3
right=7: "bcbb" → 'b' repeats multiple times
         left keeps moving: left=5, then left=6, left=7
         final window="bb", unique=1, maxLen=3
```

Final answer: **3** (substring "abc")

## Code Template

```kotlin
package strings

/**
 * Template for string problems in Kotlin
 */
fun solution(input: String, params: Any): Any {
    // TODO: Implement algorithm
}

// UTILITY: Convert to uppercase/lowercase
fun normalize(s: String): String {
    return s.uppercase() // Kotlin 1.5+
    // OR s.trim().uppercase() for trimmed uppercased string
}

// UTILITY: Check if palindrome
fun isPalindrome(s: String): Boolean {
    var left = 0
    var right = s.length - 1
    while (left < right) {
        if (s[left] != s[right]) return false
        left++
        right--
    }
    return true
}

// UTILITY: Split by delimiter
fun splitByDelimiter(input: String, delimiter: Char): List<String> {
    return input.split(delimiter)
}
```

### Line-by-Line Explanation

```kotlin
package strings              // 1: Package declaration

/**
 * Template for string problems in Kotlin                    // 2: Function template
 * Comment block explaining template purpose
 */
fun solution(input: String, params: Any): Any {             // 3: Function signature
    // TODO: Implement algorithm here                         // 4: Placeholder for implementation
}                                                          // 5: End of function

// UTILITY: Normalize string                                  // 6: Utility function
fun normalize(s: String): String {                          // 7: Function definition
    return s.uppercase()                                    // 8: Convert all letters to uppercase
    // Alternative: s.trim().uppercase()                    // 9: Trim then uppercase (handles whitespace)
}                                                            // 10: Return normalized string

// UTILITY: Check palindrome                                  // 11: Utility function
fun isPalindrome(s: String): Boolean {                      // 12: Function definition
    var left = 0                                             // 13: Left pointer starts at beginning
    var right = s.length - 1                                 // 14: Right pointer at end
    while (left < right) {                                    // 15: Continue while they haven't crossed
        if (s[left] != s[right]) return false                // 16: Mismatch found - not palindrome
        left++                                                // 17: Move left pointer inward
        right--                                               // 18: Move right pointer inward
    }
    return true                                               // 19: All matches = palindrome
}                                                            // 20: End of function

// UTILITY: Split by single character delimiter                // 21: Utility function
fun splitByDelimiter(input: String, delimiter: Char): List<String> { // 22: Function definition
    return input.split(delimiter)                             // 23: Kotlin built-in split function
}                                                            // 24: Return list of substrings
```

### Complexity Analysis (Template)
- **Time complexity**: Depends on algorithm (O(n), O(n log n), etc.)
- **Space complexity**: 
  - O(1) for pure character operations (palindrome check)
  - O(n) for hash maps storing character frequencies
  - O(n) for substring creation (new String objects)

## Practice Problems

### Easy Problems

#### 1. **Valid Palindrome**
- **LeetCode**: https://leetcode.com/problems/valid-palindrome/
- **Difficulty**: Easy
- **Core pattern**: Two pointers (characters from both ends)
- **Approach hint**: Use two pointers, one at start, one at end. Skip non-alphanumeric characters on either side. Compare characters case-insensitively. If all pairs match, it's a palindrome.
- **Expected time**: O(n), **space**: O(1)

#### 2. **Longest Substring Without Repeating Characters**
- **LeetCode**: https://leetcode.com/problems/longest-substring-without-repeating-characters/
- **Difficulty**: Easy
- **Core pattern**: Sliding window with hash set
- **Approach hint**: Use sliding window maintaining uniqueness. Expand right pointer, when duplicate found, shrink left pointer until no duplicates. Track max window size.
- **Expected time**: O(n), **space**: O(min(m,n)) where m is charset size

### Medium Problems

#### 3. **Find First and Last Position of Non-Repeating Character**
- **LeetCode**: https://leetcode.com/problems/first-unique-character-in-a-string/
- **Difficulty**: Medium
- **Core pattern**: Hash map with first/last occurrence tracking
- **Approach hint**: Count frequency of each char. Then scan again finding the first char with count == 1. If none, return [-1,-1].
- **Expected time**: O(n), **space**: O(n)

#### 4. **Add Binary**
- **LeetCode**: https://leetcode.com/problems/add-two-numbers/
- **Difficulty**: Medium
- **Core pattern**: Two pointers on reversed strings (little-endian bit addition)
- **Approach hint**: Process digits from right (least significant) to left (most significant). Carry propagates leftward. Reverse result at end.
- **Expected time**: O(max(len1, len2)), **space**: O(max(len1,len2))

### Hard Problems

#### 5. **Minimum Window Substring**
- **LeetCode**: https://leetcode.com/problems/minimum-window-substring/
- **Difficulty**: Hard
- **Core pattern**: Sliding window + hash map
- **Approach hint**: Use sliding window with frequency map. Expand right until all required chars are found (window contains everything needed). Then shrink left while window still contains required characters. Track minimum window size found.
- **Expected time**: O(n), **space**: O(charset size)

#### 6. **Regular Expression Matching**
- **LeetCode**: https://leetcode.com/problems/regular-expression-matching/
- **Difficulty**: Hard
- **Core pattern**: DP or memoized recursion with regex semantics
- **Approach hint**: Build a DP table dp[i][j] meaning "does regex pattern[0..i-1] match string s[0..j-1]". Handle '.' (match any single char) and '*' (zero or more of previous char).
- **Expected time**: O(n × m) or O(n × m) with memoization, **space**: O(n × m)

---
*End of Strings topic*