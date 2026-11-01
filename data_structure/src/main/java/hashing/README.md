# Hashing: HashMap and HashSet

## Concept Explanation

### What is a Hash Map?
A hash map is a data structure that maps keys to values using a hash function. It provides O(1) average-time complexity for insertion, deletion, and lookup operations, making it one of the most efficient general-purpose data structures.

**Key characteristics:**
- **Key-Value pairs**: Each entry consists of a key and its associated value
- **Hash function**: Computes an index into the bucket array to locate the entry
- **Collision handling**: When two keys hash to same slot (e.g., Java HashMap uses LinkedHashMap for default; Kotlin `mutableMapOf` uses HashMap internally)
- **Load factor**: Ratio of entries to buckets; resizing triggers when too many collisions occur

### What is a HashSet?
A HashSet stores unique elements without associated values. Operations have the same time complexity as HashMap but without value storage. Many problems require only uniqueness tracking, so HashSet is preferred over HashMap when values are irrelevant.

### Hash Function
The core of any hash map: a function that maps input to array index. Good properties:
1. **Uniform distribution**: Distributes keys evenly across buckets
2. **Deterministic**: Same key always produces same index
3. **Fast**: O(1) computation
4. **Efficiently handles collisions**: Via separate chaining (linked list per bucket) or open addressing

### Real-Life Analogy
Think of a **library book catalog system**:
- Keys are book titles/call numbers
- Values are books' locations on shelves
- Ask librarian: "Where is book X?" → O(1) lookup after catalog indexed by call number
- Multiple books may share a shelf (collision) — handled by cataloging system efficiently

### Time and Space Complexity

| Operation | Average | Worst | Notes |
|-----------|---------|-------|-------|
| Insert | O(1) | O(N) | Worst when many collisions → degrade to linked list search |
| Delete | O(1) | O(N) | Same collision consideration |
| Contains / Get | O(1) | O(N) | Look up by key |
| Size | O(1) | O(1) | Constant time |
| Iterator | O(N) | O(N) | Traverse all entries |

**Worst case**: All N keys collide in one bucket → essentially a linked list → O(N) per operation.
**Average case**: Uniform hash distribution → O(1) per operation.

### Common Mistakes and Edge Cases

1. **Null keys**: HashMap allows exactly ONE null key; HashSet does not allow null at all.
2. **Default hashCode**: Always override `hashCode()` when using custom objects as keys. Otherwise all identical objects compare equal!
3. **Equals mismatch**: If `equals()` returns true but `hashCode()` differs, inconsistent behavior. Must keep them consistent.
4. **Modifying during iteration**: Changing map while iterating throws `ConcurrentModificationException`. Use `Iterator` with `hasNext()` / `next()`, or collect results first.
5. **Unbounded growth**: Forgetting to handle null elements properly. Java HashMap allows null but Kotlin's `mutableMapOf` doesn't (only with empty key allowed).
6. **Not using libraries**: In Kotlin prefer `mutableMapOf`, `groupBy`, `countBy` instead of manual loops.
7. **Key mutability issues**: Modifying key after insertion can corrupt internal state. Keys should be effectively immutable.
8. **Integer overflow as hash key**: Using `key % n` where key can be negative or large — modulo can produce unexpected negatives. Use `key & 0x7FFFFFFF` or `Math.abs(key)`.
9. **Large number of iterations**: Simple `for (it in map)` pattern fails on modifications. Use explicit `iterator()` calls.

### When to Use Hash Map vs Hash Set in Interviews

| Scenario | Data Structure | Why |
|----------|---------------|-----|
| Counting occurrences | HashMap | Need values |
| Checking duplicates | HashSet | Only need membership test |
| Frequency analysis | HashMap | Track counts |
| Pairs with sum X | HashMap | Store seen numbers, check complement |
| Top K frequent items | HashMap + Heap | Track counts + track top K |
| Anagram checking | HashSet or Sort+compare | Need uniqueness or sort comparison |

## Important Patterns

### 1. Two Sum Pattern (Brute → Better → Optimal)
- **Brute**: Nested loops checking all pairs → O(N²)
- **Better**: Sort first, then two pointers → O(N log N)
- **Optimal**: Single pass with HashMap storing num→index → O(N), O(N) space
  - Key insight: for each element nums[i], check if target - nums[i] already exists

### 2. Frequency Tracking Pattern
- Count each element with one pass → O(N)
- UseHashMap<T, Int>() populated in single loop
- Then apply filtering/sorting on top of counts

### 3. First Non-Repeating Element Pattern
- Single pass to count frequency → O(N)
- Second pass to find first element with freq == 1 → O(N)
- Total: O(N) time, O(N) space

### 4. Majority Element Pattern
- Elements appearing > n/3 times → must exist among any three candidates
- Use HashMap but cap frequency at threshold, or use Boyer-Moore style three-candidate approach

### 5. Sliding Window with Hash Set
- Maintain set of characters in current window
- Add right char when expanding; remove left char when shrinking
- Check for duplicates efficiently: O(1) per operation

## Dry Run

### Example Problem: Two Sum

**Input**: `nums = [2, 7, 11, 15], target = 9`

**Brute force dry run**:
```
i=0, j=1: 2+7=9 == target → return [0,1]  ✓
Time: O(N²), Space: O(1)
```

**Better approach (sort + two pointers)**:
```
Sort: [2, 7, 11, 15]
left=0 (value 2), right=3 (value 15): sum=17 > 9 → right--
right=2 (value 11): sum=13 > 9 → right--
right=1 (value 7): sum=9 == 9 → found! But indices... solution needs original indices.
```
Wait — sorting loses original indices. This proves why optimal approach uses HashMap.

**Optimal HashMap dry run**:
```
Step 0: i=0, val=2, complement=9-2=7, 7 not in map yet → map={2:0}
Step 1: i=1, val=7, complement=9-7=2, 2 IS in map at index 0 → return [0,1]  ✓

HashMap state progression:
  Step 0: map = {2 → 0}
  Step 1: checked, found complement at index 0
```

Result: Indices [0, 1]

## Code Template

```kotlin
package hashing

/**
 * Template for hash map / hash set problems in Kotlin
 */
fun solution(...): Any {
    // TODO: Implement
}

// UTILITY: Generic count function
fun countBy(list: List<Int>, key: (Int) -> Int): Map<Int, Int> {
    return list.groupBy { key(it) }.mapValues { it.value.size }
}

// UTILITY: Generic grouping
fun groupByKey(list: List<Int>, key: (Int) -> String): Map<String, List<Int>> {
    return list.groupBy { key(it) }
}
```

### Line-by-Line Explanation

```kotlin
package hashing              // 1: Package declaration

/**
 * Template for hash map / hash set problems in Kotlin        // 2: Function template
 */
fun solution(...) {           // 3: Function signature
    // TODO placeholder                                              // 4: Implementation placeholder
}                              // 5: End of function

// UTILITY: Generic count function                                     // 6: Utility function annotation
fun countBy(list: List<Int>, key: (Int) -> Int): Map<Int, Int> {       // 7: Function definition
    return list.groupBy { key(it) }                                   // 8: Group by key function
            .mapValues { it.value.size }                             // 9: Map to counts
}                                                                      // 10: Return counts

// UTILITY: Generic grouping                                         // 11: Utility function annotation
fun groupByKey(list: List<Int>, key: (Int) -> String): Map<String, List<Int>> { // 12: Function definition
    return list.groupBy { key(it) }                                   // 13: Group values by key
              // Result: Map of key → list of values                  // 14: Return grouped result
}                                                                         // 15: End of function
```

### Complexity Analysis (Template)
- **Time**: Depends on algorithm (O(N) single pass, O(N²) brute pair, etc.)
- **Space**: O(N) for storing all unique elements in hash map

## Practice Problems

### Easy Problems

#### 1. **Contains Duplicate**
- **LeetCode**: https://leetcode.com/problems/contains-duplicate/
- **Difficulty**: Easy
- **Core pattern**: HashSet
- **Approach hint**: For each element, check if it already exists in a HashSet. If yes, duplicate found. Insert every new element into the set.
- **Expected time**: O(N), **space**: O(N)

#### 2. **Find the Duplicate Number**
- **LeetCode**: https://leetcode.com/problems/find-the-duplicate-number/
- **Difficulty**: Easy
- **Core pattern**: HashMap counting / Floyd's cycle detection
- **Approach hint**: 
  - Option A: Count frequencies using HashMap, return any count > 1
  - Option B: Floyd's tortoise & hare cycle detection on the array (treat as linked list via index→value mapping) → O(N) time, O(1) space
- **Expected time**: O(N), **space**: O(N) or O(1) (Floyd's)

### Medium Problems

#### 3. **Majority Element II** (appears > n/3 times)
- **LeetCode**: https://leetcode.com/problems/majority-element-ii/
- **Difficulty**: Medium
- **Core pattern**: Three candidate tracking
- **Approach hint**: Elements appearing > n/3 times must exist among at most 3 candidates. Track up to 3 candidates simultaneously with counting in a HashMap that caps at threshold n/3.
- **Expected time**: O(N), **space**: O(3) = O(1)

#### 4. **Remove Duplicates from Sorted Array II**
- **LeetCode**: https://leetcode.com/problems/remove-duplicates-from-sorted-array-ii/
- **Difficulty**: Medium
- **Core pattern**: Sliding window with distinct tracking
- **Approach hint**: Similar to removeDuplicates but allows repeated elements (just need distinct count). Use slow/fast pointers, but only advance fast past duplicates of slow.
- **Expected time**: O(N), **space**: O(1)

### Hard Problems

#### 5. **Longest Consecutive Sequence**
- **LeetCode**: https://leetcode.com/problems/longest-consecutive-sequence/
- **Difficulty**: Medium (interview difficulty varies)
- **Core pattern**: HashSet for O(1) lookup of consecutive numbers
- **Approach hint**: Put all numbers in HashSet for O(1) lookup. For each number, only start counting consecutive sequence if number-1 not in set (avoids re-checking shorter substrings). Total time O(N).
- **Expected time**: O(N), **space**: O(N)

#### 6. **Reorder Data in Binary Tree From Inorder to Preorder**
- **LeetCode**: https://leetcode.com/problems/reorder-data-in-binary-tree-from-inorder-to-preorder/
- **Difficulty**: Hard
- **Core pattern**: Preorder gives structure, inorder gives position; combine them
- **Approach hint**: Preorder sequence tells you root-left-right order. Inorder sequence tells you left-root-right position relative to other nodes. Recursively build tree: for preorder, first node is root; split inorder into left/right subtrees; recurse on each.
- **Expected time**: O(N²) naive / O(N log N) balanced / O(N) sorted BST building, **space**: O(N)

---
*End of Hashing topic*