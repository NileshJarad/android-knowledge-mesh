package recursion

fun main() {
    val array = arrayOf(1, 2, 3, 4, 5)
    println("Array ${array.joinToString(separator = ",", prefix = "[", postfix = "]")}")
    reverseArray(array, 0, array.size - 1)
    println("Reverse Array ${array.joinToString(separator = ",", prefix = "[", postfix = "]")}")
}


fun reverseArray(arr: Array<Int>, start: Int, end: Int) {
    if (start >= end) return
    val num = arr[start]
    arr[start] = arr[end]
    arr[end] = num

    reverseArray(arr, start + 1, end - 1)

}