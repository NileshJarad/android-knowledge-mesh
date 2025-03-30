package tree

class BinaryNode<T>(
    var data: T,
    var left: BinaryNode<T>? = null,
    var right: BinaryNode<T>? = null,
    var height: Int = 0
){
    override fun toString(): String {
        return "BinaryNode(data=$data)"
    }
}