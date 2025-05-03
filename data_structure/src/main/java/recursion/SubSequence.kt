package recursion


fun main() {
//    val array = intArrayOf(3, 1, 2)
//    subSequence(0, array, array.size, mutableListOf())


    val str = "Nil" // keep in mind that it has O(2^N) complexity larger string slows the program and above 100 length will be almost impossible
    printAllSubSequence(0, str, str.length, StringBuilder())
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


fun printAllSubSequence(index: Int, str: String, size: Int, result: StringBuilder) {
    if (index >= size) {
        println(result)
        return
    }

    // pick condition
    val c = str[index]
    result.append(c)
    printAllSubSequence(index + 1, str, size, result)
    result.deleteAt(result.length - 1)
    printAllSubSequence(index + 1, str, size, result)
}