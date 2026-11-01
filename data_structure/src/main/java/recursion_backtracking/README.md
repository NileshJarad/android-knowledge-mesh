# Recursion and Backtracking

## Concept Explanation

### What is Recursion?
Recursion is a programming technique where a function calls itself to solve smaller instances of the same problem. Each recursive call solves a smaller subproblem, building toward a base case.

**Core components:**
1. **Base case**: Smallest subproblem that can be solved directly without further recursion (stops infinite recursion)
2. **Recursive case**: Break problem into smaller pieces and call the same function on them
3. **Progress toward base case**: Each recursive call must make progress — reduce input size, move toward base case

**Example**: Computing factorial `n! = n × (n-1)!`, with base case `0! = 1`

### What is Backtracking?
Backtracking is a form of recursion specifically used for searching/enumeration problems. It tries options sequentially; when it hits dead ends or exceeds constraints, it "backtracks" (undoes choices) and tries another option.

**Characteristics:**
- **Decision tree exploration**: Each node represents a choice; each edge represents a choice made
- **Constraint checking**: Prune branches that violate constraints early
- **Undo operations**: Before trying next choice, undo previous choice
- **Counting / generating**: Often counts valid solutions rather than listing them all

**When to use:**
- Permutations/combinations generation
- N-Queens placement
- Sudoku solving
- Subset sum enumeration
- Path finding in graphs with constraints
- Validating configurations

### How Recursion Works Internally
```
Factorial(3):
  3 × Factorial(2)
  = 3 × (2 × Factorial(1))
  = 3 × (2 × (1 × Factorial(0)))
  = 3 × (2 × (1 × 1))  [base case]
  = 6
```
Call stack grows linearly with depth: O(depth) space, O(1) per level of computation.

### Real-Life Analogy
Think of a **tower of Hanoi**: you have disks on one peg, need to move to another peg following rules. At each step, choose which peg to move the top disk from. If you make wrong choice, start over with fewer disks — "backtrack".

### Time and Space Complexity

| Operation | Time | Space | Notes |
|-----------|------|-------|-------|
| Simple recursion (fibonacci, factorial) | O(N) | O(N) stack depth | Memoization optimization O(1) |
| Permutation enumeration | O(N!) | O(N) stack + output | Use pruning |
| Subset enumeration | O(2^N) | O(N) stack | Exponential, unavoidable |
| Backtracking with pruning | varies | varies | Depends on search space size |
| Fibonacci memoized | O(N) | O(N) | Or O(1) state if bottom-up DP |

**Critical insight**: Naive recursive fibonacci = O(2^N). With memoization = O(N). Bottom-up DP = O(N).

### Common Mistakes and Edge Cases

1. **Missing base case**: Causes stack overflow → no output ever computed
2. **Infinite recursion**: Base case not reached due to wrong termination condition
3. **Not making progress**: Same input every call → infinite loop
4. **Not memoizing**: For repeated subproblems, exponential blowup
5. **Passing mutable state by reference**: Leads to unexpected side effects across calls
6. **Large recursion depth**: Stack overflow for deep recursion (e.g., N=10000+ in Java)
7. **Not undoing choices in backtracking**: Leads to incorrect results because state remains dirty
8. **Off-by-one in array indexing**: In permutation generation, handle position boundaries correctly
9. **Forgetting backtrack step**: After setting `path[pos] = -1` to mark "visited", forgot to restore before next iteration
10. **Wrong pruning condition**: Prune too early or too late affects correctness
11. **Global variables in recursion**: State leaks across calls, makes testing hard
12. **Integer overflow in recursive computations**: Use long for large multiplications

### When to Use Recursion vs Iteration in Interviews

| Scenario | Approach | Why |
|----------|----------|-----|
| Tree/graph traversal | Recursion | Natural fit; O(depth) stack |
| Divide & conquer (merge sort, binary search) | Recursion | Divide naturally |
| Backtracking problems | Recursion | Decision tree patterns emerge naturally |
| Simple counting | Usually better iterative | Less overhead than call stack |
| Very deep recursion (>1000) | Iteration with manual stack | Avoids stack overflow |
| Finding max/min | Either works | Simpler iterative |

**Rule of thumb**: Use recursion when problem structure matches recursive definition (trees, divide-and-conquer); use iteration otherwise.

## Important Patterns

### 1. Classic Recursive Pattern
```
fun func(n):
    if (baseCase): return result
    return combine(func(smaller), func(remaining))
```
Fibonacci, factorial, permutations, combinations.

### 2. Backtracking Pattern
```
function backtrack(state):
    if (valid complete state): record solution
    for each possible choice:
        make choice (update state)
        backtrack()  // undo choice
```
Used for: permutations, subsets, N-Queens, sudoku.

### 3. Top-Down DP with Memoization
- Same as classic recursion but cache results
- Reduces exponential to linear
- Transform classic recursion into memoized version
- Less intuitive but simpler code initially

### 4. DFS with Constraint Checking
- Traverse nodes with explicit checks to prune invalid paths early
- Add conditions like "must contain certain properties" before exploring children

### 5. BFS vs DFS Equivalence for Certain Problems
- Some problems express as either DFS or BFS naturally
- DFS preferred for path existence with backtracking
- BFS preferred for shortest paths in unweighted graphs

### 6. Subset Sum / Knapsack Backtracking
- Explore including element i (add value[i] to current sum, move to i+1)
- Explore excluding element i (don't add value[i], stay at i+1)
- Early pruning: if current sum exceeds capacity and values positive, skip

## Dry Run

### Example: Permutations of [1, 2, 3]

```
Permute(index=0, [1,2,3]):
  Try include 1 at pos 0: state=[1,_,_]
    Permute(index=1, [1,2,_]):
      Try include 2 at pos 1: state=[1,2,_]
        Permute(index=2, [1,2,3]):
          Include 3 at pos 2: state=[1,2,3], record "123"
      Try exclude 2 at pos 1: state=[1,_,_]
        Permute(index=1, [1,2,3]):  // wait this needs to consider remaining elements
```
Actually proper algorithm: iterate over available positions, at each position try each unused element. Let me trace properly:

Standard backtracking for permutations:
```
build([], [], {used={}})
  if length == n: add to results
  for i in 0..n-1:
    if i not in used:
      mark i used
      build with position i filled by nums[i]
      unmark i
```

Trace for [1,2,3]:
- build(): position 0 empty, available = {0,1,2}
  - pick pos 0 = 1: state [1,_,_], used={0}
    - build pos 1, avail={1,2}
      - pick pos 1 = 2: state [1,2,_], used={0,1}
        - build pos 2, avail={2}
          - pick pos 2 = 3: state [1,2,3], record "123"
      - pick pos 1 = 3: state [1,3,_], used={0,2}
        - build pos 2, avail={1}
          - pick pos 2 = 2: state [1,3,2], record "132"
  - pick pos 0 = 2: state [2,_,_], used={1}
    - ... produces "213", "231"
  - pick pos 0 = 3: ... produces "312", "321"

Result: ["123", "132", "213", "231", "312", "321"]

## Code Template

```kotlin
package recursion_backtracking

/**
 * Template for recursion/backtracking problems in Kotlin
 */

// Classic recursion template
fun classicRecursive(n: Int): Int {
    if (n <= 1) return n  // base case
    return classicRecursive(n - 1) + classicRecursive(n - 2)  // recursive case
}

// Memoized recursion template
import java.util.concurrent.ConcurrentHashMap

fun memoizedRecursive(n: Int): Int {
    val memo = ConcurrentHashMap<Int, Int>()
    fun helper(k: Int): Int {
        if (k <= 1) return k
        memo.getOrPut(k) { helper(k - 1) + helper(k - 2) }
    }
    return helper(n)
}

// Backtracking template
fun backtrackTemplate(n: Int): List<String> {
    val result = mutableListOf<String>()
    val path = mutableListOf<Int>()
    val used = BooleanArray(n)
    helper(0, n, path, used, result)
    return result
}

private fun helper(
    position: Int,
    n: Int,
    path: MutableList<Int>,
    used: BooleanArray,
    result: MutableList<String>
) {
    if (position == n) {
        result.add(path.joinToString(""))
        return
    }
    for (i in 0 until n) {
        if (!used[i]) {
            used[i] = true
            path.add(i)
            helper(position + 1, n, path, used, result)
            path.removeAt(path.lastIndex)
            used[i] = false
        }
    }
}
```

### Complexity Analysis (Template)
- **Time**: Determined by number of leaves (exponential worst case) or constraint-optimized
- **Space**: O(depth) for call stack, plus O(output size) for storing results

## Practice Problems

### Easy Problems

#### 1. **Climbing Stairs**
- **LeetCode**: https://leetcode.com/problems/climbing-stairs/
- **Difficulty**: Easy
- **Core pattern**: Classic recursion with memoization
- **Approach hint**: You can climb 1 or 2 steps at a time. f(n) = f(n-1) + f(n-2), base cases f(1)=1, f(2)=2. Either memorize with DP or do iterative. Either approach validates understanding recursion.
- **Expected time**: O(N), **space**: O(N) memo or O(1) iterative

#### 2. **Reverse a Linked List**
- **LeetCode**: https://leetcode.com/problems/reverse-linked-list/
- **Difficulty**: Easy
- **Core pattern**: Recursive reversal using call stack
- **Approach hint**: Recursively reverse the rest of list first (`reverseRest`), then swap head's next with null and tail's next with new head. Or simply iterate with prev/curr pointers.
- **Expected time**: O(N), **space**: O(N) recursive or O(1) iterative

### Medium Problems

#### 3. **Permutations**
- **LeetCode**: https://leetcode.com/problems/permutations/
- **Difficulty**: Medium
- **Core pattern**: Backtracking with "used" tracking
- **Approach hint**: For each position, try each unused element. Track used array, track current permutation being built. Add permutation to results when length equals input size.
- **Expected time**: O(N!), **space**: O(N) for recursion + results

#### 4. **Combination Sum**
- **LeetCode**: https://leetcode.com/problems/combination-sum/
- **Difficulty**: Medium
- **Core pattern**: Backtracking with constraint (can reuse elements, don't reorder)
- **Approach hint**: For each candidate, recursively try adding it (since elements can repeat), then skip to next candidate (no repetition across different candidates). Early prune when sum exceeds target.
- **Expected time**: O(2^N), **space**: O(N) recursion depth

### Hard Problems

#### 5. **N-Queens II (count distinct solutions)**
- **LeetCode**: https://leetcode.com/problems/n-queens-ii/
- **Difficulty**: Hard
- **Core pattern**: Backtracking with column/diagonal constraints
- **Approach hint**: Place queens row by row. Track occupied columns, upper diagonal (`row - col`), lower diagonal (`row + col`). Prune when these conflicts exist. Count solutions when complete board placed.
- **Expected time**: O(N!), **space**: O(N) constraints tracking

#### 6. **Combination Sum III (k different numbers with sum == target)**
- **LeetCode**: https://leetcode.com/problems/combination-sum-iii/
- **Difficulty**: Medium (often appeared in Hard variant interviews)
- **Core pattern**: Backtracking + pruning based on remaining count
- **Approach hint**: Sort candidates by value (to enable efficient pruning). Recursively explore combinations, track how many picked so far, early stop when cannot reach target within remaining count.
- **Expected time**: O(C(n, k)) typically exponential in worst case but bounded by combination count
- **Space**: O(k) recursion depth

---
*End of Recursion and Backtracking topic*