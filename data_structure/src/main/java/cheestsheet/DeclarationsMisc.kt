package cheestsheet

import java.util.Stack

fun main() {


    val stack = ArrayDeque<Int>()
    stack.add(10)
    stack.add(20)
    stack.add(30)


    println("Stack= $stack")

    println("Stack last = ${stack.lastOrNull()}")
    println("Stack top = ${stack.removeLast()}")

    println("Stack= $stack")

    println("Stack is empty ${stack.isEmpty()}")


    /***
     *
     */

}