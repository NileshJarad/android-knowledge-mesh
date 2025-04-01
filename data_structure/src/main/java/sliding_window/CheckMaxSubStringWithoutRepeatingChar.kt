package sliding_window

fun main() {
    /**
     * https://leetcode.com/problems/longest-substring-without-repeating-characters/
     */
//    val str = "aaabcdd"
//    val str = "abcabcbb"
    val str = "bbbbb"
    println("Long = ${lengthOfLongestSubstring(str)}")
}

fun lengthOfLongestSubstring(s: String): Int {
    val charSet = mutableSetOf<Char>()
    var result = 0
    var left = 0
    var right = 0

    while (s.length > right) {
        if (charSet.contains(s[right])) {
            charSet.remove(s[left])
            left++
        } else {
            result = maxOf(result,right - left + 1)
            charSet.add(s[right])
            right++
        }
    }
    return result
}