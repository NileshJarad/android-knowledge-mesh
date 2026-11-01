# Topic 14 - Advanced Graphs: Union-Find, Dijkstra, Bellman-Ford, Minimum Spanning Tree

**Difficulty:** Hard  
**Estimated study time:** 4-5 hours  

---

## 1. Concept explanation

### Union-Find (Disjoint Set Union - DSU)
Union-Find data structure maintains a collection of disjoint sets with operations:
- **Find(x)**: Find representative of element x's set
- **Union(x, y)**: Merge sets containing x and y

### How Union-Find works internally
**Path Compression + Union by Rank**:
```
Find(x):
    if parent[x] != x:
        parent[x] = Find(parent[x])  # path compression
    return parent[x]

Union(x, y):
    rootX = Find(x), rootY = Find(y)
    if rootX != rootY:
        if rank[rootX] < rank[rootY]:
            parent[rootX] = rootY
        elif rank[rootX] > rank[rootY]:
            parent[rootY] = rootX
        else:
            parent[rootY] = rootX
            rank[rootX]++
```

### Real-life analogy
Union-Find is like grouping people by relationships:
- Initially everyone is in their own group
- When A befriends B → Union(A,B) 
- When C befriends D → Union(C,D)
- Query if A and D are friends → Find(A) == Find(D)

### Time and space complexity

| Operation | Time Complexity | Space Complexity | Notes |
|-----------|----------------|------------------|-------|
| Find | O(α(N)) ≈ O(1) | O(N) | α(N) is inverse Ackermann |
| Union | O(α(N)) | O(N) | 
| Initialization | O(N) | O(N) | 

Where α(N) ≤ 5 for all practical N ≤ 2^64

### Common mistakes and edge cases
1. **Path compression during find**: Don't forget to update parent pointers
2. **Union by rank**: Always attach smaller tree to larger tree
3. **Indexing**: Arrays often 0-indexed vs 1-indexed people
4. **Self-loops**: Union(x,x) should do nothing
5. **Disconnected components**: Many separate trees possible

### When to use in interviews
- **Connectivity queries**: Offline connectivity problems
- **Cycle detection**: Kruskal's MST, social network analysis
- **Component counting**: Number of connected groups
- **Dynamic connectivity**: Add/remove edges and query connectivity

---

### Dijkstra's Algorithm
**Single-source shortest paths in weighted graphs with non-negative weights**

### How Dijkstra works internally
Priority queue + relaxation:
```
Initialize distances to ∞, distance[source] = 0
PQ = {source} with weight 0

while PQ not empty:
    u = PQ.extractMin()  // vertex with smallest distance
    for each edge (u,v,weight):
        alt = distance[u] + weight
        if alt < distance[v]:
            distance[v] = alt
            PQ.update(v, alt)  // decrease key
```

### Real-life analogy
Shortest path routing in road networks:
- Start at a source city
- Expand to neighboring cities
- Keep track of shortest known distances
- Always explore the closest unvisited city

### Time and space complexity

| Implementation | Time Complexity | Space Complexity | When to use |
|----------------|----------------|------------------|-------------|
| Priority queue (binary heap) | O((V+E) log V) | O(V+E) | General graphs |
| Fibonacci heap | O(V log V + E) | O(V) | Theoretical optimal |

### Common mistakes and edge cases
1. **Negative weights**: Dijkstra fails with negative weights
2. **Priority queue implementation**: Use TreeSet or Binary Heap
3. **Distance initialization**: Use Integer.MAX_VALUE carefully
4. **Visited tracking**: Don't need explicit visited if using distances
5. **Large graphs**: Watch out for memory and time constraints

### When to use in interviews
- **Navigation**: GPS routing, map applications
- **Network routing**: Shortest path in communication networks
- **Project scheduling**: Critical path problems
- **Resource allocation**: Minimize cost/maximize benefit

---

### Bellman-Ford Algorithm
**Single-source shortest paths that can handle negative weights**

### How Bellman-Ford works internally
```
Initialize distances to ∞, distance[source] = 0

for i = 1 to V-1:
    for each edge (u,v,weight):
        if distance[u] + weight < distance[v]:
            distance[v] = distance[u] + weight

// Check for negative cycles
for each edge (u,v,weight):
    if distance[u] + weight < distance[v]:
        return "Negative cycle detected"
```

### Real-life analogy
Financial arbitrage detection:
- Edge weights represent currency exchange rates
- Negative cycles = arbitrage opportunities
- Useful for detecting profitable trading cycles

### Time and space complexity

| Operation | Time Complexity | Space Complexity | Notes |
|-----------|----------------|------------------|-------|
| All iterations | O(VE) | O(V) | Simple to implement |
| Negative cycle detection | O(E) | O(1) | Additional pass |

### Common mistakes and edge cases
1. **Negative cycles**: Must detect and report them
2. **Edge relaxation order**: All edges must be relaxed V-1 times
3. **Distance update**: Use temporary variable to avoid overwriting
4. **Initial distances**: All source distances start at ∞ except source
5. **Infinite loops**: With negative cycles, algorithm may not terminate

### When to use in interviews
- **Negative weights**: Edge cases where weights can be negative
- **Negative cycle detection**: Financial algorithms, physics simulations
- **Distance calculations**: When Dijkstra's limitations don't apply

---

### Minimum Spanning Tree (MST) Algorithms
**Connect all vertices with minimum total edge weight**

Two main algorithms:
1. **Prim's**: Grow tree from a starting vertex
2. **Kruskal's**: Add edges in increasing weight order (uses Union-Find)

### How MST works internally (Prim's)
```
visited = {start}
priority_queue = all edges from start

while pq not empty:
    edge = pq.extractMin()
    if target endpoint not in visited:
        add edge to MST
        add its other edges to pq
        mark target as visited
```

### Real-life analogy
Building infrastructure:
- Cities = vertices, roads = edges with costs
- Want to connect all cities with minimum road cost
- MST finds optimal network layout

### Time and space complexity

| Algorithm | Time | Space | Priority queue |
|-----------|------|-------|----------------|
| Prim's (heap) | O((V+E) log V) | O(V+E) | Binary heap |
| Prim's (array) | O(V²) | O(V²) | Array for dense |
| Kruskal's | O(E log E) | O(V+E) | Sort edges first |

### Common mistakes and edge cases
1. **Disconnected graph**: MST doesn't exist, return empty
2. **Self-loops**: Ignore edges from vertex to itself
3. **Duplicate edges**: Handle only minimum weight of parallel edges
4. **Edge weight equality**: Multiple valid MSTs possible
5. **Large V vs E**: Choose algorithm based on graph density

### When to use in interviews
- **Network design**: Telephone, electrical, computer networks
- **Cluster analysis**: Group similar items with minimum cost
- ** Approximation**: Use as subroutine for other problems
- **Resource allocation**: Connect all locations with minimum cost

---

## 2. Important patterns

### Pattern 1: Offline Connectivity Queries
**Multiple connectivity questions solved efficiently with offline processing**

**Example**: Roads network maintenance - determine which repairs affect connectivity

**Approach**:
1. Process all queries
2. Sort edges and queries
3. Use Union-Find to maintain connectivity as we add edges
4. Answer queries based on current state

### Pattern 2: Shortest Path with Constraints
**Find shortest paths with additional constraints**

**Example**: Delivery routes with limited fuel capacity

**Approach**:
1. Use Dijkstra with state augmentation
2. State = (vertex, remaining_resource)
3. Track multiple dimensions in priority queue

### Pattern 3: Negative Weight Handling
**When weights can be negative, use Bellman-Ford**

**Example**: Financial transaction networks with currency exchange fees

**Approach**:
1. Use Bellman-Ford for shortest path
2. Detect negative cycles
3. May require additional steps for cycle detection

### Pattern 4: MST-based Solutions
**Transform problems to MST formulation**

**Example**: Clustering points with minimum spanning tree

**Approach**:
1. Convert to complete graph with edge weights = distances
2. Run MST algorithm (Prim or Kruskal)
3. Clusters based on tree structure

### Pattern 5: 0-1 BFS
**Special case of Dijkstra for weights 0 and 1**

**Example**: Multi-level storage systems

**Approach**:
1. Use deque instead of priority queue
2. Add to front for weight 0, back for weight 1
3. Time: O(V+E) - better than Dijkstra's O((V+E) log V)

---

## 3. Dry run

Let's walk through Kruskal's algorithm step by step:

**Graph with edges**:
```
edges = [
    (A,B,1), (B,C,3), (A,C,2), 
    (C,D,4), (D,E,5), (B,E,6),
    (A,E,7), (D,A,8), (B,D,9)
]
```

**Step 1**: Sort edges by weight:
```
(A,B,1) weight=1
(A,C,2) weight=2  
(B,C,3) weight=3
(C,D,4) weight=4
(D,E,5) weight=5
(B,E,6) weight=6
(A,E,7) weight=7
(D,A,8) weight=8
(B,D,9) weight=9
```

**Step 2**: Process each edge:
```
1. (A,B,1): Add to MST. Components: {A,B}, {C}, {D}, {E}
2. (A,C,2): Add to MST. Components: {A,B,C}, {D}, {E}
3. (B,C,3): Skip (both in same component)
4. (C,D,4): Add to MST. Components: {A,B,C,D}, {E}
5. (D,E,5): Add to MST. Components: {A,B,C,D,E} (all connected)
6. Remaining edges: All skipped (would create cycles)
```

**Result MST edges**: [(A,B,1), (A,C,2), (C,D,4), (D,E,5)]
**Total weight**: 1+2+4+5 = 12

**Visualization**:
```
    B    C
   / \   |
  A --- D - E
```

### State tracking with Union-Find:
```
Initially: A: parent=A, rank=0
           B: parent=B, rank=0
           C: parent=C, rank=0
           D: parent=D, rank=0
           E: parent=E, rank=0

After (A,B): A: parent=B, rank=1
             B: parent=B, rank=0

After (A,C): A: parent=B, B: parent=C, rank=1,1
             C: parent=C, rank=0

After (C,D): A: parent=B, B: parent=C, rank=1,1
             C: parent=D, D: parent=D, rank=1,0
             E: parent=E, rank=0

After (D,E): A: parent=B, B: parent=C, rank=1,1
             C: parent=D, D: parent=E, rank=1,1
             E: parent=E, rank=0
```

---

## 4. Code template (Kotlin)

```kotlin
import java.util.PriorityQueue

class UnionFind(var n: Int) {
    val parent = IntArray(n) { it }
    val rank = IntArray(n) { 0 }
    var components = n

    fun find(x: Int): Int {
        if (parent[x] != x) {
            parent[x] = find(parent[x]) // path compression
        }
        return parent[x]
    }

    fun union(x: Int, y: Int): Boolean {
        val rx = find(x)
        val ry = find(y)
        if (rx == ry) return false // already in same component

        // union by rank
        if (rank[rx] < rank[ry]) {
            parent[rx] = ry
        } else if (rank[rx] > rank[ry]) {
            parent[ry] = rx
        } else {
            parent[ry] = rx
            rank[rx]++
        }
        components--
        return true
    }
}

class Graph {
    private val adj = mutableListOf<MutableList<Pair<Int, Int>>>()
    private val n: Int

    constructor(vertices: Int) {
        n = vertices
        repeat(n) { adj.add(mutableListOf()) }
    }

    fun addEdge(u: Int, v: Int, weight: Int) {
        adj[u].add(Pair(v, weight))
        adj[v].add(Pair(u, weight)) // undirected
    }

    // Prim's Algorithm
    fun prim(start: Int): Int {
        val visited = BooleanArray(n)
        val pq = PriorityQueue<Pair<Int, Int>> { a, b -> a.first - b.first }

        visited[start] = true
        for ((neighbor, weight) in adj[start]) {
            pq.offer(Pair(weight, neighbor))
        }

        var totalWeight = 0
        var edgesUsed = 0

        while (edgesUsed < n - 1 && pq.isNotEmpty()) {
            val (weight, u) = pq.poll()
            if (visited[u]) continue

            visited[u] = true
            totalWeight += weight
            edgesUsed++

            for ((v, w) in adj[u]) {
                if (!visited[v]) {
                    pq.offer(Pair(w, v))
                }
            }
        }

        return if (edgesUsed == n - 1) totalWeight else Int.MAX_VALUE
    }

    // Kruskal's Algorithm
    fun kruskal(): Int {
        val edges = mutableListOf<Triple<Int, Int, Int>>() // (u,v,weight)
        for (u in 0 until n) {
            for ((v, w) in adj[u]) {
                if (u < v) { // add each edge only once
                    edges.add(Triple(u, v, w))
                }
            }
        }

        edges.sortBy { it.third }
        val uf = UnionFind(n)
        var totalWeight = 0
        var edgesUsed = 0

        for ((u, v, w) in edges) {
            if (uf.union(u, v)) {
                totalWeight += w
                edgesUsed++
                if (edgesUsed == n - 1) break
            }
        }

        return if (edgesUsed == n - 1) totalWeight else Int.MAX_VALUE
    }

    // Dijkstra's Algorithm
    fun dijkstra(start: Int): IntArray {
        val dist = IntArray(n) { Int.MAX_VALUE }
        val pq = PriorityQueue<Pair<Int, Int>> { a, b -> a.first - b.first }

        dist[start] = 0
        pq.offer(Pair(0, start))

        while (pq.isNotEmpty()) {
            val (d, u) = pq.poll()
            if (d > dist[u]) continue

            for ((v, w) in adj[u]) {
                val newDist = dist[u] + w
                if (newDist < dist[v]) {
                    dist[v] = newDist
                    pq.offer(Pair(newDist, v))
                }
            }
        }

        return dist
    }

    // Bellman-Ford Algorithm
    fun bellmanFord(start: Int): Pair<IntArray, Boolean> {
        val dist = IntArray(n) { Int.MAX_VALUE }
        dist[start] = 0

        // Relax edges V-1 times
        for (i in 0 until n - 1) {
            var updated = false
            for (u in 0 until n) {
                if (dist[u] == Int.MAX_VALUE) continue
                for ((v, w) in adj[u]) {
                    if (dist[u] + w < dist[v]) {
                        dist[v] = dist[u] + w
                        updated = true
                    }
                }
            }
            if (!updated) break // no more updates
        }

        // Check for negative cycles
        var hasNegativeCycle = false
        for (u in 0 until n) {
            if (dist[u] == Int.MAX_VALUE) continue
            for ((v, w) in adj[u]) {
                if (dist[u] + w < dist[v]) {
                    hasNegativeCycle = true
                    break
                }
            }
            if (hasNegativeCycle) break
        }

        return Pair(dist, hasNegativeCycle)
    }
}

// Usage example
fun main() {
    val g = Graph(5)

    // Add edges: (u, v, weight)
    g.addEdge(0, 1, 2)
    g.addEdge(0, 3, 6)
    g.addEdge(1, 2, 3)
    g.addEdge(1, 3, 8)
    g.addEdge(2, 4, 1)
    g.addEdge(3, 4, 5)

    println("Prim's MST weight: ${g.prim(0)}")     // 11
    println("Kruskal's MST weight: ${g.kruskal()}") // 11

    val distances = g.dijkstra(0)
    println("Dijkstra from 0: ${distances.contentToString()}") // [0, 2, 5, 6, 6]

    val (bfDist, hasNegCycle) = g.bellmanFord(0)
    println("Bellman-Ford from 0: ${bfDist.contentToString()}, has negative cycle: $hasNegCycle")
}
```

**Line by line explanation**:

1. **UnionFind class**: DSU with path compression + union by rank
2. **Graph class**: Adjacency list with weighted edges
3. **prim**: Greedy algorithm using priority queue
4. **kruskal**: Sort edges + Union-Find for cycle detection
5. **dijkstra**: Priority queue + relaxation for shortest paths
6. **bellmanFord**: Relax edges V-1 times + negative cycle check

---

## 5. Practice problems

### Easy 1: Friend Circles (Social Network)
**Problem**: Given N people and M friendship relations, count friend circles (connected components).

**LeetCode**: [547. Number of Provinces](https://leetcode.com/problems/number-of-provinces/)

**Core pattern**: Connected components in undirected graph

**Hint**: Build adjacency list, use DFS/BFS to count components, or Union-Find for efficiency.

**Expected time complexity**: O(N + E) or O(N × α(N))

### Easy 2: Course Schedule (Prerequisites)
**Problem**: Given courses and prerequisites, check if all courses can be finished.

**LeetCode**: [207. Course Schedule](https://leetcode.com/problems/course-schedule/)

**Core pattern**: Cycle detection in directed graph

**Hint**: Use topological sort (Kahn's) or DFS with coloring.

**Expected time complexity**: O(N + P)

### Medium 1: Network Delay Time
**Problem**: Find the minimum time for signal to reach all nodes in directed weighted graph.

**LeetCode**: [743. Network Delay Time](https://leetcode.com/problems/network-delay-time/)

**Core pattern**: Shortest path (Dijkstra) + global optimization

**Hint**: Run Dijkstra from source, take maximum distance.

**Expected time complexity**: O(E log V)

### Medium 2: Reconstruct Itinerary
**Problem**: Given tickets in order, reconstruct valid itinerary starting from "JFK".

**LeetCode**: [332. Reconstruct Itinerary](https://leetcode.com/problems/reconstruct-itinerary/)

**Core pattern**: Hierholzer's algorithm (Eulerian path)

**Hint**: Build graph, use DFS for post-order traversal.

**Expected time complexity**: O(E log V)

### Hard 1: Minimum Cost to Connect All Points
**Problem**: Connect all points on plane with minimum total connection cost.

**LeetCode**: [1584. Min Cost to Connect All Points](https://leetcode.com/problems/min-cost-to-connect-all-points/)

**Core pattern**: MST with Euclidean distance calculation

**Hint**: Generate all edges, run Kruskal or Prim.

**Expected time complexity**: O(N² log N) for Prim, O(N²) for Kruskal

### Hard 2: Find the City With the Smallest Number of Neighbors at a Threshold Distance
**Problem**: Find city with most neighbors within threshold distance.

**LeetCode**: [1334. Find the City With the Smallest Number of Neighbors at a Threshold Distance](https://leetcode.com/problems/find-the-city-with-the-smallest-number-of-neighbors-at-a-threshold-distance/)

**Core pattern**: Floyd-Warshall + MST hybrid

**Hint**: Compute all-pairs shortest paths, then use MST properties.

**Expected time complexity**: O(N³) with Floyd-Warshall

---

**Next steps**: After completing Advanced Graphs, move to topic 15: 1-D Dynamic Programming**

Do you want:

a) more practice problems,

b) a detailed solution for one problem,

c) a quiz to test my understanding,

or the next topic?

---

