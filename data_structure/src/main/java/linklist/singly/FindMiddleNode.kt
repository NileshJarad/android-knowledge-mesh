package linklist.singly

fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.printSinglyLL()
    singlyLL.addNode(1)
    singlyLL.addNode(2)
    singlyLL.addNode(3)
    singlyLL.addNode(4)
    singlyLL.printSinglyLL()

    println()
    println("Middle node data= ${middleNode(singlyLL.head)?.data}")
}


fun middleNode(head: SinglyLLNode<Int>?): SinglyLLNode<Int>? {

    var hare = head
    var tortoise = head
    while (hare != null) {
        hare = hare.next?.next
        tortoise = tortoise?.next
    }

    return tortoise
}