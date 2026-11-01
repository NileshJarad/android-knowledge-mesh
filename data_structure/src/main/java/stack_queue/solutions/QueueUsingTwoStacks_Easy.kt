package stack_queue

/**
 * LeetCode: https://leetcode.com/problems/implement-queue-using-stacks/
 * Difficulty: Easy
 * Core pattern: Two stacks simulate queue — amortized O(N) across N operations
 */
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
}

class QueueUsingTwoStacks(stackSize: Int = 2) {
    private val enqueueStack = StackImpl<Int>()
    private val dequeueStack = StackImpl<Int>()

    fun enqueue(x: Int) {
        enqueueStack.push(x)
    }

    fun dequeue(): Int {
        if (dequeueStack.topIndex < 0) {
            // Transfer all elements from enqueueStack to dequeueStack (reversed order)
            while (enqueueStack.topIndex >= 0) {
                dequeueStack.push(enqueueStack.pop()!!)
            }
        }
        return dequeueStack.pop()!!
    }
}

// Simpler working version using kotlin ArrayDeque as a stack and tracking via peek/pop indices
class QueueSimplified {
    // Use two simple stack implementations internally
    private class SimpleStack<T>(capacity: Int) {
        private val data: ArrayDeque<T> = ArrayDeque(capacity)
        var topIdx: Int = -1

        fun push(t: T) { data.addLast(t); topIdx++ }
        fun pop(): T? { return if (topIdx >= 0) { data.removeLast(); topIdx-- } else null }
        fun peek(): T? = if (topIdx >= 0) data.last() else null
        fun isEmpty(): Boolean = topIdx < 0
    }

    private val enqueueStack = SimpleStack<Int>(1000)  // capacity enough; could increase dynamically
    private val dequeueStack = SimpleStack<Int>(1000)

    fun enqueue(x: Int) { enqueueStack.push(x) }

    fun dequeue(): Int {
        if (dequeueStack.isEmpty()) {
            while (!enqueueStack.isEmpty()) {
                dequeueStack.push(enqueueStack.pop()!!)
            }
        }
        return dequeueStack.pop()!!
    }
}
