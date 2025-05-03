package dp

/****
 * https://leetcode.com/problems/climbing-stairs/description/
 */

fun main() {
    print(climStairs(43))
}

fun climStairs(n: Int): Int {
    if (n <= 2) {
        return n
    }
    return climStairs(n - 1) + climStairs(n - 2)
}