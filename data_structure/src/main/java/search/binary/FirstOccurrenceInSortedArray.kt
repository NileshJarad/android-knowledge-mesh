package search.binary

/**
 * Think of it as how to find last occurrence
 */
fun main() {
    val intArray = intArrayOf(1, 2, 4, 4, 4, 4, 4, 6, 6, 6, 7, 8, 9)

    println("First occurrence of 4 at ${findFirstOccurrence(target = 4, arr = intArray)}")
    println("Last occurrence of 4 at ${findLastOccurrence(target = 4, arr = intArray)}")
    println("First occurrence of 6 at ${findFirstOccurrence(target = 6, arr = intArray)}")
    println("Last occurrence of 16 at ${findLastOccurrence(target = 16, arr = intArray)}")
}

fun findFirstOccurrence(target: Int, arr: IntArray): Int {
    var left = 0
    var right = arr.size - 1
    var result = -1
    while (left <= right) {
        val mid = left + (right - left) / 2
        if (target <= arr[mid]) {
            // this is potential answer
            if(target == arr[mid]) {
                result = mid
            }
            right = mid - 1
        } else {
            left = mid + 1
        }
    }
    return result
}

fun findLastOccurrence(target: Int, arr: IntArray): Int {
    var left = 0
    var right = arr.size - 1
    var result = -1
    while (left <= right) {
        val mid = left + (right - left) / 2
        if (target >= arr[mid]) {
            // this is potential answer
            if(target == arr[mid]) {
                result = mid
            }
            left = mid + 1
        } else {
            right = mid - 1
        }
    }
    return result
}