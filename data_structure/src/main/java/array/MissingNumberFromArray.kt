package array


/***
 * Missing number from array can be find by using summation numbers
 *
 * 1,2,3,4,5,7,.8,9,10 --- (6 is missing)
 *
 * Sum = (N*(N+1))/2
 *
 *
 *  ans = sum - sumOfArrayElements
 *
 */
fun main() {
    val n = 10
    val summation = (n * (n + 1)) / 2
    var sumOfElementInArray = 0
    val intArray = intArrayOf(1, 2, 3, 4, 5, 7, 8, 9, 10)
    intArray.forEach {
        sumOfElementInArray += it
    }

    println("Missing number is ${(summation - sumOfElementInArray)}")
}
