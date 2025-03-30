package tree

import java.util.LinkedList

fun main() {


    val treeRep = """        
        8
       / \
      3   10
     / \     \
    1   6     14
       / \    /
      4   7  13
"""

    print(treeRep)

    val bst = BinarySearchTree()
    val root = bst.createBst(arrayOf(8, 3, 10, 1, 6, 14, 4, 7, 13))
    rightViewOfTree(root)

    println("Left view of ")

    leftViewOfTree(root)
}

/**
 * Level order traversal + last element
 */
fun leftViewOfTree(root: BinaryNode<Int>?) {
    if (root == null) {
        return
    }

    val que = LinkedList<BinaryNode<Int>?>()
    que.offer(root)
    while (que.isNotEmpty()) {
        val size = que.size
        for (i in 0 until size) {
            val node = que.poll()
            if (node?.left != null) {
                que.offer(node.left)
            }
            if (node?.right != null) {
                que.offer(node.right)
            }

            if (i == 0) {
                print("${node?.data} ,")
            }
        }

    }

}/**
 * Level order traversal + last element
 */
fun rightViewOfTree(root: BinaryNode<Int>?) {
    if (root == null) {
        return
    }

    val que = LinkedList<BinaryNode<Int>?>()
    que.offer(root)
    while (que.isNotEmpty()) {
        val size = que.size
        for (i in 0 until size) {
            val node = que.poll()
            if (node?.left != null) {
                que.offer(node.left)
            }
            if (node?.right != null) {
                que.offer(node.right)
            }

            if (i == size - 1) {
                print("${node?.data} ,")
            }
        }

    }

}