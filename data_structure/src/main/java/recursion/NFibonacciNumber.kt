package recursion


fun main() {
    val num = 0
    println("N the fibonacci number = ${nFibonacciNumber(num)}")
}

fun nFibonacciNumber(n: Int): Int {
    if (n == 1 || n == 2) return 1
    return nFibonacciNumber(n - 1) + nFibonacciNumber(n - 2)

}