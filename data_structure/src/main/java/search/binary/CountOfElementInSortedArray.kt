package search.binary

fun main() {
    val intArray = intArrayOf(1, 2, 4, 4, 4, 4, 4, 6, 6, 6, 7, 8, 9)
    println("Count  of 4 = ${countElements(target = 4, arr = intArray)}")
    println("Count  of 6 = ${countElements(target =6, arr = intArray)}")
    println("Count  of 16 = ${countElements(target = 16, arr = intArray)}")
    println("Count  of 9 = ${countElements(target = 9, arr = intArray)}")
}

fun countElements(target: Int, arr: IntArray): Int {
    val firstIndex = findFirstOccurrence(target, arr)
    if (firstIndex == -1) return firstIndex
    val lastIndex = findLastOccurrence(target, arr)
    return lastIndex - firstIndex + 1
}