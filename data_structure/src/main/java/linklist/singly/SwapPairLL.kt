package linklist.singly




fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.addNode(1)
    singlyLL.addNode(2)
    singlyLL.addNode(3)
    singlyLL.addNode(4)

    singlyLL.printSinglyLL()
    println()
    val newHead = swapPairLL(singlyLL.head)


    var temp = newHead
    while(temp != null){
        print("${temp.data} ->")
        temp = temp.next
    }
    print("Null")
}


/***
 *
 *
 *  prev    curr    3
 *  1       2       3
 *
 *  curr    prev
 *  2       1       3
 *
 */
fun swapPairLL(head: SinglyLLNode<Int>?): SinglyLLNode<Int>? {

    var prev: SinglyLLNode<Int>? = head
    var curr: SinglyLLNode<Int>? = head?.next

    var newHead = head?.next // After first swap, this becomes the new head

    while (curr != null){
        prev?.next = curr.next
        curr.next = prev
        prev = prev?.next
        curr = prev?.next?.next
    }

    return newHead

}