package trees

/**
 * LeetCode: https://leetcode.com/problems/maximum-depth-of-binary-tree/
 * Difficulty: Easy
 * Core pattern: Recursive depth computation
 */
class TreeNode<T>(val data: T, var left: TreeNode<T>? = null, var right: TreeNode<T>? = null) {
    companion object {
        fun create(data: T, left: TreeNode<T>? = null, right: TreeNode<T>? = null): TreeNode<T> {
            return TreeNode(data, left, right)
        }
    }
}

fun maxDepth(root: TreeNode<Int>?): Int {
    if (root == null) return 0  // Base case
    val leftDepth = maxDepth(root!!.left)
    val rightDepth = maxDepth(root!!.right)
    return 1 + maxOf(leftDepth, rightDepth)
}
