package trees

/**
 * LeetCode: https://leetcode.com/problems/binary-tree-maximum-path-sum/
 * Difficulty: Medium (often appeared in Hard variant interviews)
 * Core pattern: Tree DP — compute best downward path from each node down either direction
 */
class TreeNode<T>(val data: T, val left: TreeNode<T>? = null, val right: TreeNode<T>? = null) {
    companion object {
        fun create(data: T, left: TreeNode<T>? = null, right: TreeNode<T>? = null): TreeNode<T> {
            return TreeNode(data, left, right)
        }
    }
}

fun maxPathSum(root: TreeNode<Int>?): Int {
    var globalMax = Int.MIN_VALUE
    maxDownwardSum(root, globalMax)  // returns max downward path sum starting at this node
    return globalMax
}

// Returns max path sum that includes this node and goes DOWN both directions
// (the "peak" path through this node)
fun peakPathSum(node: TreeNode<Int>, globalRef: IntRef): Int {
    if (node == null) return 0

    // Recursively get best downward paths from each child
    val leftBest = maxDownwardSum(node.left!!, globalRef)
    val rightBest = maxDownwardSum(node.right!!, globalRef)

    // Peak path through this node (turning point): node.val + left + right
    val currentPeak = node.data + leftBest + rightBest
    globalRef.value = maxOf(globalRef.value, currentPeak)

    // Best downward path starting at this node going down either side
    val currentDownward = node.data + maxOf(leftBest, rightBest)
    return currentDownward
}

private fun maxDownwardSum(node: TreeNode<Int>?, globalRef: IntRef): Int {
    if (node == null) return 0
    val bestFromThisSide = peakPathSum(node, globalRef)
    return bestFromThisSide
}

import java.util.concurrent.atomic.AtomicInteger
typealias IntRef = AtomicInteger

// Simplified implementation using a mutable var tracked differently
fun maxPathSumSimple(root: TreeNode<Int>?): Int {
    var globalMax = Int.MIN_VALUE

    fun helper(node: TreeNode<Int>?) {
        if (node == null) return

        val leftBest = if (node.left == null) 0 else helper(node.left)
        val rightBest = if (node.right == null) 0 else helper(node.right)

        globalMax = maxOf(globalMax, node.data + leftBest + rightBest)
        val currentDownward = node.data + maxOf(leftBest, rightBest)

        helper(node.left)
        helper(node.right)
    }

    helper(root)
    return globalMax
}
