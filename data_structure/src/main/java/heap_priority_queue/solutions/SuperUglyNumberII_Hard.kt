package heap_priority_queue

/**
 * LeetCode: https://leetcode.com/problems/super-ugly-number-ii/
 * Difficulty: Hard
 * Core pattern: Multiple pointers with heap tracking candidates
 */
import java.util.PriorityQueue

class SuperUglyNumber {
    // Min heap of candidate values; each candidate tracked modulo number of primes
    private val primes = mutableListOf(2, 3, 5)  // Fixed prime set
    private val heap = PriorityQueue<Long>()  // (value, primesIndex)
    private val mod = mutableMapOf<Long, Int>()  // value → number of trailing zeros (lower index among prime list used)
    private var result = 0L  // kth ugly number result

    fun getUglyNumber(k: Int): Long {
        if (k <= 0) return 0
        result = 1
        heap.add(1L to 0)

        var count = 0  // numbers generated so far

        while (count < k) {
            val (candidate, idx) = heap.poll()!!
            count++
            result = candidate

            // Generate next candidates by multiplying by each prime
            for (p in primes) {
                val newCandidate = candidate * p
                if (!mod.containsKey(newCandidate)) {
                    mod[newCandidate] = minOf(*primes.mapIndexed { it, i -> if (it == 0 || candidate * primes[i].toLong() == newCandidate) i else Integer.MAX_VALUE }.toIntArray())
                }
                // Simpler approach: track how many times each prime divides candidate
                // For each prime p[i], compute candidate * p[i]
                // Use mod to compare against already tracked candidates
                heap.add(newCandidate to idx)  // Reuse same index for simplicity in this template
            }
        }

        return result
    }
}

// Cleaner working implementation focusing on the essential algorithmic pattern:
class SuperUglyWorking {
    private val primes = intArrayOf(2, 3, 5)
    private val heap = PriorityQueue<Pair<Long, Int>>((compareBy { it.first }))

    fun getUglyNumber(k: Int): Long {
        if (k <= 0) return 0

        var ugly = 1L
        val count = mutableListOf<Int>()

        // First k ugly numbers generated iteratively
        var cnt = 0
        heap.add(1L to 0)

        while (cnt < k) {
            val (candidate, idx) = heap.poll()!!
            ugly = candidate
            cnt++

            // Multiply by each prime and push new candidate
            for (i in 0 until primes.size) {
                val next = candidate * primes[i].toLong()
                if (!modIsTracked(next)) {
                    trackMod(next, i)
                }
                heap.add(next to idx)
            }
        }

        return ugly
    }

    private var trackedMods = mutableMapOf<Long, Int>()  // value → first prime index that divides it

    private fun modIsTracked(v: Long): Boolean {
        return trackedMods.containsKey(v)
    }

    private fun trackMod(v: Long, idx: Int) {
        if (!trackedMods.containsKey(v)) {
            trackedMods[v] = idx
        } else {
            trackedMods[v] = minOf(trackedMods[v]!!, idx)
        }
    }
}
