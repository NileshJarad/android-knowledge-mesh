package tree

import java.util.*


fun main(){
    val bstRep = """        
        2
       / \
      1   5
         /  \
        3    8
              \
              15
             /
            9
"""

    val bst = BinarySearchTree()
    val root = bst.createBst(arrayOf(2, 5, 3, 8, 15, 9, 1))

    print(bstRep)
    println("Pre- Order")
    root?.preOrderTraversal()
    println("\nPre- Order Iterative")
    root?.preOrderIterativeTraversal()

    println()
    println("In- Order")
    root?.inOrderTraversal()
    println("\nIn- Order Iterative")
    root?.inOrderIterativeTraversal()

    println()
    println("Post- Order")
    root?.postOrderTraversal()
    println("\nPost- Order Iterative")
    root?.postOrderIterativeTraversal()
}


fun <T> BinaryNode<T>.postOrderTraversal() {
    this.left?.postOrderTraversal()
    this.right?.postOrderTraversal()
    print("$data ")
}


fun <T> BinaryNode<T>.postOrderIterativeTraversal() {
    if (this == null) return

    val stack1 = ArrayDeque<BinaryNode<T>>() // First stack for processing
    val stack2 = ArrayDeque<T>() // Second stack for postorder sequence

    stack1.addLast(this)

    while (stack1.isNotEmpty()) {
        val node = stack1.removeLast()
        stack2.addLast(node.data) // Store node in second stack

        // Push left and right children onto stack1
        node.left?.let { stack1.addLast(it) }
        node.right?.let { stack1.addLast(it) }
    }

    // Print in postorder (since stack2 holds Root → Right → Left)
    while (stack2.isNotEmpty()) {
        print("${stack2.removeLast()} ")
    }
}


fun <T> BinaryNode<T>.inOrderTraversal() {
    this.left?.inOrderTraversal()
    print("$data ")
    this.right?.inOrderTraversal()
}

fun <T> BinaryNode<T>.inOrderIterativeTraversal(){
    val stack = ArrayDeque<BinaryNode<T>>()
    var current: BinaryNode<T>? = this

    while (current != null || stack.isNotEmpty()) {
        // Traverse to the leftmost node
        while (current != null) {
            stack.addLast(current)
            current = current.left
        }

        // Pop and process the node
        current = stack.removeLast()
        print("${current.data} ") // Process node

        // Move to right subtree
        current = current.right
    }
}


fun <T> BinaryNode<T>.preOrderTraversal() {
    print("$data ")
    this.left?.preOrderTraversal()
    this.right?.preOrderTraversal()
}


fun <T> BinaryNode<T>.preOrderIterativeTraversal(){
    val stack = ArrayDeque<BinaryNode<T>>()
    stack.push(this)
    while (stack.isNotEmpty()) {
        val popped = stack.pop()
        print("${popped.data} ")
        popped.right?.let { stack.push(it) }
        popped.left?.let { stack.push(it) }
    }

}

fun <T> BinaryNode<T>.levelOrder() {
    val queue = LinkedList<BinaryNode<T>>()
    queue.add(this)
    while (!queue.isEmpty()) {
        val pop = queue.poll()
        print("${pop?.data} ")
        if (pop.left != null) {
            queue.add(pop.left!!)
        }
        if (pop.right != null) {
            queue.add(pop.right!!)
        }
    }
}

fun <T> BinaryNode<T>.levelOrder2() {
    val que = LinkedList<BinaryNode<T>?>()
    que.offer(this)
    while (que.isNotEmpty()) {
        val size = que.size
        for (i in 0 until size) {
            val node = que.poll()
            if (node?.left != null) {
                que.offer(node.left)
            }
            if (node?.right != null) {
                que.offer(node.right)
            }
            print("${node?.data} ")
        }
        println()
    }
}


fun <T> BinaryNode<T>.levelOrderWithNewLine() {
    val queue = LinkedList<BinaryNode<T>?>()
    queue.add(this)
    queue.add(null) // Add null as a marker for the end of the level
    while (!queue.isEmpty()) {
        val poll = queue.poll()
        if (poll == null) {
            println()
            if (queue.isNotEmpty())
                queue.add(null)

        } else {
            print("${poll?.data} - ")
            if (poll.left != null) {
                queue.add(poll.left!!)
            }
            if (poll.right != null) {
                queue.add(poll.right!!)
            }
        }
    }
}


fun <T> BinaryNode<T>.verticalOrderTraversal() {

}