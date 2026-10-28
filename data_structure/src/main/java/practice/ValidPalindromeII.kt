package practice

/****
 *
 * The Task: Given a string s, return true if the string can be a palindrome
 * after deleting at most one character from it.Example: s = "abca" -> true
 * (because you can delete the character 'b' or 'c', leaving "aca" or "aba",
 * which are both valid palindromes).
 */
class ValidPalindromeII {
    fun check(str: String): Boolean {
        var left = 0
        var right = str.length - 1
        while (left <= right) {
            if (str[left] != str[right]) {
                return isPalindrome(str, left +1, right) || isPalindrome(str, left, right -1)
            }
            left++
            right--
        }
        return true
    }

    private fun isPalindrome(str: String, left: Int, right: Int): Boolean {
        var l = left
        var r = right
        while (l < r) {
            if(str[l] != str[r]) return false
            l++
            r--
        }
        return true
    }
}

fun main() {
    val str1 = "abca"
    println("String $str1 is palindrome = ${ValidPalindromeII().check(str1)}")
    val complexString = "abcdeca"
    println("String $complexString is palindrome = ${ValidPalindromeII().check(complexString)}")
}