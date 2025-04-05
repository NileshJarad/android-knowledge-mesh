# Series


## 1. Arithmetic Series

Simple loops, incremental algorithms

### Sequence:

1, 2, 3, 4, 5, 6, …

### Formulas:

1. n-th term of Natural Numbers:
   Tₙ = n

2. Sum of first n Natural Numbers:
   Sₙ = n × (n + 1) ÷ 2



------------


## 2. Geometric Progression (GP)

A **Geometric Progression (GP)** is a sequence where each term after the first is found by multiplying the previous term by a fixed, non-zero number called the **common ratio** \( r \).



###  General Form

a, ar, ar², ar³, …


- **a**: First term
- **r**: Common ratio
- Each term: \( a \cdot r^n \), where \( n \) starts from 0


###  Examples

1. **2, 4, 8, 16, 32, ...**
    -  a = 2 ,  r = 2 

2. **81, 27, 9, 3, 1, ...**
    - a = 81 ,  r =1/3

 
### Formulas:

1. n-th term of GP:
   Tₙ = a × rⁿ⁻¹

2. Sum of first n terms:

If r ≠ 1:  
Sₙ = a × (rⁿ - 1) ÷ (r - 1)

If r = 1:  
Sₙ = a × n


----


## 3.Harmonic Series

The **Harmonic Series** is a mathematical series that appears in the analysis of many efficient algorithms — especially in **amortized analysis** and **recursive algorithms** with logarithmic behavior.


### Sequence:

1 + ½ + ⅓ + ¼ + ... + 1⁄n

This is the sum of the reciprocals of the first `n` natural numbers.


### Formula:

**Hₙ = 1 + 1⁄2 + 1⁄3 + ... + 1⁄n**

There is no closed-form formula, but it can be **approximated** as:

Hₙ ≈ ln(n) + γ


## 4.Logarithmic Series

The **Logarithmic Series** appears frequently in algorithm analysis, especially in recursive and divide-and-conquer techniques, heaps, trees, and more.


### Sequence:

log(1) + log(2) + log(3) + ... + log(n)

Here, `log` typically refers to the base-2 logarithm (`log₂`) or base-e (`ln`) depending on context.



### Sum Approximation:

There’s no simple closed-form, but the sum can be **approximated** as:

∑ₖ₌₁ⁿ log(k) ≈ log(n!)



------

## 5.Exponential Series

The **Exponential Series** appears in brute-force recursive algorithms, especially those that explore all possible combinations, subsets, or permutations.



### Sequence:

2⁰, 2¹, 2², 2³, ..., 2ⁿ

Each term doubles the previous one.

---

### Sum Formula:

**Sum of the first n terms:**

If the sequence is:
2⁰ + 2¹ + 2² + ... + 2ⁿ⁻¹

Then the sum is:

Sₙ = 2ⁿ - 1


## 6.Factorial Series

The **Factorial Series** appears in problems involving **permutations**, **combinations**, **backtracking**, and **brute-force enumeration**.

---

### Sequence:

1!, 2!, 3!, 4!, ..., n!

Which expands to:

1, 2, 6, 24, 120, 720, ...

Where:

n! = n × (n - 1) × (n - 2) × ... × 1

----

## 📚 Summary Table

| Series      | Formula                      | Time Complexity |
|-------------|------------------------------|-----------------|
| Arithmetic  | Sₙ = n × (n + 1) ÷ 2         | O(n)            |
| Geometric   | Sₙ = a × (rⁿ - 1) ÷ (r - 1)  | O(log n), O(n)  |
| Harmonic    | Hₙ ≈ ln(n) + γ               | O(log n)        |
| Logarithmic | ∑ log(k) ≈ log(n!) ≈ n log n | O(n log n)      |
| Exponential | 2ⁿ                           | O(2ⁿ)           |
| Factorial   | n!                           | O(n!)           |

---
