package hashing

/**
 * LeetCode: https://leetcode.com/problems/find-the-duplicate-number/
 * Difficulty: Easy
 * Core pattern: HashMap counting / Floyd's cycle detection (tortoise and hare)
 */

// Solution A: HashMap counting - O(N) time, O(N) space
fun findDuplicateHashMap(nums: IntArray): Int {
    val seen = mutableSetOf<Int>()
    for (num in nums) {
        if (num in seen) return num
        seen.add(num)
    }
    return -1 // No duplicate found
}

// Solution B: Floyd's Cycle Detection - O(N) time, O(1) space
fun findDuplicateFloyd(nums: IntArray): Int {
    var slow = nums[0]
    var fast = nums[0]
    while (true) {
        slow = nums[slow]
        fast = nums[nums[fast]]
        if (slow == fast) break
    }
    slow = nums[0]
    while (slow != fast) {
        slow = nums[slow]
        fast = nums[fast]
    }
    return slow
}
