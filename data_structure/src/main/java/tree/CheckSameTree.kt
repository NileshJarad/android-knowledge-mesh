package tree

fun main() {
    /***
     *  https://leetcode.com/problems/same-tree/description/
     *
     *         1
     *        / \
     *       2   3
     *
     */
    val oneP = BinaryNode(1)
    oneP.left = BinaryNode(2)
    oneP.right = BinaryNode(3)

    val oneQ = BinaryNode(1)
    oneQ.left = BinaryNode(2)
//    oneQ.right = BinaryNode(3)

   print( isSameTree(oneP, oneQ))
}

fun isSameTree(p: BinaryNode<Int>?, q: BinaryNode<Int>?): Boolean {

    if(p == null && q == null){
        return true
    }else if(p?.data != q?.data){
        return false
    }
    return isSameTree(p?.left, q?.left) && isSameTree(p?.right,q?.right)
}

