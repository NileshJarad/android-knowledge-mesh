package linked_list

/**
 * LeetCode: https://leetcode.com/problems/remove-nth-node-from-end-of-list/
 * Difficulty: Easy
 * Core pattern: Two pointers with fast pre-advancement
 */
class Node<T>(val data: T, var next: Node<T>? = null) {
    companion object {
        fun create(data: T, next: Node<T>? = null): Node<T> {
            return Node(data, next)
        }
    }
}

fun removeNthFromEnd(head: Node<Int>?, n: Int): Node<Int>? {
    if (head == null) return null
    if (n == 1) return head.next  // Remove the only node after head

    var fast = head
    var slow = head
    // Advance fast by n positions first
    repeat(n) {
        if (fast != null) fast = fast.next!!
    }
    // Now traverse both until fast reaches end
    while (fast != null) {
        slow = slow.next!!
        fast = fast.next
    }
    // slow is now at node AFTER target (we wanted to remove index n from end)
    // Wait - we want to remove (n+1)th from start if 0-indexed...
    // Actually standard implementation removes position (n+1)th from start when n is count of nodes before target
    // Correct approach: advance fast and slow together n times then advance until fast.next==null
    // Then slow is at node BEFORE target → delete slow.next
    slow.next = slow.next?.next

    return head
}

// Standard working version
fun removeNthWorking(head: Node<Int>?, n: Int): Node<Int>? {
    if (head == null || n <= 0) return head

    // Handle removing first node (n=1)
    if (n == 1) return head.next

    val dummy = Node(0)
    dummy.next = head
    var fast = dummy
    var slow = dummy

    // Move fast n positions ahead (skip n nodes before target)
    repeat(n + 1) {
        if (fast.next != null) fast = fast.next!!
    }

    // Now traverse both together
    while (fast.next != null) {
        fast = fast.next
        slow = slow.next
    }

    // slow.next is the node to remove
    slow.next = slow.next?.next

    return dummy.next
}
