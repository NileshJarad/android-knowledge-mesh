package tree

fun main() {

    val bstRep = """        
        2
       / \
      1   5
         /  \
        3    8
              \
              15
             /
            9
"""
    val bst = BinarySearchTree()
    val root = bst.createBst(arrayOf(2, 5, 3, 8, 15, 9, 1))
    val isBst = CheckIsBst().isBst(root)
    println(bstRep)
    println("\nTree isBst = $isBst")


    val question = """            
            1
           /
          2
         / \
        4   5

"""

    val oneP = BinaryNode(1)
    oneP.left = BinaryNode(2)
    oneP.left?.left = BinaryNode(4)
    oneP.left?.right = BinaryNode(5)

    println(question)
    val isBst2 = CheckIsBst().isBst(oneP)
    println("\nTree isBst2 = $isBst2")
}

class CheckIsBst {

    fun isBst(node: BinaryNode<Int>?, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Boolean {
        if (node == null) return true
         if (node.data <= min ||  node.data >= max) {
             return false
         }
        return isBst(node.left, min, node.data) &&
                isBst(node.right, node.data, max)
    }
}