package graph

import graph.DfsTraversal.Companion.GRAPH_VERTEX
import java.util.Stack

/***
 * DFS (Depth first search)
 *  - Keep going to first neighbour and it's neighbour recursively
 *  - Then go to the next neighbour
 *  - Close to Tree PreOrder traversal
 */
fun main() {
    val dfsTraversal = DfsTraversal()
    dfsTraversal.createGraph()

    var visited = Array(GRAPH_VERTEX) {
        false
    }
    println("Dfs Recursive ")
    dfsTraversal.printDfsRecursive(0, visited, dfsTraversal.graph)


    visited = Array(GRAPH_VERTEX) {
        false
    }
    println()
    println("Dfs Iterative")
    dfsTraversal.printDfsIterative(0, visited, dfsTraversal.graph)

}


class DfsTraversal {
    companion object {
        val GRAPH_VERTEX = 7
    }

    class Edge(
        val src: Int,
        val dest: Int
    )

    val graph = Array(GRAPH_VERTEX) {
        ArrayList<Edge>()
    }

    fun createGraph() {

        /****
         *
         *      1 ------- 3
         *    /           | \
         *  0             |  5 --- 6
         *    \           | /
         *      2 ------- 4
         *
         *
         */

        graph[0].add(Edge(0, 1))
        graph[0].add(Edge(0, 2))

        graph[1].add(Edge(1, 3))

        graph[2].add(Edge(2, 4))

        graph[3].add(Edge(3, 4))
        graph[3].add(Edge(3, 5))

        graph[4].add(Edge(4, 3))
        graph[4].add(Edge(4, 5))

        graph[5].add(Edge(5, 6))
    }

    /**
     * Dfs Recursive
     * 0	1	3	4	5	6	2
     *
     */
    fun printDfsRecursive(curr: Int, visited: Array<Boolean>, graph: Array<ArrayList<Edge>>) {
        if (visited[curr]) return
        print("$curr ")
        visited[curr] = true

        graph[curr].forEach {
            printDfsRecursive(it.dest, visited,graph)
        }
    }


    /**
     * Dfs Iterative
     * 0	2	4	5	6	3	1
     */
    fun printDfsIterative(curr: Int, visited: Array<Boolean>, graph: Array<ArrayList<Edge>>) {
        val stack = Stack<Int>()

        stack.push(curr)

        while (stack.isNotEmpty()) {
            val popItem = stack.pop()
            print("$popItem ")
            visited[popItem] = true
            graph[popItem].forEach {
                if (!visited[it.dest]) {
                    stack.push(it.dest)
                }
            }
        }
    }
}