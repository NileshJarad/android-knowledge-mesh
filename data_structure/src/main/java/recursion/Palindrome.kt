package recursion


fun main() {

//    val str = "MOM"
    val str = "Mkl"
    print("Check string is palindrome ${palindrome(str, 0, str.length - 1)}")
}

fun palindrome(str: String, left: Int, right: Int): Boolean {

    if (left >= right) return true

    if (str[left] != str[right]) return false

    return palindrome(str, left + 1, right - 1)
}