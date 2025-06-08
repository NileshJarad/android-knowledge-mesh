# Functions 

## Higher-Order Function
A higher-order function is a function that takes another function as a parameter or returns a function. This allows functional programming concepts like passing behaviors, callbacks, and transformations.

**Basic Example of Higher-Order Function**

```kotlin
fun operateOnNumbers(a: Int, b: Int, operation: (Int, Int) -> Int): Int {
    return operation(a, b)  // Calls the passed function
}

fun main() {
    val sum = operateOnNumbers(5, 3) { x, y -> x + y }
    val multiply = operateOnNumbers(5, 3) { x, y -> x * y }

    println("Sum: $sum")       // Output: Sum: 8
    println("Multiply: $multiply")  // Output: Multiply: 15
}

```

**Returning a Function from a Higher-Order Function**

```kotlin
fun getOperation(type: String): (Int, Int) -> Int {
    return when (type) {
        "add" -> { a, b -> a + b }
        "multiply" -> { a, b -> a * b }
        else -> { _, _ -> 0 }
    }
}

fun main() {
    val operation = getOperation("add")
    println(operation(10, 5)) // Output: 15
}

```

**Why Use Higher-Order Functions?**
- Code Reusability → Pass behavior instead of duplicating logic. 
- Cleaner Code → Reduces the need for repetitive conditional statements. 
- Flexible & Extensible → Allows dynamic function selection.


## Lambda function
A lambda function (or lambda expression) in Kotlin is a concise way to define anonymous functions. It is a function that does not have a name and can be passed around as an expression.
Lambdas are commonly used in higher-order functions, collections operations, and functional programming.

**Syntax of Lambda**

```kotlin
val lambdaName: (ParameterType) -> ReturnType = { parameterName -> functionBody }

```

**Example**

```kotlin
val sum: (Int, Int) -> Int = { a, b -> a + b }

fun main() {
    println(sum(10, 5))  // Output: 15
}

```

## inline, noinline, reified and crossinline
### inline
- When a function is marked inline, the compiler copies its body wherever it's called instead of creating a function call.
- Reduces function call overhead, especially in lambda-heavy code.
- Helps avoid creating unnecessary objects (like lambda instances).

**Example: Without inline (Object Creation)**
```kotlin

fun nonInlineFunction(action: () -> Unit) {
    println("Before action")
    action() // This creates a new function object
    println("After action")
}

fun main() {
    nonInlineFunction { println("Executing action") }
}

```
Here, Kotlin creates an extra function object for action, which can slow performance

**Example: With inline (No Object Creation)**
```kotlin
inline fun inlineFunction(action: () -> Unit) {
    println("Before action")
    action() // The lambda is directly placed here (no function object)
    println("After action")
}

fun main() {
    inlineFunction { println("Executing action") }
}

```

Advantages of inline: 
- No function object is created → Better performance. 
- Function calls are replaced with direct code execution → Less overhead.

### noinline (Preventing Inlining)
- When using inline, all lambda parameters are automatically inlined.
- If you don’t want some lambdas to be inlined, use noinline.

**Example: inline with noinline**

```kotlin
inline fun testFunction(inlinedLambda: () -> Unit, noinline normalLambda: () -> Unit) {
    inlinedLambda()  // This lambda will be inlined
    normalLambda()   // This lambda will NOT be inlined
}

```

Why use noinline? 
- If a lambda is passed around (e.g., stored in a variable), it CANNOT be inlined.
- If a lambda is used multiple times inside the function, inlining can increase code size.

### reified (Retaining Type Information)

- Normally, type parameters are erased at runtime due to type erasure.
- reified allows accessing generic types at runtime, but it can only be used inside inline functions`.

**Example: Problem Without reified**

```kotlin
fun <T> printType(clazz: Class<T>) {
    println(clazz.simpleName)
}

fun main() {
    printType(String::class.java)  // ✅ Works
    // printType<List<String>>()  // ❌ Error: Type information is erased
}
```

Generic types like List<String> lose type information at runtime.

**Solution: reified with inline**

```kotlin
inline fun <reified T> printType() {
    println(T::class.java.simpleName)  // Now we can access the type at runtime
}

fun main() {
    printType<String>()  // ✅ Output: String
    printType<List<String>>()  // ✅ Output: ArrayList (No type erasure issue)
}

```

Why use reified? 
- Allows working with generic types at runtime.
- Eliminates the need to pass Class<T> manually.


### crossinline ( prevent non-local returns)
The crossinline keyword ensures that the lambda function cannot contain non-local returns. Non-local returns are when you try to return from an outer function within a lambda. By marking a lambda as crossinline, you prevent non-local returns inside the lambda body.

**When to use crossinline:**
To prevent a lambda from containing non-local returns when you still want to inline the function.

**What is a Non-Local Return?**
A non-local return happens when you return from a lambda, and that return is applied to an outer function, not just the lambda itself.

----
## Extension Functions
<p>Extension functions in Kotlin allow you to add new functionality to existing classes without modifying their source code. They are a powerful feature that lets you extend a class with new methods, which can make your code more readable and concise.</p>

<p>Even though Kotlin is a statically-typed language, extension functions allow you to extend classes as if you are adding new methods to them. However, these methods are not actually added to the class—they are just syntactic sugar to call them in a way that looks like they're part of the class.</p>

```kotlin
fun ClassName.extensionMethodName() {
    // Function body
}
```

**Example :**

```kotlin
data class Person(val firstName: String, val lastName: String)

// Extension function to print full name
fun Person.printFullName() {
    println("$firstName $lastName")
}

fun main() {
    val person = Person("John", "Doe")
    person.printFullName()  // Output: John Doe
}

```

- **Receiver type** is the class to which the function is being added (it’s an implicit parameter of the function). 
- The function behaves as if it is defined in the class itself, even though it's just syntactic sugar.

## Extension Properties

- In addition to functions, you can also define extension properties.


**Example : Adding an Extension Property to a Person Class**
```kotlin
data class Person(val firstName: String, val lastName: String)

val Person.fullName: String
    get() = "$firstName $lastName"

fun main() {
    val person = Person("John", "Doe")
    println(person.fullName)  // Output: John Doe
}

```
**Explanation:**
- Here, we define an extension property fullName for the Person class.
- This allows us to access the fullName property as if it were part of the Person class, even though it’s defined outside the class.

**Limitations of Extension Functions**
- No real inheritance: Extension functions do not actually modify the class and are not part of the class. They are just syntactic sugar. 
- Cannot override methods: You cannot override existing methods of a class with extension functions. They cannot interact with private or protected methods or fields of the class.

**Note:**
- Extension functions in Kotlin are resolved statically, not dynamically.
- Member wins over extension function if we define extension function same as member function

--- 

## Infix Function
In Kotlin, an infix function is a special kind of function that allows you to call it using a more natural, readable syntax without parentheses. Infix functions can only be called on instances of classes and are usually used for operator-like behavior or when you want to create expressive, readable code.

- To define an infix function in Kotlin, you need to use the infix keyword. The function must meet the following conditions:
- It must be a member function or an extension function.
- It must take exactly one parameter.

    ```kotlin
    
    infix fun ClassName.functionName(parameter: Type): ReturnType {
        // function body
    }
    
    ```

**Example Simple Infix Function**

```kotlin
infix fun Int.isDivisibleBy(divisor: Int): Boolean {
    return this % divisor == 0
}

fun main() {
    val number = 10
    println(number isDivisibleBy 2)  // Output: true
    println(number isDivisibleBy 3)  // Output: false
}

```