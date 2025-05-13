# Graph
Network of node(vertex) and edges(connection)


---

### Graph Terminology
- **Degree of Node**
  - Number of edges connected to node is degree of that node.
  - In an undirected graph, The degree of a node is equal to the number of edges incident to that node.
    - There are two types of degrees commonly considered:
      - **In-degree:** In a directed graph, the in-degree of a node is the number of incoming edges to that node. It represents the number of edges pointing towards the node. 
      - **Out-degree:** In a directed graph, the out-degree of a node is the number of outgoing edges from that node. It represents the number of edges originating from the node
        
- **Example**
  ```agsl
         A --- B
        /       \
       D         C
        
    ```
  1. Node A has a degree of 2 because it is connected to nodes B and D.
      
  2. Node B has a degree of 2 because it is connected to nodes A and C.
    
  3. Node C has a degree of 1 because it is connected to node B.
      
  4. Node D has a degree of 1 because it is connected to node A.
      

---
### Types of graph
- **Undirected Graph**
  - An undirected graph is a graph where the edges do not have a direction
  - The edges represent a symmetric relationship between vertices. 
  - For example, if vertex A is connected to vertex B, then vertex B is also connected to vertex A.
  ```agsl
        A --- B
  ```
- **Directed Graph (Digraph)**
  - In Directed graph edges have direction
  - The edges represent asymmetric relationship between vertices, that means A is connected B, does not mean B is connected A.
  ```agsl
        A ---> B
  ```
- **Un-Weighted Graph**
  - Graph where edge does not have weight or cost associated with it
  - The connections between vertices are considered to have equal importance or distance.
- **Weighted Graph**
  - A weighted graph is a graph where each edge has an associated weight or cost
  - These weights can represent various quantities, such as distances, costs, or capacities
  - Weights can be positive, negative or zero
  ```agsl
        (10)
    A -------> B
  ```
---

### Graph representation
- **Adjacency Matrix**:
  - An adjacency matrix is a 2D matrix where each cell represents an edge between two nodes.
  - If there is an edge between nodes A and B, the corresponding cell will contain a value (usually 1), and if there is no edge, the cell will contain a special value (usually 0 or infinity).
    ```agsl
       A  B  C  D
    A  0  1  1  0
    B  1  0  0  1
    C  1  0  0  1
    D  0  1  1  0
    ```
  **Advantage** 
  1. Simplicity: Edge representation is simple. Uses 2D matrix
  2. Efficient edge queries: Checking edge takes **O(1)** time.
  3. Suitable for dense graph
 
  **Disadvantage**
  1. Space complexity: Matrix requires **O(V^2)** space.
  2. Inefficient for the sparse graph.

---

- **Adjacency List**:
  - An adjacency list represents a graph as an array (or a hash table or list) of lists
  - Each element of the array corresponds to a node, and the list associated with each node contains its adjacent nodes.
    ```agsl
    A: [B, C, D]
    B: [A, D]
    C: [A, D]
    D: [A, B, C]
    ```
    ![Graph](https://www.plantuml.com/plantuml/dpng/SoWkIImgAStDuIejJarEB4vLS8IpdE0iPpZBXSjHGLSNA0McdC4KKD0PGZb1A8VKl1IWaG00)
  
  **Advantage**
  1. Efficient memory usage it requires only **O(V+E)** space.
  2. Efficient traversal of neighbors 
  3. Easy to insert and delete edge

  **Disadvantage**
  1. Slower edge queries. Checking the edge exists between two nodes takes **O(deg(V))**
  2. Less suitable for dense graph.


---

- **Edge List**
  - The edge list representation is a simple and straightforward way to represent a graph using a list of edge
  -  In this representation, each edge of the graph is listed individually, typically with the identifiers or labels of the vertices it connects.
      ```agsl
      [(A, B), (B, C), (C, D), (D, A)]
      ```
  **Advantage**
  1. Simplicity: The edge list representation is simple and intuitive.
  2. Efficient memory usage, Requires **O(E)** space.
  3. Flexibility: edge list allows easy to addition and removal of edges.

  **Disadvantage**
  1. Slower edge queries
  2. Inefficient for certain graph operations like finding neighbors of specific node
  3. Lack of direct node information
---

### Graph Traversal
Graph traversal is the process of visiting all the nodes in a graph.
- **Depth-First Search (DFS)**: 
  - In DFS, you start at a given node and explore as far as possible along each branch before backtracking. 
  - This approach is often implemented using recursion or a stack.
  - [DFS traversal code](DfsTraversal.kt)

- **Breadth-First Search (BFS)**: 
  - In BFS, you start at a given node and explore all its neighboring nodes before moving to the next level of nodes. 
  - This approach is often implemented using a queue.
  - Is level order traversal in terms of tree
  - It goes by step by step
  - [BFS traversal code](BfsTraversal.kt)


---

### Graph Algorithms
- **Shortest Path - Dijkstra's Algorithm**
  - It is Greedy algorithm


### Question for the Graph
- Check graph is connected or not
- Check graph contains cycle or not
- Check graph is bipartite graph