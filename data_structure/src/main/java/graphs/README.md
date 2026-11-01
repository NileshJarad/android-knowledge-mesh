# Topic 13 - Graphs: BFS, DFS, Connected Components, Cycle Detection, Topological Sort

**Difficulty:** Medium  
**Estimated study time:** 3-4 hours  

---

## 1. Concept explanation

### What a Graph is
A Graph is a non-linear data structure consisting of **nodes (vertices)** connected by **edges**. Unlike trees, graphs can have cycles and any node can be connected to any other node.

### Types of graphs
1. **Undirected Graph**: Edges have no direction (A-B is same as B-A)
2. **Directed Graph**: Edges have direction (A→B ≠ B→A)
3. **Weighted Graph**: Edges have weights/costs associated
4. **Unweighted Graph**: All edges are equal
5. **Cyclic Graph**: Contains at least one cycle
6. **Acyclic Graph**: No cycles

### How it works internally

**Adjacency List** (most common):
```
graph = {
  0: [1, 2],
  1: [0, 3],
  2: [0],
  3: [1]
}
```

**Adjacency Matrix**:
```
   0 1 2 3
0  0 1 1 0
1  1 0 0 1
2  1 0 0 0
3  0 1 0 0
```

### Real-life analogy
A Graph is like a city map where locations are nodes and roads are edges. Navigation apps use graphs to find shortest paths, social networks use graphs to find friends-of-friends, and airline route maps use graphs to find connections.

### Time and space complexity

| Operation | Adjacency List | Adjacency Matrix | Notes |
|-----------|---------------|------------------|-------|
| Add edge | O(1) | O(1) | List: append to neighbors |
| Remove edge | O(degree) | O(1) | List: find and remove neighbor |
| Get neighbors | O(degree) | O(V) | List: iterate neighbors |
| Check if edge exists | O(degree) | O(1) | List: search neighbors |
| BFS/DFS traversal | O(V + E) | O(V²) | V = vertices, E = edges |
| Space | O(V + E) | O(V²) | List saves space for sparse graphs |

### Common mistakes and edge cases
1. **Disconnected graph**: Not all nodes reachable from start — need to iterate all nodes
2. **Cycles**: Infinite loops without visited tracking
3. **Self-loops**: Node pointing to itself
4. **Parallel edges**: Multiple edges between same pair
5. **Empty graph**: No nodes or no edges
6. **Single node**: Trivial case

### When to use in interviews
- **Network connectivity**: Social networks, computer networks
- **Shortest path**: Navigation, routing
- **Cycle detection**: Deadlock detection, dependency resolution
- **Topological sort**: Task scheduling, course prerequisites, build systems
- **Connected components**: Friend circles, network clusters

---

## 2. Important patterns

### Pattern 1: BFS (Breadth-First Search)
**Explore level by level using a queue**.

**Example**: Find shortest path in unweighted graph

**Brute force**: Try all possible paths (exponential)

**Better**: DFS (not guaranteed shortest)

**Optimal**: BFS (guarantees shortest path in unweighted graphs)

### Pattern 2: DFS (Depth-First Search)
**Explore as deep as possible using recursion or stack**.

**Example**: Check if path exists between two nodes

**Brute force**: Check all permutations

**Better**: BFS (also works but uses more memory)

**Optimal**: DFS (simple, O(V+E) time, O(V) space for recursion)

### Pattern 3: Connected Components
**Count groups of connected nodes in undirected graph**.

**Example**: Friend circles in social network

**Approach**: Run BFS/DFS from each unvisited node, count iterations

**Example**: Nodes {0-1-2}, {3-4} → 2 components

### Pattern 4: Cycle Detection
**Detect if graph contains a cycle**.

**Undirected graph**: DFS with parent tracking
**Directed graph**: DFS with recursion stack / coloring (white, gray, black)

**Example**: Course prerequisites with circular dependency → cycle detected

### Pattern 5: Topological Sort
**Linear ordering of vertices such that for every directed edge u→v, u comes before v**.

**Example**: Course schedule where course A must be taken before course B

**Approach**: Kahn's algorithm (BFS with indegree) or DFS-based

**Example**: Courses [0,1], prerequisites [[1,0]] → order: [0,1]

---

## 3. Dry run

Let's walk through BFS on a graph:

**Graph (Adjacency List)**:
```
0: [1, 2]
1: [0, 2]
2: [0, 1, 3]
3: [2]
```

**BFS from node 0**:
```
Step 1: Queue = [0], Visited = {0}, Distance[0] = 0
Step 2: Dequeue 0. Neighbors: 1, 2. Queue = [1, 2]. Visited = {0, 1, 2}
Step 3: Dequeue 1. Neighbors: 0(visited), 2(visited). Queue = [2]
Step 4: Dequeue 2. Neighbors: 0(v), 1(v), 3. Queue = [3]. Visited = {0, 1, 2, 3}
Step 5: Dequeue 3. Neighbors: 2(v). Queue = []
```

**Traversal order**: 0 → 1 → 2 → 3

**State at each step**:
| Step | Queue | Visited | Action |
|------|-------|---------|--------|
| 1 | [0] | {0} | Start at 0 |
| 2 | [1, 2] | {0, 1, 2} | Process 0, add 1, 2 |
| 3 | [2] | {0, 1, 2} | Process 1, no new nodes |
| 4 | [3] | {0, 1, 2, 3} | Process 2, add 3 |
| 5 | [] | {0, 1, 2, 3} | Process 3, done |

---

## 4. Code template (Kotlin)

```kotlin
class Graph(val numVertices: Int) {
    // Adjacency list representation
    private val adj: MutableList<MutableList<Int>> = Array(numVertices) { mutableListOf() }
    
    fun addEdge(u: Int, v: Int, directed: Boolean = false) {
        adj[u].add(v)
        if (!directed) {
            adj[v].add(u)
        }
    }
    
    // BFS - Breadth First Search
    fun bfs(start: Int): List<Int> {
        val visited = BooleanArray(numVertices)
        val result = mutableListOf<Int>()
        val queue = ArrayDeque<Int>()
        
        visited[start] = true
        queue.add(start)
        
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node)
            
            for (neighbor in adj[node]) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true
                    queue.add(neighbor)
                }
            }
        }
        return result
    }
    
    // DFS - Depth First Search (iterative)
    fun dfsIterative(start: Int): List<Int> {
        val visited = BooleanArray(numVertices)
        val result = mutableListOf<Int>()
        val stack = ArrayDeque<Int>()
        
        stack.push(start)
        
        while (stack.isNotEmpty()) {
            val node = stack.pop()
            if (visited[node]) continue
            
            visited[node] = true
            result.add(node)
            
            for (neighbor in adj[node].reversed()) {
                if (!visited[neighbor]) {
                    stack.push(neighbor)
                }
            }
        }
        return result
    }
    
    // DFS - Recursive
    fun dfsRecursive(start: Int): List<Int> {
        val visited = BooleanArray(numVertices)
        val result = mutableListOf<Int>()
        dfsHelper(start, visited, result)
        return result
    }
    
    private fun dfsHelper(node: Int, visited: BooleanArray, result: MutableList<Int>) {
        visited[node] = true
        result.add(node)
        for (neighbor in adj[node]) {
            if (!visited[neighbor]) {
                dfsHelper(neighbor, visited, result)
            }
        }
    }
    
    // Cycle Detection for Undirected Graph
    fun hasCycleUndirected(): Boolean {
        val visited = BooleanArray(numVertices)
        for (i in 0 until numVertices) {
            if (!visited[i]) {
                if (hasCycleDFS(i, -1, visited)) return true
            }
        }
        return false
    }
    
    private fun hasCycleDFS(node: Int, parent: Int, visited: BooleanArray): Boolean {
        visited[node] = true
        for (neighbor in adj[node]) {
            if (!visited[neighbor]) {
                if (hasCycleDFS(neighbor, node, visited)) return true
            } else if (neighbor != parent) {
                return true // Back edge found
            }
        }
        return false
    }
    
    // Cycle Detection for Directed Graph (using coloring)
    fun hasCycleDirected(): Boolean {
        // 0 = white (unvisited), 1 = gray (in progress), 2 = black (done)
        val color = IntArray(numVertices) // all 0 by default
        for (i in 0 until numVertices) {
            if (color[i] == 0) {
                if (hasCycleDirectedDFS(i, color)) return true
            }
        }
        return false
    }
    
    private fun hasCycleDirectedDFS(node: Int, color: IntArray): Boolean {
        color[node] = 1 // gray - in progress
        for (neighbor in adj[node]) {
            if (color[neighbor] == 1) return true // back edge = cycle
            if (color[neighbor] == 0) {
                if (hasCycleDirectedDFS(neighbor, color)) return true
            }
        }
        color[node] = 2 // black - done
        return false
    }
    
    // Connected Components count
    fun countComponents(): Int {
        val visited = BooleanArray(numVertices)
        var count = 0
        for (i in 0 until numVertices) {
            if (!visited[i]) {
                bfsFrom(i, visited)
                count++
            }
        }
        return count
    }
    
    private fun bfsFrom(start: Int, visited: BooleanArray) {
        val queue = ArrayDeque<Int>()
        visited[start] = true
        queue.add(start)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for (neighbor in adj[node]) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true
                    queue.add(neighbor)
                }
            }
        }
    }
    
    // Topological Sort using Kahn's Algorithm (BFS-based)
    fun topologicalSort(): List<Int>? {
        val indegree = IntArray(numVertices)
        for (u in 0 until numVertices) {
            for (v in adj[u]) {
                indegree[v]++
            }
        }
        
        val queue = ArrayDeque<Int>()
        for (i in 0 until numVertices) {
            if (indegree[i] == 0) queue.add(i)
        }
        
        val result = mutableListOf<Int>()
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node)
            for (neighbor in adj[node]) {
                indegree[neighbor]--
                if (indegree[neighbor] == 0) queue.add(neighbor)
            }
        }
        
        return if (result.size == numVertices) result else null // null = cycle exists
    }
}

// Usage example
fun main() {
    val g = Graph(4)
    g.addEdge(0, 1)
    g.addEdge(0, 2)
    g.addEdge(1, 2)
    g.addEdge(2, 3)
    
    println("BFS from 0: ${g.bfs(0)}")      // [0, 1, 2, 3]
    println("DFS from 0: ${g.dfsRecursive(0)}")  // [0, 1, 2, 3]
    println("Has cycle (undirected): ${g.hasCycleUndirected()}") // true (0-1-2-0)
    println("Components: ${g.countComponents()}") // 1
    println("Topological sort: ${g.topologicalSort()}") // null (cycle exists)
}
```

**Line by line explanation**:

1. **Graph class**: Encapsulates adjacency list with `numVertices`
2. **addEdge**: Adds edge; if undirected, adds both directions
3. **bfs**: Uses queue, level-by-level exploration, O(V+E)
4. **dfsIterative**: Uses stack, simulates recursion manually
5. **dfsRecursive**: Classic recursive DFS with visited tracking
6. **hasCycleUndirected**: DFS with parent tracking to avoid false positives
7. **hasCycleDirected**: Coloring approach (white/gray/black) for directed graphs
8. **countComponents**: Run BFS from each unvisited node
9. **topologicalSort**: Kahn's algorithm using indegree counting

---

## 5. Practice problems

### Easy 1: Find the Town Judge
**Problem**: In a town, find the judge who trusts nobody but is trusted by everybody else.

**LeetCode**: [997. Find the Town Judge](https://leetcode.com/problems/find-the-town-judge/)

**Core pattern**: Graph indegree/outdegree (Connected components variant)

**Hint**: Track how many people trust each person and how many each person trusts. Judge has indegree = N-1, outdegree = 0.

**Expected time complexity**: O(N + Trust.length)

### Easy 2: Find if Path Exists in Graph
**Problem**: Determine if there's a valid path from source to destination in an undirected graph.

**LeetCode**: [1971. Find if Path Exists in Graph](https://leetcode.com/problems/find-if-path-exists-in-graph/)

**Core pattern**: BFS/DFS connectivity check

**Hint**: Build adjacency list, run BFS/DFS from source, check if destination is reached.

**Expected time complexity**: O(N + E)

### Medium 1: Number of Islands
**Problem**: Count the number of islands in a 2D grid where '1' is land and '0' is water.

**LeetCode**: [200. Number of Islands](https://leetcode.com/problems/number-of-islands/)

**Core pattern**: Connected Components on grid (BFS/DFS)

**Hint**: Treat grid cells as nodes, adjacent land cells as edges. Count connected components of '1's.

**Expected time complexity**: O(M × N)

### Medium 2: Course Schedule
**Problem**: Determine if all courses can be finished given prerequisites (detect cycle in directed graph).

**LeetCode**: [207. Course Schedule](https://leetcode.com/problems/course-schedule/)

**Core pattern**: Cycle Detection in Directed Graph

**Hint**: Build directed graph from prerequisites, detect cycle using coloring or Kahn's algorithm.

**Expected time complexity**: O(N + Prerequisites.length)

### Hard 1: Course Schedule II
**Problem**: Return the ordering of courses to finish all courses given prerequisites.

**LeetCode**: [210. Course Schedule II](https://leetcode.com/problems/course-schedule-ii/)

**Core pattern**: Topological Sort (Kahn's Algorithm)

**Hint**: Use Kahn's algorithm — process courses with no prerequisites first, reduce indegree of dependents.

**Expected time complexity**: O(N + Prerequisites.length)

### Hard 2: Alien Dictionary
**Problem**: Given sorted words in an alien language, determine the order of characters.

**LeetCode**: [269. Alien Dictionary](https://leetcode.com/problems/alien-dictionary/)

**Core pattern**: Topological Sort with graph building from strings

**Hint**: Compare adjacent words to find character ordering edges, then topological sort.

**Expected time complexity**: O(C) where C = total characters in all words

---

**Next steps**: After completing Graphs, move to topic 14: Advanced Graphs**

Do you want:

a) more practice problems,

b) a detailed solution for one problem,

c) a quiz to test my understanding,

or the next topic?

---

