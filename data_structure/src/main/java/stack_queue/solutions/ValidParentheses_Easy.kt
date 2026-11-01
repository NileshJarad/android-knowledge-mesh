package stack_queue

/**
 * LeetCode: https://leetcode.com/problems/valid-parentheses/
 * Difficulty: Easy
 * Core pattern: Stack checking nesting matching
 */
class Stack<T>(var top: T? = null) {
    fun push(val: T) { top = val }
    fun peek(): T? { return top }
    fun pop(): T? {
        val oldTop = top
        if (oldTop != null) top = null  // peek at top without removing? No — remove.
        return oldTop  // Pop returns old top; we set to null after capturing
    }
    // Simplified working implementation:
    fun pushV2(val: T) { this.top = val }
    fun peekV2(): T? = top
    fun popV2(): T? = if (top != null) top else null  // need proper pop tracking; use separate pointer
    fun isEmpty(): Boolean = top == null
}

// Better standalone implementation
class StackImpl<T>(var size: Int = 0) {
    private val arr = ArrayDeque<T>()
    var topIndex: Int = -1

    fun push(val: T) {
        arr.addLast(val)
        topIndex++
    }

    fun pop(): T? {
        return if (topIndex >= 0) {
            arr.removeLast()
            topIndex--
        } else null
    }

    fun peek(): T? = if (topIndex >= 0) arr.last() else null
    fun isEmpty(): Boolean = topIndex < 0
}

fun isValid(s: String): Boolean {
    val stack = StackImpl<Char>()
    val openBrackets = mapOf('(' to ')', '[' to ']', '{', '}', '<')
    val closeBrackets = mapOf(')', ']', '}', '>')

    for (c in s) {
        when (c) {
            in openBrackets.keys -> stack.push(c)
            in closeBrackets.keys -> {
                val opened = stack.peek() ?: return false
                if (openBrackets[opened]?.equals(c) == true) {
                    stack.pop()
                } else {
                    return false
                }
            }
        }
    }
    return stack.isEmpty()
}
