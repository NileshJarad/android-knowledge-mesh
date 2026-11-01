package trees

/**
 * LeetCode: https://leetcode.com/problems/lowest-common-ancestor-of-two-binary-search-trees/
 * Difficulty: Medium (often appeared in Hard variant interviews)
 * Core pattern: BST optimization — follow paths based on comparisons
 */
class TreeNode<T>(val data: T, val left: TreeNode<T>? = null, val right: TreeNode<T>? = null) {
    companion object {
        fun create(data: T, left: TreeNode<T>? = null, right: TreeNode<T>? = null): TreeNode<T> {
            return TreeNode(data, left, right)
        }
    }
}

fun lowestCommonAncestor(root: TreeNode<Int>?, p: TreeNode<Int>?, q: TreeNode<Int>?): TreeNode<Int>? {
    if (root == null) return null

    // p and q are nodes in the tree; search by value if values distinguish them
    val pVal = p!!.data
    val qVal = q!!.data

    if (pVal > qVal) {
        return searchLCA(root, qVal, pVal)  // swap for consistent traversal direction
    } else {
        return searchLCA(root, pVal, qVal)
    }
}

private fun searchLCA(node: TreeNode<Int>, target1: Int, target2: Int): TreeNode<Int>? {
    // If both targets are on one side, descend that side; otherwise root is LCA
    if (target1 < node.data && target2 < node.data) {
        return searchLCA(node.left!!, target1, target2)
    } else if (target1 > node.data && target2 > node.data) {
        return searchLCA(node.right!!, target1, target2)
    } else {
        // One is here, other is in subtree (or here too, both here!)
        return node
    }
}

import kotlin.math.min
import kotlin.math.max
