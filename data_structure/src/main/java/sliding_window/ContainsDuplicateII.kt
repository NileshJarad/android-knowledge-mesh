package sliding_window

import java.util.*

fun main() {
    /**
     * https://leetcode.com/problems/contains-duplicate-ii/description
     */
    println("containsNearbyDuplicate (1,2,3,1) 3  true =  ${containsNearbyDuplicate(intArrayOf(1, 2, 3, 1), 3)}")
    println("containsNearbyDuplicate (1,0,1,1) 1 true =  ${containsNearbyDuplicate(intArrayOf(1, 0, 1, 1), 1)}")
    println(
        "containsNearbyDuplicate (1,2,3,1,2,3) 2 false =  ${
            containsNearbyDuplicate(
                intArrayOf(1, 2, 3, 1, 2, 3),
                2
            )
        }"
    )
    println("containsNearbyDuplicate (1,2,1) 0 false=  ${containsNearbyDuplicate(intArrayOf(1, 2, 1), 0)}")
    println(
        "containsNearbyDuplicate (0,1,2,3,2,5) 3 true=  ${
            containsNearbyDuplicate(
                intArrayOf(0, 1, 2, 3, 2, 5),
                3
            )
        }"
    )
}


fun containsNearbyDuplicate(nums: IntArray, k: Int): Boolean {
    // Map that tracks num -> last seen index
    val map = HashMap<Int, Int>()
    for (i in nums.indices) {
        if (map.containsKey(nums[i])) {
            val lastSeenIndex = map[nums[i]]!!
            if (Math.abs(i - lastSeenIndex) <= k) return true
        }
        map[nums[i]] = i
    }
    return false
}

