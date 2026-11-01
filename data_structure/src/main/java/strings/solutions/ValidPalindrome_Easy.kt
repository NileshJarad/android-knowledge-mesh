package strings

/**
 * LeetCode: https://leetcode.com/problems/valid-palindrome/
 * Difficulty: Easy
 * Core pattern: Two pointers with case-insensitive comparison
 */
fun isPalindrome(s: String): Boolean {
    var left = 0
    var right = s.length - 1
    while (left < right) {
        // Skip non-alphanumeric characters from left
        while (left < right && !s[left].isLetterOrDigit()) left++
        // Skip non-alphanumeric characters from right
        while (left < right && !s[right].isLetterOrDigit()) right--
        if (s[left].uppercase() != s[right].uppercase()) return false
        left++
        right--
    }
    return true
}
