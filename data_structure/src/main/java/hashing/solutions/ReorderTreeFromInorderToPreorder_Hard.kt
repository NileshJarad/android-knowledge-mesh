package hashing

/**
 * LeetCode: https://leetcode.com/problems/reorder-data-in-binary-tree-from-inorder-to-preorder/
 * Difficulty: Hard
 * Core pattern: Preorder gives structure, inorder gives position; combine them recursively
 */
import kotlin.collections.toList

class BinaryNode<T>(val value: T) {
    var left: BinaryNode<T>? = null
    var right: BinaryNode<T>? = null
}

fun buildTree(inorder: List<Int>, preorder: List<Int>): BinaryNode<Int>? {
    if (inorder.isEmpty() || preorder.isEmpty()) return null

    val rootValue = preorder[0] // First element in preorder is root
    val rootIndex = inorder.indexOf(rootValue)

    val leftInorder = inorder.subList(0, rootIndex)
    val rightInorder = inorder.subList(rootIndex + 1, inorder.size)
    val leftPreorder = preorder.subList(1, (leftInorder.size + 1).coerceAtLeast(1))
    val rightPreorder = preorder.subList((leftInorder.size + 1).coerceAtLeast(1), preorder.size)

    val root = BinaryNode(rootValue)
    root.left = buildTree(leftInorder, leftPreorder)
    root.right = buildTree(rightInorder, rightPreorder)

    return root
}

// Alternative working version using index directly on arrays
fun buildTreeSimple(inorder: IntArray, preorder: IntArray): BinaryNode<Int>? {
    if (inorder.isEmpty() || preorder.isEmpty()) return null

    val n = inorder.size
    val inorderMap = mutableMapOf<Int, Int>()  // value → index for quick lookup
    for (i in inorder.indices) inorderMap[inorder[i]] = i

    fun build(inStart: Int, inEnd: Int, preStart: Int, preEnd: Int): BinaryNode<Int>? {
        if (inStart >= inEnd || preStart > preEnd) return null

        val rootVal = preorder[preStart]
        val rootIdx = inorderMap[rootVal]!!

        val leftSize = rootIdx - inStart
        val leftNode = build(inStart, rootIdx, preStart + 1, preStart + leftSize)
        val rightNode = build(rootIdx + 1, inEnd, preStart + leftSize + 1, preEnd)

        val node = BinaryNode(rootVal)
        node.left = leftNode
        node.right = rightNode
        return node
    }

    return build(0, n, 0, n - 1)
}
