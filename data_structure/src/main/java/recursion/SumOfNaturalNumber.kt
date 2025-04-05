package recursion

fun main() {

    val n = 10
    println("Print sum of natural number ($n) sum =  ${sumOfNaturalNumber(n)}")
    println("Print sum of natural ny formula =  ${(n * (n + 1) / 2)}")
}

fun sumOfNaturalNumber(num: Int): Int {
    if (num == 1) return 1
    return num + sumOfNaturalNumber(num - 1)
}