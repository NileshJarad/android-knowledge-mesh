package linklist.singly


fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.addNode(1)
    singlyLL.addNode(2)
    singlyLL.addNode(2)
    singlyLL.addNode(1)

    println("Is Palindrom ${isPalindrome(singlyLL.head)}")
}

    fun isPalindrome(head: SinglyLLNode<Int>?): Boolean {
        val stack = ArrayList<SinglyLLNode<Int>?>()

        var node: SinglyLLNode<Int>? = head
        stack.add(node)
        while (node != null) {
            val next = node.next
            stack.add(next)
            node = next
        }
        stack.removeAt(stack.size - 1)

        node = head
        val min = stack.size shr 1
        while (stack.size > min) {
            if (node?.data != stack.removeAt(stack.size - 1)?.data) return false
            node = node?.next
        }
        return true
    }
