package recursion

import kotlin.math.abs


fun main() {
    val num = 2
    val pow = 3


    val numD = 2.0
    val powD = -2

    println("Power of ($num, $pow) =  ${power(num, pow)}")
    println("Improved Power of ($numD, $powD) =  ${myPow(numD, powD)}")

}

fun power(x: Int, pow: Int): Long {
    if (pow == 0) {
        return 1
    }
    return x * power(x, pow - 1)
}

/***
 * Improved version
 */


fun myPow(x: Double, n: Int): Double {
    val base = if (n < 0) 1.0 / x else x
    return exp(base, abs(n))
}
private fun exp(x: Double, n: Int) : Double {
    if (n == 0) return 1.0
    val half = exp(x, n / 2)
    return if (n % 2 == 0) {
        half * half
    } else {
        x * half * half
    }
}
