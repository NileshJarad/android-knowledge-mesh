package recursion


fun main() {
    val array = intArrayOf(3, 1, 2)

    subSequence(0, array, array.size, mutableListOf())

}

fun subSequence(index: Int, list: IntArray, size: Int, result: MutableList<Int>) {

    if (index >= size) {
        println(result)
        return
    }
    result.add(list[index]) // take condition
    subSequence(index + 1, list, size, result)
    result.removeLast() // not take
    subSequence(index + 1, list, size, result)
}