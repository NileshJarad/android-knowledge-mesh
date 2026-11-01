package linked_list

/**
 * LeetCode: https://leetcode.com/problems/linked-list-cycle/
 * Difficulty: Medium
 * Core pattern: Floyd's tortoise & hare cycle detection
 */
class Node<T>(val data: T, var next: Node<T>? = null)

fun hasCycle(head: Node<Int>?): Boolean {
    var slow: Node<Int>? = head
    var fast: Node<Int>? = head

    while (fast != null && fast.next != null) {
        slow = slow!!.next   // Move 1 step
        fast = fast.next.next // Move 2 steps
        if (slow === fast) return true  // They met → cycle exists
    }

    return false
}

// Variant to find cycle starting node
fun detectCycle(head: Node<Int>?): Pair<Node<Int>?, Boolean> {
    if (hasCycle(head)) {
        // Find where cycle starts
        var slow = head
        var fast = head
        while (fast != null && fast.next != null) {
            slow = slow!!.next
            fast = fast.next.next
            if (slow === fast) {
                // Reset one pointer to head, move both at same speed
                val slowPtr = head
                val fastPtr = head
                while (slowPtr !== fastPtr) {
                    slowPtr = slowPtr!!.next
                    fastPtr = fastPtr!!.next
                }
                return Pair(slowPtr, true)
            }
        }
    }
    return Pair(null, false)
}
