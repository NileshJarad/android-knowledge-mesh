package linklist.singly


fun main() {
    val head = SinglyLLNode(1)

    val node2 = SinglyLLNode(2)
    val node3 = SinglyLLNode(2)
    val node4 = SinglyLLNode(2)


    head.next = node2
    node2.next = node3
    node3.next = node4
//    node4.next = head

    val cyclePresent = cyclic(head)
    println("Link list contains cycle $cyclePresent")
}

fun cyclic(head: SinglyLLNode<Int>?): Boolean {
    var hare = head?.next
    var tortoise = head
    while (hare?.next != null) {
        if (hare == tortoise) {
            return true
        }
        hare = hare.next?.next
        tortoise = tortoise?.next
    }
    return false
}