# Trees: Binary Tree, BST, and Tree Traversals

## Concept Explanation

### What is a Tree?
A tree is a hierarchical (non-linear) data structure where:
- **Root**: Topmost node
- **Node**: Contains data + pointers to children (typically left and right for binary trees)
- **Child**: Has exactly one parent (except root), zero or more siblings share same parent
- **Leaf**: No children
- **Internal/Non-leaf**: At least one child

**Binary Tree**: Each node has at most 2 children (left and right).

### Binary Search Tree (BST) Special Properties
A BST maintains ordering invariant:
- Left subtree values < Root value
- Right subtree values > Root value
- Both subtrees must themselves be BSTs
- **In-order traversal of BST gives sorted output**

**Search in BST**: Average O(log N), Worst case O(N) when skewed (linked list). Height determines performance.

**Why balancing matters**: AVL/Red-Black trees keep height ≤ ~1.44 log N; skewed BST degrades to O(N).

### How It Works Internally
- Recursive structure naturally matches: process left subtree, then current node, then right subtree
- Parent pointers enable upward traversal (but not all trees have them; recursive descent standard)
- Height is key to algorithm performance on trees: O(height) per operation
- If tree is balanced: operations O(log N); if skewed: O(N)

### Real-Life Analogy
Think of an **organizational chart**:
- CEO = root
- Department heads = internal nodes
- Individual employees = leaves
- You read top-down for hierarchy queries
- In-order traversal reads alphabetically ordered reports
- Pre-order reads as CEO sees it (current + subordinates)
- Post-order reports before boss (subordinates report to supervisor first)

### Time and Space Complexity

| Operation | Balanced BST | Skewed BST | Notes |
|-----------|-------------|-----------|-------|
| Insert | O(log N) | O(N) | Balanced through rotations; skewed = no rotations |
| Delete | O(log N) | O(N) | Same analysis |
| Search | O(log N) | O(N) | |
| Traverse all nodes | O(N) | O(N) | Always linear in number of nodes |
| Find min/max | O(log N) | O(1)-O(N) | Straightforward by traversing down one side |
| Check BST validity | O(N) | O(N) | Single pass with parent checks |
| In-order | O(N) | O(N) | Linear regardless |
| LCA | O(log N) | O(N) | Paths to root both; find divergence point |

### Common Mistakes and Edge Cases

1. **Memory leak / null dereference**: Always check null children before accessing fields
2. **Forgetting balance after insertions**: Makes O(N) worst case degrading even for balanced-looking tree
3. **BST property violation on insert**: Insert in wrong position invalidates tree invariant
4. **Maximizing stack depth**: Deep recursion → stack overflow. Increase recursion limit or use iterative approach.
5. **Null vs undefined behavior**: Java allows null but Kotlin doesn't (NPE on null child access)
6. **Off-by-one error in height calculation**: Height defined as number of edges or levels — consistent definition matters
7. **Recursive path not making progress**: Ensure each recursion reduces problem size or moves toward base case
8. **Modifying tree while iterating**: Must traverse with explicit Node references rather than index-based iteration over arrays
9. **Not handling equal values carefully**: Problem may specify "strictly less/greater" or "less or equal" — violates differ
10. **In-order traversal order**: Pre-order visits root first; In-order left-root-right; Post-order left-right-root

### When to Use Trees in Interviews

| Scenario | Approach | Why |
|----------|----------|-----|
| Range queries / dynamic data | BST / Augmented BST | Ordered structure supports range |
| Find lowest common ancestor | Path comparison / parent pointers | Hierarchical relationships |
| Expression evaluation | BST (operator precedence) | Natural tree representation |
| Dynamic programming on state space | Tree DP | State transitions form tree |
| Generic hierarchy modeling | General binary tree | Flexible node model |
| Expression parsing | BST | Operators & operands naturally structured |
| Augmented tree (max/min tracking) | Augmented BST | O(1) access to min/max at any point |

## Important Patterns

### 1. Tree Traversal Orders
```
Pre-order:    visit(root), visit(left), visit(right)
In-order:     visit(left), visit(root), visit(right)  → sorted for BST
Post-order:   visit(left), visit(right), visit(root)
Level-order:  BFS via queue, top to bottom
```
All four visits take O(N) total time each.

### 2. Maximum Subtree Sum
- Recursively compute max subtree sum at each node: `maxSum(node) = max(node.val, maxSum(left), maxSum(right))`
- Global maximum tracked across all nodes
- Handle negative values correctly (empty subtrees allowed, negative values kept)

### 3. Minimum Path Sum (Weighted Path with Constraint)
- DFS returning best path sum from current node down either side
- Recurse on left/right children with appropriate constraints
- Combine results at each node

### 4. Same Subtree Detection (Equivalence Check)
- Compare recursively: return (val, leftEquivalent, rightEquivalent) tuple
- Two nodes are same subtree iff their tuples match

### 5. Lowest Common Ancestor
**Case 1 - Parent pointers available:**
```
return null if both root and target are same (target found)
if root == target: return root
find in left subtree, find in right subtree, return LCA
```

**Case 2 - No parent pointers (standard):**
- Path-based: find path from root to each target, compare paths
- Or single-pass recursive: if node is one target, recurse other; else recurse both children

### 6. Path Sum variations
- Path can go up then down (not just down-branch) — need cumulative sum DFS
- Can go from leaf to leaf crossing root
- Each node visited at most once: use global accumulator + recursion

### 7. Tree DP (Dynamic Programming on Trees)
- Process nodes bottom-up
- Compute values from children before parent
- States: dp[node] depends on dp[children]

## Dry Run

### Example: Maximum Subtree Sum
Tree: `[-10, 9, 20, null, null, 15, 7]`

```
       Node(-10)
        /     \
    Node(9)  Node(20)
             /    \
        Node(15)  Node(7)

Subtree sums:
  9  = 9
  15+7 = 22, Node(20) = 22+9=31
  Node(-10) = -10+9+31 = 30 ← max!

Answer: 30 (the whole tree)
```

### Example: In-Order Traversal of BST
BST: `[50, 30, 60, 20, 40, 70]`

```
         50
        /   \
      30    60
     / \   / \
   20  40 70

In-order (L, root, R):
20, 30, 40, 50, 60, 70 ← sorted!
```

## Code Template

```kotlin
package trees

/**
 * Template for tree problems in Kotlin
 */
class TreeNode<T>(var val: T, var left: TreeNode<T>? = null, var right: TreeNode<T>? = null) {
    companion object {
        fun create(val: T, left: TreeNode<T>? = null, right: TreeNode<T>? = null): TreeNode<T> {
            return TreeNode(val, left, right)
        }
    }
}

// Traversal template
fun inorderTraversal(root: TreeNode<Int>?): List<Int> {
    val result = mutableListOf<Int>()
    helper(root, result)
    return result
}

private fun helper(node: TreeNode<Int>?, result: MutableList<Int>) {
    if (node == null) return
    helper(node!!.left, result)
    result.add(node.val)
    helper(node!!.right, result)
}

// Maximum subtree sum template
fun maxSubtreeSum(root: TreeNode<Int>?): Int {
    val best = Int.MIN_VALUE
    maxSubtreeHelper(root, best)
    return best
}

private fun maxSubtreeHelper(node: TreeNode<Int>?, bestRef: IntRef) {
    if (node == null) return 0  // base case: empty subtree contributes 0
    val leftSum = maxSubtreeHelper(node!!.left, bestRef)
    val rightSum = maxSubtreeHelper(node!!.right, bestRef)
    val currentSum = maxOf(node.val, node.val + leftSum, node.val + rightSum)
    bestRef.value = maxOf(bestRef.value, currentSum)
    return currentSum
}

import java.util.concurrent.atomic.AtomicInteger
typealias IntRef = AtomicInteger
```

### Complexity Analysis (Template)
- **Time**: O(N) for most tree operations since each node visited once
- **Space**: O(N) for recursion stack in worst case (skewed tree); O(height) for balanced

## Practice Problems

### Easy Problems

#### 1. **Maximum Depth of Binary Tree**
- **LeetCode**: https://leetcode.com/problems/maximum-depth-of-binary-tree/
- **Difficulty**: Easy
- **Core pattern**: Recursive depth computation
- **Approach hint**: `depth(node) = 1 + max(depth(left), depth(right))`, base case `node == null → 0`. Either recursive or iterative BFS/DFS.
- **Expected time**: O(N), **space**: O(height) = O(N) worst case

#### 2. **Validate BST**
- **LeetCode**: https://leetcode.com/problems/validate-binary-search-tree/
- **Difficulty**: Easy
- **Core pattern**: Invariant check with min/max bounds passed via recursion
- **Approach hint**: Return boolean indicating if subtree satisfies constraints. For each node, check all values in left subtree < node.val and right > node.val, recursively validate children. Pass bounds: (minBound, maxBound) restrict what values are valid in this subtree.
- **Expected time**: O(N), **space**: O(N) recursion or O(height) iterative

### Medium Problems

#### 3. **Maximum Width of Binary Tree**
- **LeetCode**: https://leetcode.com/problems/maximum-width-of-binary-tree/
- **Difficulty**: Medium
- **Core pattern**: Level-order traversal (BFS) with index tracking
- **Approach hint**: BFS level by level. Assign index = 2^level - 1 + position in level. Max index gap = width. Edge case: empty tree. Alternative iterative DFS with index math avoids queue but uses index-based insertion tracking.
- **Expected time**: O(N), **space**: O(maxWidth)

#### 4. **Invert Binary Tree**
- **LeetCode**: https://leetcode.com/problems/invert-binary-tree/
- **Difficulty**: Easy
- **Core pattern**: Recursive swapping of left and right children
- **Approach hint**: Recursively invert subtrees, then swap left and right at each node. Base case: null → return.
- **Expected time**: O(N), **space**: O(N) recursion

### Hard Problems

#### 5. **Lowest Common Ancestor of Two BST Nodes**
- **LeetCode**: https://leetcode.com/problems/lowest-common-ancestor-of-two-binary-search-trees/
- **Difficulty**: Medium (often Hard variant interviews)
- **Core pattern**: BST optimization — follow paths based on comparisons
- **Approach hint**: Starting from root, if both targets < root, go left; if both > root, go right; otherwise they're in different subtrees, root is LCA. Same logic applied recursively for each side.
- **Expected time**: O(log N) average (balanced), O(N) worst case (skewed)

#### 6. **Maximum Path Sum**
- **LeetCode**: https://leetcode.com/problems/binary-tree-maximum-path-sum/
- **Difficulty**: Medium (often appeared in Hard variant interviews)
- **Core pattern**: Tree DP — compute best downward path from each node down either direction
- **Approach hint**: For each node, consider it as peak of path with path going down left or right. Best path through node = node.val + max(leftBest, rightBest) where bestLeft/Right = best downward sum from that side. Track global maximum across all possible peaks. Use post-order DFS where each call returns max downward path sum starting at that node.
- **Expected time**: O(N), **space**: O(N) recursion

---
*End of Trees topic*