package tree

import kotlin.math.max



fun main() {
    val bst = BinarySearchTree()

    /**
     *         8
     *        / \
     *       3   10
     *      / \     \
     *     1   6     14
     *        / \    /
     *       4   7  13
     *
     */
    val root = bst.createBst(arrayOf(8, 3, 10, 1, 6, 14, 4, 7, 13))

    println("Height of every node : ${getNodeHeights(root)}")
    println("Height of Tree : ${getHeightOfTree(root)}")
    println("Level order tree : ")
    root?.levelOrder()
    println()
    println()
    root?.levelOrder2()
    println()
    root?.levelOrderWithNewLine()
    println()


    /***
     *         1
     *        / \
     *       2   3
     *      / \
     *     4   5
     *
     */
    val rootManual = BinaryNode(1)
    rootManual.left = BinaryNode(2)
    rootManual.right = BinaryNode(3)
    rootManual.left?.left = BinaryNode(4)
    rootManual.left?.right = BinaryNode(5)
    println("rootManual Height of Tree : ${getHeightOfTree(rootManual)}")
    println("Height of every node : ${getNodeHeights(root)}")
}


fun getNodeHeights(root: BinaryNode<Int>?): HashMap<Int, Int> {
    val map = HashMap<Int, Int>()
    computeHeights(root, map)
    return map
}

fun computeHeights(root: BinaryNode<Int>?, heightMap: MutableMap<Int, Int>): Int {
    if (root == null) return -1 // Height of null is -1 (leaf nodes will be 0)

    val leftHeight = computeHeights(root.left, heightMap)
    val rightHeight = computeHeights(root.right, heightMap)

    val height = 1 + maxOf(leftHeight, rightHeight)
    heightMap[root.data] = height

    return height
}

fun getHeightOfTree(root: BinaryNode<Int>?): Int {
    if (root == null) {
        return -1
    }
    return 1 + max(getHeightOfTree(root.left), getHeightOfTree(root.right))
}