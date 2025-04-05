package recursion

fun main() {
    printIncreasing(5)
    println()
    printDecreasing(5)
}


fun printDecreasing(num: Int) {
    // base condition
    if (num == 0) {
        return
    }
    // Going toward  base condition
    print("$num ")
    printDecreasing(num - 1)
}


fun printIncreasing(num: Int) {
    // base condition
    if (num == 0) {
        return
    }
    printIncreasing(num - 1)
    // coming from base condition
    print("$num ")
}
