# Tree

Tree is hierarchical (Non-Linear) data structure.

## Tree concepts (terminology)

[PlatUml Tree](https://www.plantuml.com/plantuml/uml/SoWkIImgoStCIybDBE0goIzGACbNICelASdFLKZ9B4fDBidCp-FIKd3aqj9ISEBI0fAkG198UK8eBmYL3PPo0wbo1ZFT8U8-4CeN9E42bl3WSaZDIu7Q2000)

![alt](http://www.plantuml.com/plantuml/png/SoWkIImgoStCIybDBE0goIzGACbNICelASdFLKZ9B4fDBidCp-FIKd3aqj9ISEBI0fAkG198UK8eBmYL3PPo0wbo1ZFT8U8-4CeN9E42bl3WSaZDIu7Q2000)

- **Root** - Top most element in tree
    - A is root of tree
- **Node** - Which store the information
    - A,B,C,D,E,F,G,H,I,J,K,L,M
- **Parent** - who having child below it
    - G is parent of L and M
    - B is parent of E and F
- **Child** - having parent
    - L and M are child of the G
    - H is child of D
- **Leaf Node** - having no child
    - I,J,K,F,L,M and H
- **Non-Leaf Node (Internal Node)** - having at-least one child
    - A, B,C,D,E nad G are Non-Leaf node
- **Ancestor** - Any predecessor from root node till that node
    - Ancestor of L are G,C and A
    - Ancestor of H are D and A
- **Descendants** Any predecessor from that node till leaf node
    - Descendants of C are G,L and M
    - Descendants of C are E,I,J, K and F
- **Siblings** - All children of same parent
    - E and F are siblings
    - B, C and D are siblings
- **Degree** - No of children of node
    - Degree of A is 3
    - Degree of G is 2
    - Degree of D is 1
    - Degree of L is 0
- **Depth** - Length of longest path from root to that node (no of edges)
    - Depth of F is 2
    - Depth of L is 3
    - Depth of D is 1
    - Depth of A is 0
- **Height** - length of longest path till leaf node
    - Height of B is 2
    - Height of A is 3
    - Height of D is 1

---

## Types of tree

- [Binary tree](BinaryTree.kt)
- [Binary search tree](BinarySearchTree.kt)
    - It is Binary tree.
    - It has special property that left and right of tree should be BST
    - All node in Left subtree should be less than **Root**
    - All node in Right subtree should be larger than **Root**
    - Equal value can be put in left or right but not in both
    - In-Order traversal of BST will always sorted
    - Search is faster in **_average_** case **O(log N)**. It depends on height of three (skewed tree)
- AVL tree
- B tree
- B+ tree
- Heap tree
- Red Black tree

 ---

## Tree traversal

- **Pre-Order** (Node, Left, Right)
- **In-Order** (Left, Node, Right)
- **Post-Order** (Left, Right, Node)
- **Level Order Traversal**
- [Traversal's code](Traversal.kt)



### Question on the tree
- Check if tree is BST
- Print left view of the tree
- Print right view of the tree
- Print top view of the tree
- [Print all elements in given range](AllElementsInRange.kt)



