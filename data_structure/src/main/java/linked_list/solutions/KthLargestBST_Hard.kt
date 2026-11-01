package linked_list

/**
 * LeetCode: https://leetcode.com/problems/kth-largest-element-in-a-binary-search-tree/
 * Difficulty: Medium (often appeared in Hard variant interviews)
 * Core pattern: In-order traversal + QuickSelect or Heap
 */
class TreeNode(var `val`: Int, var left: TreeNode? = null, var right: TreeNode? = null) {
    companion object {
        fun create(val: Int, left: TreeNode? = null, right: TreeNode? = null): TreeNode {
            return TreeNode(val, left, right)
        }
    }
}

// Approach 1: In-order + QuickSelect O(N) average
fun kthLargestQuickSelect(root: TreeNode?, k: Int): Int {
    // Collect all values via in-order traversal (O(N))
    val values = mutableListOf<Int>()
    inorderCollect(root, values)
    return quickSelect(values, k - 1, values.size - 1)  // 0-indexed, so k-1 for kth largest
}

private fun inorderCollect(node: TreeNode?, list: MutableList<Int>) {
    if (node == null) return
    inorderCollect(node!!.left, list)
    list.add(node.val)
    inorderCollect(node.right, list)
}

private fun quickSelect(nums: List<Int>, k: Int): Int {
    if (nums.size == 1) return nums[0]

    // Partition function around pivot nums[k]
    val lo = 0
    val hi = nums.lastIndex

    while (lo <= hi) {
        val pivotIndex = partition(nums, lo, hi, k)
        when {
            pivotIndex == k -> return nums[pivotIndex]
            pivotIndex < k -> lo = pivotIndex + 1
            else -> hi = pivotIndex - 1
        }
    }

    return -1  // Should never reach here if k is valid
}

private fun partition(nums: List<Int>, lo: Int, hi: Int, pivotIdx: Int): Int {
    val pivot = nums[pivotIdx]
    var storeIdx = lo
    for (i in lo until hi) {
        if (nums[i] < pivot) {
            val temp = nums[storeIdx]
            nums[storeIdx] = nums[i]
            nums[i] = temp
            storeIdx++
        }
    }
    val temp2 = nums[pivotIdx]
    nums[pivotIdx] = nums[storeIdx]
    nums[storeIdx] = temp2
    return storeIdx
}

// Approach 2: Min-Heap of size k during in-order traversal O(N log K)
import java.util.PriorityQueue

fun kthLargestHeap(root: TreeNode?, k: Int): Int {
    val pq = PriorityQueue<Int>()  // max heap by default; we want largest first, add smaller to keep top

    fun inorderHeap(node: TreeNode?) {
        if (node == null) return
        inorderHeap(node!!.left)
        pq.offer(node.val)
        if (pq.size > k) pq.poll()  // Remove smallest from heap, keep top largest k
        inorderHeap(node.right)
    }

    inorderHeap(root)
    return pq.peek() ?: throw IllegalArgumentException("No kth largest element found")
}
