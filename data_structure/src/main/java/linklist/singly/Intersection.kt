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

    println("Intersection ${intersectionNode(singlyLL.head, singlyLL2.head)?.data}")
}


/**
 * Step	    ptr1	ptr2
 * 0	    1	    9
 * 1	    2	    4
 * 2	    3	    5
 * 3	    4	    null
 * 4	    5	    1
 * 5	    null	2
 * 6	    9	    3
 * 7	    4	    4 ✅ ← Intersection found
 */
fun intersectionNode(head1: SinglyLLNode<Int>?, head2: SinglyLLNode<Int>?): SinglyLLNode<Int>? {

    var ptr1: SinglyLLNode<Int>? = head1
    var ptr2: SinglyLLNode<Int>? = head2

    // If any one of the heads is NULL, there is no intersection

    // If any one of the heads is NULL, there is no intersection
    if (ptr1 == null || ptr2 == null) return null

    while (ptr1 != null || ptr2 != null) {
        ptr1 = if (ptr1 != null) ptr1.next else head2
        ptr2 = if (ptr2 != null) ptr2.next else head1
    }
    return ptr1
}