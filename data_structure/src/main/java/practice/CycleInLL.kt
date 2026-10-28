package practice

fun main() {
    // 1. Build the nodes
    val head = SinglyNode(1)
    val second = SinglyNode(2)
    val third = SinglyNode(3)
    val fourth = SinglyNode(4)
    val fifth = SinglyNode(5)
    val six = SinglyNode(6)
    val seven = SinglyNode(7)

    // 2. Link them sequentially
    head.next = second
    second.next = third
    third.next = fourth
    fourth.next = fifth
    fifth.next = six
    six.next = seven

    // 3. Create the cycle: 7 points back to 3
    seven.next = third

    // 4. Test the detection function
    val cycleStartNode = detectCycleStart(head)

    if (cycleStartNode != null) {
        println("Cycle starts at node with value: ${cycleStartNode.data}") // Should print 3
    } else {
        println("No cycle detected.")
    }
}

fun detectCycleStart2(head: SinglyNode?): SinglyNode? {
    var slow = head
    var fast = head
    var hasCycle = false

    while (fast?.next != null) {
        slow = slow?.next
        fast = fast.next?.next

        if (slow == fast) {
            hasCycle = true
            break
        }
    }

    if (!hasCycle) return null
    print("Meet point is ${slow?.data}")

    fast = head // Reset fast pointer back to the head
    while (slow != fast) {
        slow = slow?.next
        fast = fast?.next
    }

    return slow
}

fun detectCycleStart(head: SinglyNode?): SinglyNode? {
    var slow = head
    var fast = head
    var hasCycle = false

    // Phase 1: Detect if a cycle exists
    while (fast != null && fast.next != null) {
        slow = slow?.next
        fast = fast.next?.next

        if (slow == fast) {
            hasCycle = true
            break
        }
    }

    if (!hasCycle) return null

    // Phase 2: Find the exact node where the cycle starts
    fast = head // Reset fast to head
    while (slow != fast) {
        slow = slow?.next
        fast = fast?.next
    }

    return slow
}