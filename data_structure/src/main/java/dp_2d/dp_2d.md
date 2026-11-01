# Topic 16: 2-D Dynamic Programming

## Concept Explanation

### What is 2-D Dynamic Programming?
2-D Dynamic Programming extends 1-D DP to handle problems with two dimensions (matrix/grid). The DP state is represented as `dp[i][j]` where both `i` and `j` indices matter. The recurrence relation typically depends on values from adjacent cells (left, top, top-left, top-right).

### How it works internally
1. **Define the state**: `dp[i][j]` typically represents some property of subgrid ending at `(i,j)` or subproblem starting from `(i,j)`
2. **Identify dependencies**: Which neighboring cells does `dp[i][j]` depend on? (typically `dp[i-1][j]`, `dp[i][j-1]`, `dp[i-1][j-1]`)
3. **Determine iteration order**: Must process cells in an order that ensures dependencies are computed first (row-by-row, column-by-column, or diagonal-by-diagonal)
4. **Define base cases**: First row, first column, or corner cases
5. **Extract final answer**: Usually `dp[m-1][n-1]` or max/min over the entire DP table

### Real-life analogy
**Maze navigation**: Finding the shortest path through a grid from top-left to bottom-right. Each cell might have obstacles, costs, or different movement rules. The DP table stores the best path value to reach each cell, building from the start.

### Time and space complexity for every major operation
- **Time**: O(m × n) to process all cells in an m×n grid
- **Space**: O(m × n) for full DP table; can often reduce to O(n) or O(min(m,n)) if only need current/previous row

### Common mistakes and edge cases
- **Wrong iteration order**: Processing cells before their dependencies are computed
- **Missing boundary conditions**: First row/column special handling
- **Confusing directions**: Mixing up row vs column dependencies
- **Not considering all neighbors**: Forgetting diagonal dependencies when needed
- **Space optimization errors**: Reducing to 1D array loses necessary information

### When to use it in interviews
- Grid path problems (shortest path, maximum path, number of paths)
- Matrix problems with overlapping subproblems
- Problems where state depends on both row and column position
- Dynamic obstacles or varying cell costs

## 2. Important Patterns

### Pattern 1: Path Counting (Unique Paths)
**Example**: Unique Paths (LeetCode 62)
```kotlin
dp[i][j] = dp[i-1][j] + dp[i][j-1]  // from top + from left
```
**Brute force**: Recursion with backtracking (exponential)
**Better**: DP with O(m×n) time, O(m×n) space
**Optimal**: O(m×n) time, O(min(m,n)) space (1D array optimization)

### Pattern 2: Maximum/Minimum Path Sum
**Example**: Minimum Path Sum (LeetCode 64)
```kotlin
dp[i][j] = min(dp[i-1][j], dp[i][j-1]) + grid[i][j]
```
**Brute force**: Try all paths 2^(m+n)
**Better**: DP O(m×n) time
**Optimal**: Same as better - can modify grid in-place for O(1) extra space

### Pattern 3: Largest Square/Rectangle
**Example**: Maximal Square (LeetCode 221)
```kotlin
if (matrix[i][j] == '1') {
    dp[i][j] = min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]) + 1
}
```
**Brute force**: Check all squares O(m×n×min(m,n))
**Better**: DP O(m×n) time
**Optimal**: Same as better - clever use of previously computed squares

### Pattern 4: Edit Distance on Grid
**Example**: Edit Distance (LeetCode 72 - 2D version for matrix alignment)
```kotlin
if (matrix1[i][j] == matrix2[i][j]) {
    dp[i][j] = dp[i-1][j-1]
} else {
    dp[i][j] = 1 + min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
}
```
**Brute force**: All possible edit sequences
**Better**: DP O(m×n) time, O(m×n) space
**Optimal**: Can reduce space to O(min(m,n)) with careful ordering

### Pattern 5: Island Problems (Connected Components)
**Example**: Number of Islands, Max Area of Island
```kotlin
// DFS/BFS with DP/memoization to avoid recomputation
dp[i][j] = 1 + dp[i+1][j] + dp[i-1][j] + dp[i][j+1] + dp[i][j-1]  // with bounds checking
```
**Brute force**: Re-explore from same cells
**Better**: Visited tracking + memoization
**Optimal**: Same - flood fill with state tracking

## 3. Dry Run

**Problem**: Minimum Path Sum
**Input**: 
```
grid = [
  [1, 3, 1],
  [1, 5, 1],
  [4, 2, 1]
]
```
**Expected Output**: 7 (path: 1 → 3 → 1 → 1 → 1)

**Algorithm**: dp[i][j] = min(dp[i-1][j], dp[i][j-1]) + grid[i][j]

**Step-by-Step** (with space optimization using 1D array):

```
Initialize dp array with grid[0][0] = 1

Row 0 (first row - only can come from left):
dp[0] = 1          // grid[0][0]
dp[1] = dp[0] + 3 = 4  // grid[0][1]
dp[2] = dp[1] + 1 = 5  // grid[0][2]

After row 0: dp = [1, 4, 5]

Row 1:
i = 1 (processing grid[1]):
  dp[0] = dp[0] + grid[1][0] = 1 + 1 = 2  // first column, only from above
  dp[1] = min(dp[1], dp[0]) + grid[1][1] = min(4, 2) + 5 = 2 + 5 = 7
  dp[2] = min(dp[2], dp[1]) + grid[1][2] = min(5, 7) + 1 = 5 + 1 = 6

After row 1: dp = [2, 7, 6]

Row 2:
i = 2 (processing grid[2]):
  dp[0] = dp[0] + grid[2][0] = 2 + 4 = 6  // first column, only from above
  dp[1] = min(dp[1], dp[0]) + grid[2][1] = min(7, 6) + 2 = 6 + 2 = 8
  dp[2] = min(dp[2], dp[1]) + grid[2][2] = min(6, 8) + 1 = 6 + 1 = 7

Final: dp[2] = 7 ✓
```

**Variable Tracking** (1D DP array):
- `dp[j]` always represents minimum path sum to reach current row, column j
- Each iteration: update in-place from left to right
- `dp[j] = min(dp[j], dp[j-1]) + grid[i][j]` — min of (from above, from left) + current cell
- Only need O(n) space instead of O(m×n)

**Why left-to-right order works**: When computing dp[j], dp[j-1] has already been updated to current row's value, and dp[j] still holds previous row's value (from above). This gives us exactly the two options we need.

## 4. Code Template

**Time Complexity**: O(m × n) where m = rows, n = columns
**Space Complexity**: O(n) — 1D array optimization (or O(m×n) for full table)

```kotlin
package dp_2d

import kotlin.math.minOf

/**
 * LeetCode: https://leetcode.com/problems/minimum-path-sum/
 * Difficulty: Medium
 * Core pattern: 2-D DP with grid traversal
 * 
 * Time Complexity: O(m × n)
 * Space Complexity: O(n) — 1D array optimization
 */
fun minPathSum(grid: Array<IntArray>): Int {
    val m = grid.size       // number of rows
    val n = grid[0].size    // number of columns
    
    // 1D DP array — dp[j] = min path sum to reach current cell in column j
    val dp = IntArray(n)
    dp[0] = grid[0][0]
    
    // Initialize first row — can only come from left
    for (j in 1 until n) {
        dp[j] = dp[j - 1] + grid[0][j]
    }
    
    // Process remaining rows
    for (i in 1 until m) {
        // First column — can only come from above (dp[0] already has above value)
        dp[0] += grid[i][0]
        
        // Process remaining columns in current row
        for (j in 1 until n) {
            // dp[j] currently holds value from above (previous row, same column)
            // dp[j-1] already updated to current row's value (from left)
            dp[j] = minOf(dp[j], dp[j - 1]) + grid[i][j]
        }
    }
    
    return dp[n - 1]
}

/**
 * Explanation:
 * - dp[j] represents minimum path sum to reach current position (current row, column j)
 * - Key insight: update left-to-right so dp[j-1] is already current row, dp[j] is still from above
 * - Transition: min(come from above, come from left) + current cell value
 * - First row initialized specially (only left moves)
 * - First column handled by dp[0] += grid[i][0] each row (only above moves)
 * 
 * Why 1D works:
 * - Before update: dp[j] = value from previous row (above)
 * - After dp[j-1] update: dp[j-1] = value from current row (left)
 * - minOf(dp[j], dp[j-1]) gives us the two options correctly
 */
```

**Line-by-line explanation**:
- Lines 8-10: Get grid dimensions, initialize DP array
- Lines 13-15: Special case — first row can only come from left, so cumulative sum
- Line 18: Outer loop iterates through each row (starting from second row)
- Line 21: First column — only can come from above, so just add current cell value
- Lines 24-26: Core DP transition for columns 1 to n-1
- Line 29: Return result — minimum path sum to bottom-right corner

**Why this works**:
- Each cell's optimal path depends only on the cell above and the cell to the left
- 1D array works because of the left-to-right update order
- In-place modification saves space while maintaining correctness
- Final answer at dp[n-1] after processing all rows

## 5. Practice Problems

### Easy Problem 1: Unique Paths
- **LeetCode**: https://leetcode.com/problems/unique-paths/
- **Difficulty**: Medium
- **Core pattern**: Path counting — dp[i][j] = dp[i-1][j] + dp[i][j-1]
- **Approach hint**: Use combinatorics or DP with 1D array optimization
- **Expected time and space complexity**: O(m×n) time, O(min(m,n)) space

### Easy Problem 2: Unique Paths II (With Obstacles)
- **LeetCode**: https://leetcode.com/problems/unique-paths-ii/
- **Difficulty**: Medium
- **Core pattern**: Path counting with obstacles modification
- **Approach hint**: Set dp[i][j] = 0 for obstacle cells, rest same pattern
- **Expected time and space complexity**: O(m×n) time, O(n) space

### Medium Problem 1: Minimum Path Sum
- **LeetCode**: https://leetcode.com/problems/minimum-path-sum/
- **Difficulty**: Medium
- **Core pattern**: dp[i][j] = min(dp[i-1][j], dp[i][j-1]) + grid[i][j]
- **Approach hint**: Use 1D DP array with left-to-right update order
- **Expected time and space complexity**: O(m×n) time, O(n) space

### Medium Problem 2: Maximum Path Sum (Triangle)
- **LeetCode**: https://leetcode.com/problems/triangle/ (or similar)
- **Difficulty**: Medium
- **Core pattern**: Bottom-up DP on triangular grid
- **Approach hint**: Process from bottom row upward, each cell chooses min of two below
- **Expected time and space complexity**: O(n²) time, O(n) space

### Hard Problem 1: Maximal Square
- **LeetCode**: https://leetcode.com/problems/maximal-square/
- **Difficulty**: Hard
- **Core pattern**: dp[i][j] = min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]) + 1 for '1' cells
- **Approach hint**: Track largest square ending at each cell
- **Expected time and space complexity**: O(m×n) time, O(m×n) space (can optimize to O(n))

### Hard Problem 2: Word Search II
- **LeetCode**: https://leetcode.com/problems/word-search-ii/
- **Difficulty**: Hard
- **Core pattern**: Trie + DFS + DP with memoization on grid
- **Approach hint**: Build trie from word list, DFS from each cell, memoize visited states
- **Expected time and space complexity**: O(M×N×4^L) time worst case, O(W×L) space for Trie (M×N=grid, L=max word length, W=word count)