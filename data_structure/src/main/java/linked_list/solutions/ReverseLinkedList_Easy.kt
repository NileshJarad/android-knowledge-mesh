package linked_list

/**
 * LeetCode: https://leetcode.com/problems/reverse-linked-list/
 * Difficulty: Easy
 * Core pattern: Iterative reversal with three pointers
 */
class Node<T>(val data: T, var next: Node<T>? = null)

fun reverseList(head: Node<Int>?): Node<Int>? {
    var prev: Node<Int>? = null
    var curr = head

    while (curr != null) {
        val nextTemp = curr.next  // Save reference to next node before breaking link
        curr.next = prev          // Reverse the link
        prev = curr               // Move prev forward
        curr = nextTemp          // Move curr forward
    }

    return prev
}
