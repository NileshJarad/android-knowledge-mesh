package mixed_revision

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/valid-palindrome/
 * Difficulty: Easy
 * Core pattern: Two pointers from both ends
 *
 * Time Complexity: O(n)
 * Space Complexity: O(1)
 */
fun isPalindrome(s: String): Boolean {
    var left = 0
    var right = s.length - 1

    while (left < right) {
        // Skip non-alphanumeric from left
        while (left < right && !s[left].isLetterOrDigit()) {
            left++
        }
        // Skip non-alphanumeric from right
        while (left < right && !s[right].isLetterOrDigit()) {
            right--
        }

        // Compare characters (case-insensitive)
        if (s[left].lowercaseChar() != s[right].lowercaseChar()) {
            return false
        }

        left++
        right--
    }

    return true
}

/**
 * Explanation:
 * - Two pointers start at both ends, move toward center
 * - Skip non-alphanumeric characters (spaces, punctuation)
 * - Compare characters case-insensitively
 * - If any pair doesn't match → not palindrome
 * - Single pass O(n), constant space O(1)
 */