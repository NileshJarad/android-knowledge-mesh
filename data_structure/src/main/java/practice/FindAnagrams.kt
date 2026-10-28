package practice

class FindAnagrams {

    fun find(s: String, p: String): List<Int> {
        val result = mutableListOf<Int>()
        val pFreq = IntArray(26)
        val runningFreq = IntArray(26)

        for (char in p) {
            pFreq[char - 'a']++
        }

        val window = p.length

        for (i in s.indices) {
            runningFreq[s[i] - 'a']++

            if (i >= window) {
                runningFreq[s[i - window] - 'a']--
            }

            if (i >= window - 1) {
                if (pFreq.contentEquals(runningFreq)) {
                    result.add(i - window + 1)
                }
            }
        }


        return result
    }
}

fun main() {
    val s = "cbaebabacd"
    val p = "abc"
    println("Result for anagram is ${FindAnagrams().find(s, p)}")
}