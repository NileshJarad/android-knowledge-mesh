# Sliding Window


- Use it for problems involving contiguous subarrays or substrings.
- Problems involve finding a max/min/sum/average/length of a subarray or substring.
- Brute-force would be O(n²), but constraints hint you need an O(n) solution.


## Sliding Window works in two main forms:

- Fixed-size window (e.g., subarray of size k)
- Variable-size window (e.g., longest subarray that satisfies a condition)

### Fixed Size Sliding Window

```kotlin
fun maxSumOfSubArray(arr: IntArray, k: Int): Int {
    var maxSum = 0
    var windowSum = 0

    for (i in arr.indices) {
        windowSum += arr[i]

        if (i >= k - 1) {
            maxSum = maxOf(maxSum, windowSum)
            windowSum -= arr[i - k + 1]
        }
    }

    return maxSum
}

```
### Variable Size Sliding Window

```kotlin
fun minLengthSubarrayWithSum(nums: IntArray, target: Int): Int {
    var left = 0
    var sum = 0
    var minLength = Int.MAX_VALUE

    for (right in nums.indices) {
        sum += nums[right]

        while (sum >= target) {
            minLength = minOf(minLength, right - left + 1)
            sum -= nums[left++]
        }
    }

    return if (minLength == Int.MAX_VALUE) 0 else minLength
}

```

###  How to think Sliding window

For each problem:

- Can I brute-force it in O(n²)? 
- Can I maintain a window as I iterate from left to right? 
- When do I expand or shrink the window? 
- What condition tells me when the window is valid/invalid?


