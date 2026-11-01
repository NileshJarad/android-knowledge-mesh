package linked_list

/**
 * LeetCode: https://leetcode.com/problems/reverse-linked-list-ii/
 * Difficulty: Medium
 * Core pattern: Fast & slow pointer to find midpoint, then reverse second half and merge
 */
class Node<T>(val data: T, var next: Node<T>? = null) {
    companion object {
        fun create(data: T, next: Node<T>? = null): Node<T> {
            return Node(data, next)
        }
    }
}

fun reverseKGroup(head: Node<Int>?, k: Int): Node<Int>? {
    if (head == null || k <= 1) return head

    // Step 1: Reverse k nodes at a time to build reversed segments
    var prevHead = null
    var current = head
    var segmentCount = 0

    while (current != null && segmentCount < k) {
        val nextTemp = current!!.next
        current.next = prevHead  // Prepend current node to result
        prevHead = current
        current = nextTemp
        segmentCount++
    }

    // Now prevHead points to new head of first reversed segment
    // If there are remaining nodes (< k), we still reverse them but append after the last segment
    if (current != null) {
        // Reverse the remaining nodes too (handles case where total length isn't multiple of k)
        while (current != null && segmentCount < k) {
            val nextTemp = current!!.next
            current.next = prevHead
            prevHead = current
            current = nextTemp
            segmentCount++
        }
    }

    return prevHead
}
