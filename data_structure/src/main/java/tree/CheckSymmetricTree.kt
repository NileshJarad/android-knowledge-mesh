package tree

fun main() {

    val question = """   
       https://leetcode.com/problems/symmetric-tree/description/         
           1
        /    \
       2      3
      / \    / \
     4   5  5   4
"""

    val oneP = BinaryNode(1)
    oneP.left = BinaryNode(2)
    oneP.left?.left = BinaryNode(4)
    oneP.left?.right = BinaryNode(5)

    oneP.right = BinaryNode(3)
    oneP.right?.left = BinaryNode(5)
    oneP.right?.right = BinaryNode(4)

    println("Tree Symmetric ${isSymmetric(oneP)} ")
}


fun isSymmetric(root: BinaryNode<Int>?): Boolean {
    if (root == null) return true
    return isMirror(root.left, root.right)
}
fun isMirror(left: BinaryNode<Int>?, right: BinaryNode<Int>?): Boolean {
    if (left == null && right == null) return true
    if (left == null || right == null) return false
    return (left.data == right.data) && isMirror(left.left, right.right) && isMirror(left.right, right.left)
}