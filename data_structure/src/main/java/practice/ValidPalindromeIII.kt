package practice

class ValidPalindromeIII {
    fun checkCValidPalindrome(str: String, deletionsLeft: Int = 0): Boolean {
        return canBePalindrome(str, 0, str.length - 1, deletionsLeft)
    }

    private fun canBePalindrome(str: String, left: Int, right: Int, deletionsLeft: Int): Boolean {
        var l = left
        var r = right
        while (l < r) {
            if (str[l] != str[r]) {
                if (deletionsLeft <= 0) {
                    return false
                } else {
                    return canBePalindrome(
                        str, l + 1, r, deletionsLeft - 1
                    ) || canBePalindrome(
                        str, l, r - 1, deletionsLeft - 1
                    )
                }
            }
            l++
            r--
        }
        return true
    }
}


fun main() {
    val str1 = "abca"
    println("String $str1 is palindrome = ${ValidPalindromeIII().checkCValidPalindrome(str1,1)}")
    val complexString = "abcdeca"
    println("String $complexString is palindrome = ${ValidPalindromeIII().checkCValidPalindrome(complexString,2)}")
}