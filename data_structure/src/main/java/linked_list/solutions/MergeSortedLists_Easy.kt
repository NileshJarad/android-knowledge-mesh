package linked_list

/**
 * LeetCode: https://leetcode.com/problems/merge-two-sorted-lists/
 * Difficulty: Easy
 * Core pattern: Classic merge algorithm
 */
class Node<T>(val data: T, var next: Node<T>? = null) {
    companion object {
        fun create(data: T, next: Node<T>? = null): Node<T> {
            return Node(data, next)
        }
    }
}

fun mergeTwoLists(l1: Node<Int>?, l2: Node<Int>?): Node<Int>? {
    // Dummy head simplifies edge case where we prepend to result
    val dummy = Node(0)
    var tail = dummy

    var curr1 = l1
    var curr2 = l2

    while (curr1 != null && curr2 != null) {
        when {
            curr1!!.data <= curr2!!.data -> {
                tail.next = curr1
                curr1 = curr1.next
            }
            else -> {
                tail.next = curr2
                curr2 = curr2.next
            }
        }
        tail = tail.next!!
    }

    // Attach remaining elements from whichever list is not exhausted
    if (curr1 != null) {
        tail.next = curr1
    } else if (curr2 != null) {
        tail.next = curr2
    }

    return dummy.next
}
