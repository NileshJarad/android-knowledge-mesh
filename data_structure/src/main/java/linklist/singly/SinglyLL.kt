package linklist.singly


fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.printSinglyLL()
    singlyLL.addNode(3)
    singlyLL.addNode(5)
    singlyLL.addNode(6)
    singlyLL.addNode(8)
    singlyLL.addNode(12)
    singlyLL.addNode(1)
    singlyLL.addNode(25)
    singlyLL.printSinglyLL()
    println()
    singlyLL.addNodeAtPosition(222,2)
    println("After insertionad position 2")
    singlyLL.printSinglyLL()
    println()
    singlyLL.deleteNode(12)
    println("After deletion 12")
    singlyLL.printSinglyLL()

    println("\nAfter adding head as 4")
    singlyLL.addFirst(4)
    singlyLL.printSinglyLL()
    println("\n Size of LL = ${singlyLL.length(singlyLL.head)}")
}


data class SinglyLLNode<T>(
    val data: T, var next: SinglyLLNode<T>? = null
)

class SinglyLL<T> {

    var head: SinglyLLNode<T>? = null

    /**
     * Time complexity O(N) as we have to traverse the link list
     */
    fun addNode(data: T) {
        head?.let {
            var runnerNext = head
            while (runnerNext?.next != null) {
                runnerNext = runnerNext.next
            }
            runnerNext?.next = getNode(data)
        } ?: run {
            head = getNode(data)
        }
    }


    /***
     *
     */
    fun addNodeAtPosition(data: T, position: Int) {
        if (position < 0) {
            return
        }
        if (position == 1 || position == 0) {
            addFirst(data)
        }

        var currPos = 1
        var runner = head
        // below loop will take runner one step behind the position
        while (head?.next != null && currPos < position) {
            runner = runner?.next
            currPos++
        }

        // user has provided position that is out of list
        if(currPos < position - 1){
            return
        }

        val insertNode = SinglyLLNode(data, runner?.next)
        runner?.next = insertNode
    }

    /**
     * Time complexity O(1) as we have to traverse the link list
     */
    fun addFirst(data: T) {
        head = SinglyLLNode<T>(data = data, head)
    }


    /**
     * Time complexity O(N) as we have to traverse the link list
     */
    fun printSinglyLL() {
        head?.let {
            var runnerNext: SinglyLLNode<T>? = it
            while (runnerNext != null) {
                print(" ${runnerNext.data} ->")
                runnerNext = runnerNext.next
            }
            print(" NULL")
        } ?: run {
            println("Link list is empty")
        }
    }

    private fun getNode(data: T) = SinglyLLNode(data = data)

    /**
     * Time complexity O(N) as we have to traverse the link list
     */
    fun deleteNode(data: T) {
        head?.let {
            // check if user want to delete first node that is head
            if (it.data == data) {
                head = head?.next
                return
            }

            var current: SinglyLLNode<T>? = it
            var prev: SinglyLLNode<T>? = it

            while (current != null) {
                if (current.data == data) { // we found the node
                    prev?.next = current.next
                    return
                }
                prev = current
                current = current.next
            }

        } ?: run {
            println("Link list is empty")
        }
    }

    fun length(head: SinglyLLNode<Int>?): Int {
        if (head == null) {
            return 0
        }
        return 1 + length(head.next)
    }

}