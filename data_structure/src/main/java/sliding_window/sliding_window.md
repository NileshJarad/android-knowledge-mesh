# Sliding window




1. **Sliding Window Problem Identification**
   1. Look for problems involving sub-arrays, subsequences, or fixed-size contiguous segments. 
   2. Problems often ask for maximum, minimum, or specific sums, averages, or lengths. 
   3. Examples include finding the longest/shortest sub-array, sub-array with a given sum, or sub-array/product within a range.

2. **Sliding Window Technique:**
    - Create two pointers: left and right, initially pointing to the start of the array.
    - Maintain a window that represents the current sub-array or subsequence of interest.
    - Slide the window by moving the pointers left or right while updating the window's state.

3. **Window Size and Movements:**
    - Determine the size of the window and its movements based on the problem requirements.
    - The window can have a fixed or variable size, depending on the problem.
    - Movements can be either one element at a time or based on specific conditions (e.g., sum reaches a threshold).

4. **Tracking and Updating:**
    - Identify what needs to be tracked and updated as the window slides.
    - Common things to track include sums, counts, maximums, minimums, or other relevant metrics.
    - Update the tracked values when adding or removing elements from the window.

5. **Conditions and Terminating:**
    - Determine the conditions for terminating or extending the window.
    - Decide when to move the left and right pointers based on specific conditions or problem constraints.
    - Termination conditions may include reaching the end of the array or satisfying certain criteria.

6. **Algorithm Steps:**
    - Initialize necessary variables, pointers, and tracked values.
    - Set up the initial window by positioning the pointers accordingly.
    - Iterate through the array while updating the window and tracked values.
    - Check termination conditions and update the final result if necessary.

7. **Example:**
    - Let's take an example to understand the process better:
      Problem: Given an array, find the maximum sum of any contiguous sub-array of size K.
        - Initialize variables: `maxSum = 0`, `currentSum = 0`
        - Set up initial window: Move the right pointer `K` steps ahead.
        - Iterate through the array:
            - Add the element at the right pointer to the `currentSum`.
            - If the window size is greater than `K`, remove the leftmost element from the `currentSum`.
            - Update the `maxSum` if the `currentSum` is larger.
            - Move both pointers to the right.
        - Return `maxSum` as the final result.

Remember that this cheat sheet provides a general guideline, and you may need to adapt the approach based on the specific problem requirements.



### Question for the Stack
- [Find maximum (or minimum) sum of a subarray of size k](MaxOfMinOfSubArrayOfSizeK.kt)
- [First negative number]()