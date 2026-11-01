package mixed_revision

import kotlin.math.maxOf
import kotlin.math.minOf

/**
 * Definition for singly-linked list.
 */
class ListNode(var `val`: Int) {
    var next: ListNode? = null
}

/**
 * LeetCode: https://leetcode.com/problems/merge-k-sorted-lists/
 * Difficulty: Hard
 * Core pattern: Min-heap + Linked list
 *
 * Time Complexity: O(n log k) where n = total nodes, k = number of lists
 * Space Complexity: O(k) for heap
 */
fun mergeKLists(lists: Array<ListNode?>): ListNode? {
    // Min-heap of (value, listIndex, node)
    val heap = java.util.PriorityQueue<Triple<Int, Int, ListNode>>(compareBy { it.first })

    // Initialize heap with first node from each list
    for (i in lists.indices) {
        if (lists[i] != null) {
            heap.offer(Triple(lists[i]!!.`val`, i, lists[i]!!))
        }
    }

    val dummy = ListNode(0)  // Dummy head for easier list building
    var current: ListNode? = dummy

    while (heap.isNotEmpty()) {
        val (val, listIdx, node) = heap.poll()
        current!!.next = node
        current = node

        // If this node has a next, add it to heap
        if (node.next != null) {
            heap.offer(Triple(node.next!!.`val`, listIdx, node.next!!))
        }
    }

    return dummy.next
}

/**
 * Explanation:
 * - Min-heap stores (value, listIndex, node) from each list's current head
 * - Repeatedly extract minimum node, add to result, push next node from same list
 * - Each node pushed/popped once → O(n log k) total (heap operations O(log k))
 * - O(k) space for heap (at most k nodes in heap at any time)
 *
 * Alternative: Divide and conquer (merge pairs) — same O(n log k) time, O(1) space
 *
 * Why log k: heap size is at most k (one node per list at a time)
 */