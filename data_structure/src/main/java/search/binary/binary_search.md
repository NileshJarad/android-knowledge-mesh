# Binary Search

1. The array (or range) is sorted in ascending or descending order.
2. Or, the data has a monotonic behavior (always increasing or decreasing).
3. Example: nums = [1, 3, 5, 7, 9], or a function where f(x) increases with x.

--- 

### Notes:

1. When element is NOT found:
   - low will point to the next greater element (first element greater than target). 
   - high will point to the last smaller element (just smaller than target).

        ```text
        array: [1, 3, 5, 7]
        target: 4
        final: low = 2 (points to 5), high = 1 (points to 3)
        
        ```

2. In a valid sorted array:
   - low <= high during the loop 
   - Once loop exits: low > high

3. Insertion Point
     After binary search fails, low is where the element would be inserted to maintain order.

----

### Problems:

1. [Binary Search vanilla](BinarySearch.kt)
2. [Find the first occurrence in sorted array](FirstOccurrenceInSortedArray.kt)
3. [Find the count of element](CountOfElementInSortedArray.kt)
4. [Find rotation in array](FindRotationInSortedArray.kt)
5. [Find element in rotated array](FindElementInRotatedArray.kt)
6. [Find floor in rotated array](FindFloorArray.kt)
6. [Find Peak in Bitonic array](PeakInBitonicArray.kt)
7. [Search in Bitonic array](SearchInBitonicArray.kt)