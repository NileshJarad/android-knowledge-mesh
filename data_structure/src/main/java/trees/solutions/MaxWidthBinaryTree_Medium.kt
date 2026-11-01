package trees

/**
 * LeetCode: https://leetcode.com/problems/maximum-width-of-binary-tree/
 * Difficulty: Medium
 * Core pattern: Level-order traversal (BFS) with index tracking
 */
class TreeNode<T>(val data: T, val left: TreeNode<T>? = null, val right: TreeNode<T>? = null) {
    companion object {
        fun create(data: T, left: TreeNode<T>? = null, right: TreeNode<T>? = null): TreeNode<T> {
            return TreeNode(data, left, right)
        }
    }
}

fun widthOfBinaryTree(root: TreeNode<Int>?): Int {
    if (root == null) return 0

    // BFS level by level
    val result = 1  // width starts at 1 for first level
    var prevQueueSize = 1
    val currentLevel = mutableListOf<TreeNode<Int>>()

    var levelQueue = mutableListOf<TreeNode<Int>>()
    levelQueue.add(root)

    while (levelQueue.isNotEmpty()) {
        val queue = levelQueue
        val nextLevel = mutableListOf<TreeNode<Int>>()
        for (node in queue) {
            nextLevel.add(node.left!!)
            nextLevel.add(node.right!!)
        }

        // Track indices in this level; max index difference = width
        // Use position tracking since index of node at level L depends on path from root
        // Index = 2^level + position within level (0-indexed)
        // Simplified: compute actual position mathematically using bit length
        result = maxOf(result, nextLevel.size)
        levelQueue = nextLevel
    }

    // Need actual index computation to find true max width
    // Correct approach: assign index based on bit length of path
    var maxWidth = 0
    assignIndicesAndMeasure(root, 0, maxWidth)
    return maxWidth
}

private fun assignIndicesAndMeasure(node: TreeNode<Int>?, level: Int, maxWidth: Int) {
    if (node == null) return
    node.bitLength = level + 1  // store path length for index computation
    assignIndicesAndMeasure(node.left, level + 1, maxWidth)
    assignIndicesAndMeasure(node.right, level + 1, maxWidth)

    val leftBitLength = node.left!!.bitLength
    val rightBitLength = node.right!!.bitLength
    maxWidth = maxOf(maxWidth, rightBitLength - leftBitLength + 1)
}
