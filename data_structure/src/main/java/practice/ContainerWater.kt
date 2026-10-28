package practice


/*****
 * ou are given an integer array height of length n.
 * There are n vertical lines drawn such that the two endpoints of
 * the $i$-th line are at $(i, 0)$ and $(i, \text{height}[i])$.Find two lines that,
 * together with the x-axis, form a container such that the container contains the most water.
 * Return the maximum amount of water a container can store.Note:
 * You may not slant the container.
 * Example:height = [1, 8, 6, 2, 5, 4, 8, 3, 7] -> 49 (The max water is between index 1 (height 8) and index 8 (height 7).
 * Distance = $8 - 1 = 7$, min height = 7, so $7 \times 7 = 49$).
 */

class ContainerWater {
    fun getMaxWater(height: IntArray): Int {
        var left = 0
        var right = height.size - 1
        var maxWater = 0
        while (left < right) {
            val leftHeight = height[left]
            val rightHeight = height[right]
            val width = right  - left
            val area = width * minOf(leftHeight, rightHeight)
            maxWater = maxOf(maxWater, area)
            if(leftHeight < rightHeight) {
                left++
            }else{
                right--
            }
        }
        return  maxWater
    }
}

fun main() {
    val height = intArrayOf(1, 8, 6, 2, 5, 4, 8, 3, 7)
    val containerWater = ContainerWater().getMaxWater(height)
    println(containerWater)
}