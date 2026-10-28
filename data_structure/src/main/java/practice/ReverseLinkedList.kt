package practice


class SinglyNode(var data: Int) {
    var next: SinglyNode? = null
}

fun main() {
    val head = SinglyNode(1)
    val second = SinglyNode(2)
    val third = SinglyNode(3)
    val fourth = SinglyNode(4)
    val fifth = SinglyNode(5)

    head.next = second
    second.next = third
    third.next = fourth
    fourth.next = fifth
    printLL(head)


    val middle = middleOfLL(head)
    printLL(middle)

    val newHead = reverseLL(head)
    println()
    printLL(newHead)
}

fun printLL(head: SinglyNode?) {
    var temp: SinglyNode? = head
    while (temp != null) {
        print("${temp.data} -> ")
        temp = temp.next
    }
    println()
}

fun middleOfLL(head: SinglyNode?): SinglyNode? {
    if (head == null) return null
    if (head.next == null) return head

    var slow = head
    var fast = head

    while (fast?.next != null) {
        slow = slow?.next
        fast = fast.next?.next
    }
    return slow
}


fun reverseLL(head: SinglyNode?): SinglyNode? {
    var prev: SinglyNode? = null
    var curr = head
    var next: SinglyNode?

    while (curr != null) {
        next = curr.next
        curr.next = prev
        prev = curr
        curr = next
    }

    return prev
}

