package search.binary


fun main() {
    val intArray = intArrayOf(1, 2, 4, 6, 7, 8, 9)

    println("Floor of 5 = ${floorInArray(5, intArray)}")
}

fun floorInArray(num: Int, arr: IntArray): Int {
    var result = 0
    var left = 0
    var right = arr.size - 1

    while (left <= right) {
        val mid = (left + right).ushr(1)
        when {
            arr[mid] == num -> return mid
            arr[mid] < num -> { // move right
                result = mid
                left = mid + 1
            }
            else -> right = mid - 1 // move to left
        }
    }

    return arr[result]
}