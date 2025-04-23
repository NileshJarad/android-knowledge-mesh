# LinkList

- A linked list is a linear data structure in which elements,
  called nodes, are connected using pointers or references.
  Each node in a linked list contains two components: the data and a reference to the next/previous node in the list.

**Advantage of LinkList over the array**

- Dynamic Size
- Easy reordering
- Efficient insertion and deletion
- Flexible memory allocation

**Disadvantage of LinkList over the array**

- Random access not possible
- Pointer memory overhead
- Not support array like operation sorting/ binary search

---

## Types of LinkList

### Singly Link List

- Forward sequential movement is possible
- [Singly Link List Code](singly/SinglyLL.kt)

| Operation          | Time Complexity | 
|--------------------|-----------------|
| Insert at position | O(N)            | 
| Delete at position | O(N)            | 
| Search             | O(N)            | 
| Access at position | O(N)            |
| Insert head        | O(1)            |
| Delete head        | O(1)            |

#### Question on Singly LinkList

- [Reverse a Linked List: Reverse the order of nodes in a linked list](singly/ReverseSinglyLL.kt)
- [Detect Cycle in a Linked List: Determine whether a linked list contains a cycle (i.e., there is a loop in the list)](singly/DetectCycleInLL.kt).
- [Find Middle of Linked List: Find the middle node of a linked list. If the list has an even number of nodes, return the
  second middle node.](singly/FindMiddleNode.kt)
- [Merge Two Sorted Lists: Given two sorted linked lists, merge them into a single sorted linked list.](singly/MergeSortedLinkList.kt)
- [Remove Nth Node From End of List: Given a linked list, remove the nth node from the end of the list and return the modified list](singly/NthNode.kt)
- [Remove Duplicates from Sorted List: Remove any duplicate nodes from a sorted linked list, keeping only distinct values](singly/RemoveDuplicateFromSorted.kt)
- [Intersection of Two Linked Lists: Given two linked lists, determine if they intersect and return the intersection
  node.](singly/Intersection.kt)
- [Palindrome Linked List: Check if a linked list is a palindrome (the elements read the same forwards and backward).](singly/PalindromLL.kt)
- [Remove Linked List Elements: Given a linked list and a value, remove all nodes with the given value from the list](singly/RemoveAllNodeWithGivenValue.kt)
- [Swap Nodes in Pairs: Swap adjacent nodes in a linked list in pairs.](singly/SwapPairLL.kt)

---

### Doubly Link List

- Forward and Backward sequential movement is possible