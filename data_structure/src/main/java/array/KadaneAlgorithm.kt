package array

/***
 * kaden's algorithm state that keep two sum one for running sum and other for max sum
 *
 * Better ignore the current sum if it's negative
 */
fun main() {
    val array = intArrayOf(-2, 1, -3, 4, -1, 2, 1, -5, 4)
    var maxSoFar = array[0]
    var runningSum = array[0]

    for (i in 1 until array.size) {
        runningSum = maxOf(array[i], runningSum + array[i])
        maxSoFar = maxOf(maxSoFar, runningSum)
    }

    println("max sum = $maxSoFar")
}
