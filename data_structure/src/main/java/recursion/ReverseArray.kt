package recursion

fun main() {
    val array = arrayOf(1, 2, 3, 4, 5)
    println("Array ${array.joinToString(separator = ",", prefix = "[", postfix = "]")}")
    reverseArray(array, 0, array.size - 1)
    println("Reverse Array ${array.joinToString(separator = ",", prefix = "[", postfix = "]")}")


    val array2 = arrayOf(1, 2, 3, 4, 5)
    reverseUsingSinglePointer(array2, 0, array.size )
    println("Reverse Array ${array2.joinToString(separator = ",", prefix = "[", postfix = "]")}")
}


fun reverseArray(arr: Array<Int>, start: Int, end: Int) {
    if (start >= end) return
    val num = arr[start]
    arr[start] = arr[end]
    arr[end] = num
    reverseArray(arr, start + 1, end - 1)
}

fun reverseUsingSinglePointer(arr: Array<Int>, start: Int, size: Int) {
    if (start >= size / 2) return

    val num = arr[start]
    val i = size - start - 1
    arr[start] = arr[i]
    arr[i] = num
    reverseArray(arr, start + 1, start)
}