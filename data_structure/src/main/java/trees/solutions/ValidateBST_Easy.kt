package trees

/**
 * LeetCode: https://leetcode.com/problems/validate-binary-search-tree/
 * Difficulty: Easy
 * Core pattern: Invariant check with min/max bounds via recursion
 */
class TreeNode<T>(var data: T, var left: TreeNode<T>? = null, var right: TreeNode<T>? = null) {
    companion object {
        fun create(data: T, left: TreeNode<T>? = null, right: TreeNode<T>? = null): TreeNode<T> {
            return TreeNode(data, left, right)
        }
    }
}

fun isBST(root: TreeNode<Int>?): Boolean {
    return helper(root, Int.MIN_VALUE, Int.MAX_VALUE)
}

private fun helper(node: TreeNode<Int>?, minBound: Int, maxBound: Int): Boolean {
    if (node == null) return true  // Base case: empty subtree always valid

    // Check current node against bounds
    if (node.val <= minBound || node.val >= maxBound) return false

    // Recursively validate both subtrees with updated bounds
    return helper(node!!.left, minBound, node.val) &&
           helper(node!!.right, node.val, maxBound)
}

import kotlin.math.min
import kotlin.math.max