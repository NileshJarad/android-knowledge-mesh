package linklist.singly

fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.addNode(1)
    singlyLL.addNode(2)
    singlyLL.addNode(3)
    singlyLL.addNode(4)
    singlyLL.printSinglyLL()


    println()
    val singlyLL2 = SinglyLL<Int>()
    singlyLL2.addNode(2)
    singlyLL2.addNode(3)
    singlyLL2.addNode(4)
    singlyLL2.addNode(6)
    singlyLL2.printSinglyLL()
    println()
//    val node = mergeSortedLinkList(singlyLL.head, singlyLL2.head)

    val nodeRecursion = mergeSortedLinkListRecursive(singlyLL.head, singlyLL2.head)


    var temp = nodeRecursion
    while (temp != null) {
        print("${temp.data} ->")
        temp = temp.next
    }

    print("Null")
}

fun mergeSortedLinkList(headFirst: SinglyLLNode<Int>?, headSecond: SinglyLLNode<Int>?): SinglyLLNode<Int>? {

    val newHead: SinglyLLNode<Int> = SinglyLLNode(data = headFirst?.data ?: headSecond?.data ?: return null)
    var current: SinglyLLNode<Int>? = newHead
    var head1 = headFirst
    var head2 = headSecond

    while (head2 != null && head1 != null) {
        if (head1.data <= head2.data) {
            current?.next = head1
            head1 = head1.next
        } else {
            current?.next = head2
            head2 = head2.next
        }
        current = current?.next
    }
    current?.next = head1 ?: head2
    return newHead.next
}

fun mergeSortedLinkListRecursive(l1: SinglyLLNode<Int>?, l2: SinglyLLNode<Int>?): SinglyLLNode<Int>? {
    if (l1 == null) return l2
    if (l2 == null) return l1

    println("L1 = ${l1.data}  L2 =${l2.data}")

    return if (l1.data <= l2.data) {
        l1.next = mergeSortedLinkListRecursive(l1.next, l2)
        l1
    } else {
        l2.next = mergeSortedLinkListRecursive(l1, l2.next)
        l2
    }
}