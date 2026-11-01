package recursion_backtracking

/**
 * LeetCode: https://leetcode.com/problems/reverse-linked-list/
 * Difficulty: Easy
 * Core pattern: Recursive reversal using call stack (or iterative)
 */
class Node<T>(val data: T, var next: Node<T>? = null) {
    companion object {
        fun create(data: T, next: Node<T>? = null): Node<T> {
            return Node(data, next)
        }
    }
}

// Recursive solution
fun reverseRecursive(head: Node<Int>?): Node<Int>? {
    if (head == null || head!!.next == null) return head  // Base case: single or empty node
    val newHead = reverseRecursive(head!!.next)
    head.next!!.next = head  // Swap links: new tail → old head
    head.next = null         // New tail's next should be null
    return newHead
}

// Iterative solution (usually preferred for interviews)
fun reverseIterative(head: Node<Int>?): Node<Int>? {
    var prev: Node<Int>? = null
    var curr = head

    while (curr != null) {
        val nextTemp = curr.next  // Save reference to next node before breaking link
        curr.next = prev          // Reverse the link
        prev = curr               // Move prev forward
        curr = nextTemp           // Move curr forward
    }

    return prev
}
