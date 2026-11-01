package trees

/**
 * LeetCode: https://leetcode.com/problems/invert-binary-tree/
 * Difficulty: Easy
 * Core pattern: Recursive swapping of left and right children
 */
class TreeNode<T>(val data: T, var left: TreeNode<T>? = null, var right: TreeNode<T>? = null) {
    companion object {
        fun create(data: T, left: TreeNode<T>? = null, right: TreeNode<T>? = null): TreeNode<T> {
            return TreeNode(data, left, right)
        }
    }
}

fun invertTree(root: TreeNode<Int>?): TreeNode<Int>? {
    if (root == null) return null  // Base case

    // Swap children first
    val temp = root.left!!
    root.left = root.right!!
    root.right = temp

    // Recursively invert both subtrees
    invertTree(root.left)
    invertTree(root.right)

    return root
}

// Iterative version using stack
fun invertTreeIterative(root: TreeNode<Int>?): TreeNode<Int>? {
    if (root == null) return null

    val stack = ArrayDeque<TreeNode<Int>>()
    stack.addLast(root)

    while (stack.isNotEmpty()) {
        val node = stack.removeFirst()
        val temp = node.left!!
        node.left = node.right!!
        node.right = temp

        stack.addLast(node.left)
        stack.addLast(node.right)
    }

    return root
}
