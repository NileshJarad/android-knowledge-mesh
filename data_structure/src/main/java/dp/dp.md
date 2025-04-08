# Dynamic Programming

Dynamic Programming is an optimization technique used for problems with overlapping subproblems and optimal
substructure.

## Key Ideas:

- Overlapping Subproblems: The same subproblem is solved multiple times.
- Optimal Substructure: The optimal solution of the problem can be formed from the optimal solutions of its subproblems.
- **Memoization (Top-Down)**: Recursively solve subproblems, storing the result.
- **Tabulation (Bottom-Up)**: Iteratively solve all subproblems and store them.

## Common DP Patterns

- Simple Recursion + Memoization - Fib
- DP with choices (Pick / Not Pick) - 0/1 Knapsack
- Item can be picked infinite times - Unbounded Knapsack
- Subsequence with two pointers - Longest Common Subsequence (LCS)
- Min coins to form a target sum - Coin Change (Min Number of Coins)

Problems:

1. [Nth Fibonacci number with Memoization and Tabulation](FibonacciDp.kt)
2. [Frog Jump](FrogJump.kt)