# Android Interview Knowledge Base

> A comprehensive reference covering Android development, Kotlin, Data Structures & Algorithms, Design Patterns, SOLID Principles, OOP, System Design, and more. Designed for interview preparation and RAG-based retrieval.

---

## Table of Contents

1. [Kotlin Fundamentals](#kotlin-fundamentals)
2. [Android Components](#android-components)
3. [Jetpack Compose](#jetpack-compose)
4. [Kotlin Coroutines & Flow](#kotlin-coroutines--flow)
5. [Threading](#threading)
6. [Dependency Injection](#dependency-injection)
7. [Networking](#networking)
8. [Android Security](#android-security)
9. [OOP Concepts](#oop-concepts)
10. [SOLID Principles](#solid-principles)
11. [Design Patterns](#design-patterns)
12. [Object-Oriented Design & UML](#object-oriented-design--uml)
13. [Data Structures & Algorithms](#data-structures--algorithms)
14. [System Design (LLD)](#system-design-lld)
15. [Git](#git)
16. [Behavioral Interview](#behavioral-interview)

---

# 1. Kotlin Fundamentals


## 1.1 Kotlin Basics

# Basics

## Variables

- `val`
    - is used to declare read-only (immutable) variables.
    - Once initialized, the value of a `val` cannot be changed.
- `var`
    - is used to declare mutable variables.
    - The value of a `var` can be changed after it is initialized.
- `const val`:
    - This keyword is used to declare compile-time constants.
    - Constants declared with` const val` must be top-level or
      member of an object declaration or a companion object. (not inside a class or function)
    - These constants are replaced by their actual values at
      compile time wherever they are referenced.
    - Can only hold primitive types or String.

```kotlin
      val x: Int = 5
x = 10 // This will cause a compilation error because x is immutable

var y: Int = 5
y = 10 // This is valid because y is mutable
```

- `lateinit`
    - is used for properties that are initialized later, after the object creation.
    - It is mainly used with non - null properties of classes that cannot be initialized in constructors .
    - Only allowed for non-primitive types (String, List, CustomClass, etc.).
- `lazy`
    - is a function that takes a lambda and returns an instance of Lazy<T>, which can be used to access the lazily
      computed value .

> Remember, lateinit should be used with caution, as accessing an uninitialized lateinit property will throw an
> exception.lazy is thread-safe by default and will compute the value only once, on the first access, making it
> suitable for lazy initialization .

> lateinit and lazy are features used for lazy initialization in Kotlin :

```kotlin
class MyClass {
    lateinit var lateInitVar: String

    val lazyVar: String by lazy {
        println("Lazy block executed for the First time") // this will only be executed once in first intialisation
        "Lazy initialized value"
    }
}

fun main() {
    val obj = MyClass()

    // Accessing lateinit variable before initialization will throw an exception
    // obj.lateInitVar // This will throw UninitializedPropertyAccessException

    obj.lateInitVar = "Initialized late"
    println(obj.lateInitVar) // Prints: Initialized late

    // Accessing lazy variable will initialize it on first access
    println(obj.lazyVar) // Prints: Lazy initialized value
}

```

Checking whether a lateinit var is initialized

  ```kotlin
  if (foo::bar.isInitialized) {
    println(foo.bar)
}
  ```

---------

| Keyword        | Mutable? | Init Required?    | When?        | Null Support         | Use for                            |
|----------------|----------|-------------------|--------------|----------------------|------------------------------------|
| `const val`    | ❌ No     | ✅ Yes             | Compile time | ❌ No                 | Constants                          |
| `val`          | ❌ No     | ✅ Yes             | Runtime      | ✅ Yes/No             | Read-only variables                |
| `var`          | ✅ Yes    | ✅ Yes             | Runtime      | ✅ Yes/No             | Mutable variables                  |
| `lateinit var` | ✅ Yes    | ❌ No (init later) | Runtime      | ❌ No (non-null only) | Dependency injection, delayed init |

--------

## object and companion object

Both companion object and object are used to define singleton-like behavior, but they serve different purposes

### object (Standalone Singleton)

- Declares a singleton in Kotlin (only one instance exists).
- Automatically instantiated when first accessed.
- Useful for utility functions, managers, or single-instance objects.
  ```kotlin
  object DatabaseManager {
      val dbName = "MyDatabase"
  
      fun connect() {
          println("Connected to $dbName")
      }
  }
  
  fun main() {
      DatabaseManager.connect()  // ✅ Singleton access
  }

  ```

### companion object (Static-like Members)

- Used inside a class to create static-like behavior.
- Allows defining properties & methods shared across all instances of a class.
- Functions as a companion to the class, allowing access without an instance.

  ```kotlin
  class User(val name: String) {
      companion object {
          val defaultUser = User("Guest")
  
          fun printDefaultUser() {
              println("Default user is: ${defaultUser.name}")
          }
      }
  }
  
  fun main() {
      User.printDefaultUser()  // ✅ Accessing without an instance
  }
  
  ```

----

## Range Operator

```kotlin
1..5  //1,2,3,4,5
```

```kotlin
1.rangeTo(5)  // 1,2,3,4,5
```

```kotlin
5 downTo 1  // 5,4,3,2,1
```

```kotlin
1..5 step 2 // 1,3,5
```

```kotlin
5 downTo 1 step 2 // 5,3,1
```

```kotlin
1 until 4 // 1,2,3 
```

```kotlin
1..<4 // 1,2,3 
```

--- 

## Ways to initialise Int Array

- **Using IntArray**
    ```kotlin
    val arr = IntArray(5) // [0, 0, 0, 0, 0]
    ```
- **Using IntArray lambda to set values**
    ```kotlin
    //Each index it is passed to the lambda.
    val arr = IntArray(5) { it * 2 } // [0, 2, 4, 6, 8]
    ```
- **Using .fill() after creating an array**

    ```kotlin
    val arr = IntArray(5)
    arr.fill(7) // arr becomes [7, 7, 7, 7, 7]
    ```

- **Using arrayOf() and converting to IntArray**
    ```kotlin
    // Using Array<Int> and converting to IntArray
    
    val arr = arrayOf(1, 2, 3).toIntArray() // [1, 2, 3]
    ```
- **Using intArrayOf() directly**
    ```kotlin
    val arr = intArrayOf(10, 20, 30) // [10, 20, 30]
    ```

## Elvis Operator (?:) and Safe Call Operator (?.)

### Safe Call Operator (?.)

- The safe call operator (?.) is used to invoke methods or access properties on nullable types.
- If the object is not null, the operation proceeds. If it's null, the expression returns null instead of throwing a
  NullPointerException.

  ```kotlin
  class Person(val name: String, val age: Int?)
  
  fun main() {
      val person: Person? = Person("John", null)
      
      // Safe call operator
      val age = person?.age
      println(age)  // Output: null (because person?.age is safely accessed)
      
      val nameLength = person?.name?.length
      println(nameLength)  // Output: 4 (length of "John" since `name` is not null)
  }
  
  ```

### Elvis Operator (?:)

- The Elvis operator (?:) is used in conjunction with nullable types to provide a default value when the expression is
  null.
- It returns the expression's result if it is non-null; otherwise, it returns a specified default value.
  ```kotlin
  fun getLength(str: String?): Int {
      // If str is null, return 0
      return str?.length ?: 0
  }
  
  fun main() {
      val name: String? = null
      println(getLength(name))  // Output: 0 (because name is null)
      
      val anotherName: String? = "Kotlin"
      println(getLength(anotherName))  // Output: 6 (length of "Kotlin")
  }
  
  ```

## === and ==

1. **== (Structural Equality / Equals Comparison)**
    - Purpose: The == operator checks for structural equality, which means it compares the values of two objects to
      determine if they are equal.
    - How it works: When you use ==, Kotlin internally calls the equals() function on the objects to check for equality.
    - This is the operator you will most commonly use to check if two objects have the same data or value.

       ```kotlin
       data class Person(val name: String, val age: Int)
       
       fun main() {
           val person1 = Person("Alice", 30)
           val person2 = Person("Alice", 30)
           
           println(person1 == person2)  // Output: true (structurally equal)
       }
       ```

2. **=== (Referential Equality / Reference Comparison)**
    - Purpose: The === operator checks for referential equality, which means it compares whether two variables point to
      the same object in memory (i.e., if they are the same reference)
    - How it works: When you use ===, Kotlin checks whether the two references refer to the exact same object in memory.

      ```kotlin
       fun main() {
           val person1 = Person("Alice", 30)
           val person2 = Person("Alice", 30)
           
           // Checking if both variables refer to the same object in memory
           println(person1 === person2)  // Output: false (different objects in memory)
       } 
    ```

## Smart Cast and Safe Cast in Kotlin

### Smart Cast

Smart cast is a feature in Kotlin that allows the compiler to automatically cast an object to a specific type when it is
safe to do so. When the type is checked in a conditional block (like if or when), Kotlin automatically casts the object
to the target type after the check. This eliminates the need for explicit casting in many cases.

**How Smart Cast Works:**

- Type Checking: If you check the type of an object using is (e.g., if (x is String)), Kotlin automatically casts it to
  that type within the scope of that check.
- No Explicit Casting: After the type check, you can directly use the object as that type without needing to explicitly
  cast it with (x as String).

    ```kotlin
    fun printLength(obj: Any) {
        if (obj is String) {
            // Smart cast to String, no need for explicit casting
            println("Length: ${obj.length}")
        } else {
            println("Not a string!")
        }
    }
    
    fun main() {
        printLength("Hello, Kotlin!")  // Output: Length: 14
        printLength(42)                // Output: Not a string!
    }
    
    ```

### Safe Cast (as?)

A safe cast allows you to try to cast an object to a specific type, but instead of throwing an exception if the cast
fails, it returns null. This ensures that your program doesn't crash due to invalid casts, making the casting operation
safe.

```kotlin
val result = obj as? T
```

**Example :**

```kotlin
fun printStringLength(obj: Any) {
    val str = obj as? String
    if (str != null) {
        println("Length of string: ${str.length}")
    } else {
        println("Not a string!")
    }
}

fun main() {
    printStringLength("Kotlin")  // Output: Length of string: 6
    printStringLength(42)        // Output: Not a string!
}

```


## 1.2 Kotlin Classes

# Classes 


## Why are Kotlin Classes final by Default?
In Kotlin, classes are final by default, meaning they cannot be inherited unless explicitly marked as open. This design decision is intentional and serves multiple purposes, primarily related to immutability, safety, and predictability.

1. Encourages Immutability and Stability 
   - Immutability is a key design principle in Kotlin. Making classes final by default encourages developers to write code that is less likely to be accidentally altered or extended in ways that could introduce bugs. 
   - Immutability leads to predictability and safety. If a class is final, it means that its implementation cannot be extended or overridden, so you can be sure that the class’s behavior won’t change unexpectedly in subclasses.
2. Prevents Inheritance and Unintended Subclassing 
   - In many cases, allowing inheritance or subclassing may not be necessary and can lead to misuse or incorrect overrides. 
   - Final classes ensure that no one can accidentally extend them or override their methods unless explicitly intended. This helps in avoiding incorrect behavior that could be introduced by subclassing. 
   - By making classes final by default, Kotlin ensures that developers think carefully before deciding to expose a class for inheritance.
3. Encourages Composition Over Inheritance
  - Kotlin encourages the use of composition (i.e., "has-a" relationships) over inheritance (i.e., "is-a" relationships). Composition tends to lead to more flexible and maintainable code. 
  - By making classes final by default, Kotlin forces developers to consider alternatives like composition or delegation instead of inheritance.
- Improved Performance
  - Final classes can be optimized by the compiler and the JVM. If a class is final, the compiler knows that it doesn't need to account for potential subclassing or overridden methods, so it can generate more efficient code. 
  - This can result in better performance, as the JVM can optimize method dispatch more aggressively, knowing that the method implementations won’t change at runtime.
- Clearer and Safer Design
  - By making classes final by default, Kotlin makes the design more explicit and clearer. 
    - If a class is meant to be extended, you can explicitly declare it as open, making the intention clear. 
    - This makes the design of your code more intentional and explicit. Developers can easily understand which classes are meant for extension and which are not.
- Encourages a More Predictable Object-Oriented Design
  - In classic object-oriented programming, inheritance is a powerful but dangerous tool that can lead to a fragile base class problem. This happens when a superclass is modified and those changes inadvertently break child classes. 
  - By making classes final by default, Kotlin helps to prevent such issues, promoting a more predictable and maintainable design.

----

## Backing field

- Kotlin's properties have implicit support for getters and setters. When you define a custom getter or setter and want to access the value within the setter or getter, you need to use the backing field, which is referenced using the field keyword
- The **field** identifier can only be used in the accessors of the property.
- A backing field will be generated for a property if usage of the `field` keyword is required.
- Use a backing field when you define a custom getter/setter AND need to store a value internally for that property.

```kotlin
var counter = 0 // the initializer assigns the backing field directly
    set(value) {
        if (value >= 0)
            field = value
            // counter = value // ERROR StackOverflow: Using actual name 'counter' would make setter recursive
    }

var name : String = "" // no backing field will be generated

var isCountSet: Boolean // no backing field will be generated
  get() = counter != 0

```

**Why is Backing Field Needed?**

Without a backing field, if you try to access the property inside its own getter/setter, you will end up with infinite recursion.


**_Wrong way (causes stack overflow):_**

```kotlin
var name: String = "Guest"
    get() = name  // ❌ This recursively calls the getter itself!
```

**_Correct way (uses backing field):_**
```kotlin
var name: String = "Guest"
    get() = field

```

----

## Data class
In Kotlin, data classes are special classes designed to hold and manage data. They automatically generate useful methods like equals(), hashCode(), toString(), and others based on the properties you define, which makes them ideal for simple value objects.
```kotlin
data class Person(val name: String, val age: Int)
```
**Key Characteristics of Data Classes:**
- Primary Constructor with Properties: Data classes must have at least one parameter in the primary constructor, and these parameters must be used as properties.
- Automatically Generated Methods:
  - `toString()`: Provides a string representation of the class. 
  - `equals()`: Compares the data of two instances for equality. 
  - `hashCode()`: Generates a hash code based on the properties. 
  - `copy()`: Allows creating a copy of an object with some modified properties.
- Component Functions: For each property, a component function (e.g., component1(), component2()) is automatically generated to allow destructuring.


**Differences Between Data Classes and Regular Classes**

1. Automatic Method Generation
   - **Data Classes**: Kotlin automatically generates the following methods for you:
     - `equals()`
     - `hashCode()`
     - `toString()`
     - `copy()`
    - **Destructuring functions** (e.g., component1(), component2())
   - **Regular Classes**: You need to manually implement these methods if needed. This means you have to write additional code for comparison, object copying, string representation, etc.

2. Purpose
   - **Data Classes**: Primarily used to hold data. They are intended to represent objects with properties but no additional behavior.
   - **Regular Classes**: Can be used for more general-purpose classes, which may include behavior (methods) and state (properties).

3. Inheritance
   - **Data Classes**: By default, cannot be inherited (they are final by default). You cannot subclass a data class unless it is explicitly declared as open (though this is rarely done).

   - **Regular Classes**: Regular classes can be inherited and can have subclasses unless they are marked as final.

4. Immutability
   - **Data Classes**: Data classes are immutable by default if their properties are declared as val (read-only). However, you can define them with var properties, making the properties mutable.
   - **Regular Classes**: Regular classes have no such restrictions. Properties can be val or var, and it is up to the developer to decide if they want the properties to be mutable or immutable.

5. Destructuring Declaration
   - **Data Classes**: You can use a destructuring declaration with data classes. The compiler automatically provides componentN() functions for each property of the class.
   - **Regular Classes**: Destructuring is not available unless you define the componentN() functions yourself.


## Sealed Class (Restricted Hierarchy)
- Restricts subclassing → All subclasses must be defined in the same file.
- Used for representing finite states (like enum but with multiple types).
- Cannot be instantiated directly
```kotlin
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: String) : UiState()
    data class Error(val message: String) : UiState()
}

fun handleState(state: UiState) {
    when (state) {
        is UiState.Loading -> println("Loading...")
        is UiState.Success -> println("Success: ${state.data}")
        is UiState.Error -> println("Error: ${state.message}")
    }
}

```
**Key Points:**
- Sealed class restricts inheritance to only the same file.
- Useful for modeling states in when expressions (ensures exhaustiveness).
- Cannot be instantiated directly → Only its subclasses can be used.
- Sealed class can extend another sealed class if and only if it's present in same file, as compiler should know it at compile time

## Abstract Class (General Inheritance)
- Allows subclassing from anywhere (no file restriction).
- Can contain both abstract and concrete methods.
- Cannot be instantiated directly.

```kotlin
abstract class Animal(val name: String) {
    abstract fun makeSound()

    fun describe() = "Animal: $name"
}

class Dog(name: String) : Animal(name) {
    override fun makeSound() = println("Bark!")
}

fun main() {
    val myDog = Dog("Buddy")
    myDog.makeSound() // Output: Bark!
    println(myDog.describe()) // Output: Animal: Buddy
}

```
**Key Points:**
- Abstract class allows inheritance across files.
- Can have both abstract (makeSound) and non-abstract (describe) methods.
- Used when creating a base class for multiple related classes.

----

## Enum class
An enum class in Kotlin (and Android) is a special class used to define a set of constants. These constants are often related and known at compile time.

Think of it like a list of named values that represent a finite set of options, like days of the week, directions, states, etc.


```kotlin
enum class Direction {
    NORTH, SOUTH, EAST, WEST
}

Usage:

val dir: Direction = Direction.NORTH

```

**Enum with Properties and Methods**

````kotlin
enum class Status(val code: Int) {
    SUCCESS(200),
    ERROR(500),
    LOADING(102);

    fun isError(): Boolean = this == ERROR
}


val status = Status.ERROR  // or Status.values()[1]
println(status.code)        // Output: 500
println(status.isError())   // Output: true


````

**Enum with implement**

```kotlin
interface StatusCode {
    fun code(): Int
}

enum class Status : StatusCode {
    SUCCESS {
        override fun code() = 200
    },
    ERROR {
        override fun code() = 500
    }
}

```


**Note:**
* Enum classes in Kotlin cannot be inherited
* You can have an enum class implement an interface
* Cannot create new instances at runtime
* Fixed set of constants
* Implicitly extends Enum class, final
* Private only constructor 

----

## Value class

A value class is a Kotlin class that wraps a single property but avoids creating an object at runtime (under certain conditions) — meaning it’s more memory-efficient.

Think of it as a way to give semantic meaning to a simple value.

```kotlin
@JvmInline
value class UserId(val id: String) {
    init {
        require(id.isNotEmpty()) { "Invalid user Id" }
    }

    fun print() = println("Print user ID")
}


val user = UserId("abc123")
println(user.id)

```

**Why Use value class?**

Without value class:

```kotlin
fun getUser(id: String) { ... } // What is this String?
```

With value class:

```kotlin
fun getUser(id: UserId) { ... } // Much clearer!
```


### Kotlin Classes - Code Examples

```kotlin
package kotlin_fundamentals


fun main() {
    print("sol =${Solution().longestCommonPrefix(arrayOf("flower","flow","flight"))}")
}


class KotlinClass {

    var name: String = ""
        get() = field
        set(value) {
            field = value +""
        }

    var isEmpty: Boolean = false
        get() = name.isEmpty()
        set(value){
            field = value
        }


}


@JvmInline
value class Person(private val fullName: String) {
    init {
        require(fullName.isNotEmpty()) {
            "Full name can not be empty"
        }
    }

    constructor(firstName: String, lastName: String) : this("$firstName $lastName")

    val length: Int
        get() = fullName.length

    fun greet(setVal: Int) {
        println("Hello $fullName")
    }
}


internal class Solution {
    fun longestCommonPrefix(strs: Array<String>): String {
        if (strs.size == 0) return ""
        var prefix = strs[0]

        strs.forEach{ data ->
            while(data.indexOf(prefix) != 0){
                prefix = prefix.substring(0, prefix.length - 1)
                println("prefix $prefix")
            }
        }
        return prefix
    }
}```


## 1.3 Kotlin Functions

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

## 1.4 Scope Functions

# Scope functions 


>Scoped functions in Kotlin are powerful constructs that help simplify and streamline code by providing a concise and readable way to operate on objects within a limited scope. These functions include let, run, with, apply, and also. Let's explore each one:

## Let
- **Purpose**: Executes a given block of code with a non-null object as the receiver, allowing safe navigation and transformation of the object.
- **Returns** : Result
- **Context Object** : it
- **Context Rename** : Yes
- **Null check** : Yes


Syntax:
```kotlin
object?.let { /* code block */ }
```
Usage:
```kotlin
val result = someNullableObject?.let {
    // Perform operations on non-null object
    it.doSomething()
    it.calculate()
} ?: defaultValue

```

## Run
- **Purpose**: Executes a given block of code within the context of an object and returns the result of the block.
- **Returns** : Result
- **Context Object** : this
- **Context Rename** : No
- **Null check** : Yes
  
Syntax

```kotlin
object.run { /* code block */ }
```

Usage:
```kotlin
val result = someObject.run {
    // Access properties and methods of the object directly
    doSomething()
    calculate()
}
```

## with
- **Purpose**: Similar to run, but used without the context object. It's typically used to clean up code by removing redundant references to the object.
- **Returns** : Result
- **Context Object** : this
- **Context Rename** : No
- **Null check** : No

Syntax
```kotlin
with(object) { /* code block */ }
```

Usage:
```kotlin
val result = with(someObject) {
    // Access properties and methods of the object directly
    doSomething()
    calculate()
}

```

## apply
- **Purpose**: Applies the specified function to the object and returns the object itself, commonly used for initializing or configuring objects.
- **Returns** : Object
- **Context Object** : this
- **Context Rename** : No
- **Null check** : Yes

Syntax
```kotlin
object.apply { /* code block */ }
```

Usage:
```kotlin
val someObject = SomeClass().apply {
    // Initialize or configure object properties
    property1 = value1
    property2 = value2
}
```


## also
- **Purpose**: Performs additional actions on an object and returns the object itself, often used for logging or side effects.
- **Returns** : Object
- **Context Object** : it
- **Context Rename** : Yes
- **Null check** : Yes

Syntax
```kotlin
object.also { /* code block */ }
```

Usage:
```kotlin
val someObject = SomeClass().also {
    // Perform additional actions
    log.info("Object created: $it")
    // Other operations
}
```

**Best Practices**
- Use let for null checks and transformations.
- Use apply for modifying objects (builders).
- Use also for side effects like logging.
- Use run when operations should return a value.
- Use with for grouping actions on an object.


| Function | Returns           | Used For                                         | Context Object |
|----------|-------------------|--------------------------------------------------|----------------|
| `let`    | Last expression   | Transforming or using the result                 | `it`           |
| `run`    | Last expression   | Performing operations & returning result         | `this`         |
| `apply`  | The object itself | Configuring an object                            | `this`         |
| `also`   | The object itself | Additional actions (logging, debugging)          | `it`           |
| `with`   | Last expression   | Grouping operations without returning the object | `this`         |




![alt](https://i.ytimg.com/vi/6KL3B4NZauY/maxresdefault.jpg)

---

# 2. Android Components


## 2.1 Activity

# Activity

An android provides the window in which the app draws its UI. This window typically fills the screen but may be smaller than the screen and float on top of other windows. Generally, one android implements one screen in an app

## Activity lifecycle methods:

1. **onCreate()**
   - **_Called When_** - The activity is first created (only once per instance).
   - **_Purpose_** - Initialize the android. Setup Views, restore states

2. **onStart()**
   - **_Called When_**: After onCreate() or when coming back from background.
   - **_Purpose_**: Make the android visible to the user, but not interactive yet.

3. **onResume()**
   - **_Called When_**: After onStart() or when returning from a paused state.
   - **_Purpose_**: The activity is now in the foreground and the user can interact with it.
4. **onPause()**

   - **_Called When_**: Activity is partially obscured, like:
     - A new android is started 
     - A dialog appears
   - **_Purpose_**:
     - Pause animations, music, or video 
     - Save unsaved data (if lightweight)

5. **onStop()**
   - **_Called When_**: Activity is no longer visible (completely hidden).

   - **_Purpose_**:
     - Release resources that are not needed when off-screen
     - Stop heavy processes

6. **onRestart()**

   - **Called When**: The user navigates back to the android from the stopped state (e.g., back button).
   - **Purpose**: Prepare the android to go back into foreground.

7. **onDestroy()**

   - **_Called When_**:
     - The activity is finishing (user presses back or calls finish())
     - The system destroys the android (e.g., configuration change)

   - **_Purpose_**: Cleanup all resources to avoid memory leaks


## Starting a New Activity and Coming back

**Launch  MainActivity**
- MainActivity: onCreate()
- MainActivity: onStart()
- MainActivity: onResume()

**Launch Flow (from MainActivity to SecondActivity):**
- MainActivity: onPause()
- SecondActivity: onCreate()
- SecondActivity: onStart()
- SecondActivity: onResume()
- MainActivity: onStop()

**Press Back (from SecondActivity to MainActivity):**

- SecondActivity: onPause()
- MainActivity: onRestart()
- MainActivity: onStart()
- MainActivity: onResume()
- SecondActivity: onStop()
- SecondActivity: onDestroy()


## Starting a New Activity and user press home button 

- MainActivity: onCreate()
- MainActivity: onStart()
- MainActivity: onResume()
- MainActivity: onPause()
- MainActivity: onStop() - _The activity is not destroyed. It just goes to the background (stopped state), and stays in memory (unless Android kills it due to low memory)._



## Scenario: System Dialog appears on top of your android

- Incoming call screen
- Battery saver alert 
- Permission dialog 
- Airplane mode warning 
- Notification drawer being pulled (only partial impact)

### Depends on whether the dialog is:
- Partially obstructing (e.g., floating permission dialog)
- Fully covering (e.g., system alert that takes full focus)

### Android Activity Lifecycle – System Dialog Summary
| 🧪 System Dialog Type       | 📱 Activity Visibility           | 🔁 Lifecycle Methods Called            | 🧷 Final Activity State |
|-----------------------------|----------------------------------|----------------------------------------|-------------------------|
| Permission Dialog (partial) | Partially visible                | `onPause()`                            | Paused                  |
| Incoming Call Screen        | Not visible (fully covered)      | `onPause()`, `onStop()`                | Stopped                 |
| Battery/Low Power Alert     | Depends (partial/full)           | `onPause()` or `onPause()`, `onStop()` | Paused / Stopped        |
| Notification Drawer Pull    | Still visible (no loss of focus) | *(No lifecycle method called)*         | Resumed                 |
| Airplane Mode Toggle Dialog | Partially visible                | `onPause()`                            | Paused                  |

### When Dialog is Dismissed

| 💬 Previous State | 🚀 Lifecycle Methods Called on Return    |
|-------------------|------------------------------------------|
| Paused            | `onResume()`                             |
| Stopped           | `onRestart()`, `onStart()`, `onResume()` |


---

## Start Activity for Result

**1. Define the launcher in your activity or fragment:**
```kotlin
private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val data: Intent? = result.data
        val resultValue = data?.getStringExtra("key") // Or whatever data you're expecting
        Toast.makeText(this, "Result: $resultValue", Toast.LENGTH_SHORT).show()
    }
}
```

**2. Launch another activity and expect a result:**
```kotlin
val intent = Intent(this, SecondActivity::class.java)
launcher.launch(intent)

```
**3. Set result in SecondActivity**

```kotlin
val resultIntent = Intent()
resultIntent.putExtra("key", "Some Data")
setResult(Activity.RESULT_OK, resultIntent)
finish()

```

----

### [List of ActivityResultContracts](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts)

----

**Note:**  
- Each launcher must have a unique name (you cannot define private val activityLauncher multiple times with the same name)
- Each launcher should be defined only once (in onCreate, or class body, not dynamically inside methods)

## Android Activity Priority Levels

When the Android system is low on memory and needs to reclaim resources, it decides which activity or component to kill based on priority and recency, not just the runtime duration (short-run vs long-run). Let's break it down clearly.

Android assigns importance levels (often referred to as "priority") to each process. The system kills the lowest priority process first when memory is needed.

**From highest to lowest priority:**

1. Foreground Activity – currently visible and interacting with the user.

2. Visible Activity – visible but not in the foreground (e.g., dialog partially over it).

3. Service Process – running a foreground or background service.

4. Background Activity – not visible, in back stack.

5. Empty Process – not holding any active component, just cached for faster reload.



### Important Concepts
- Both activities having same priority usually means they are in the background state.
- When multiple background activities are candidates for killing:

  - The system considers memory consumption, process age, and LRU (Least Recently Used) order.
  - Long-running background activity may have higher memory usage.
  - If both are equally recent in the LRU list, the one consuming more memory is more likely to be killed.
  - If all else is equal, the oldest one (least recently used) will be killed first.

----

### Retain data while Re-Creation of activity

-  ViewModel
- onSaveInstanceState / saveInstanceState
    ```kotlin
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("key", "value")
        super.onSaveInstanceState(outState)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val value = savedInstanceState?.getString("key")
    }
    
    ```
- 
---


## Questions
1. We have two launcher activities defined in our manifest file. While running the application, What will be the outcome?
>If you have two launcher Activities in your manifest When running the application, It will install two instances of the application(both instances behave the same). If you uninstall any one instance of the app, both instances will be uninstalled.


2. What happens to the Activity when the device is rotated?
> The Activity is destroyed and recreated by default. This happens because rotation triggers a configuration change.
> 
> onPause() → onStop() → onDestroy()
> 
> onCreate() → onStart() → onResume()
3. What's the difference between finish() and pressing the back button?
>  Both will:
> Call onPause() → onStop() → onDestroy() on the current android.
> 
> But:
> 
> finish() is programmatic — you call it explicitly in code.
>
>Back button is user-driven, and may be intercepted via onBackPressedDispatcher.

4. Can an android be in the onPause state but still be visible?
> Yes.
>
> Example: A dialog or transparent android appears on top.
>
> The underlying android is paused, but still partially visible.
5. What if you call finish() inside onCreate()?
> The activity is created, then immediately destroyed.
>
> Lifecycle calls:
> onCreate() → onDestroy()
> 
> onStart() and onResume() are not called.

6. What happens if two activities have the same intent filter with MAIN and LAUNCHER?
> When you launch the app, the system will ask you which one to open, showing a chooser dialog.
>
> If one is marked as “Always”, that becomes the default launcher.
7. What if the system kills the android in the background? Will onDestroy() be called?
> ❌ No.
>
>If the system kills your android (e.g., low memory), onDestroy() is NOT guaranteed to be called.
>
>You must save essential data in onSaveInstanceState().
8. Is onStop() always called before onDestroy()?
>  ✅ Yes, in most cases.
>
>However:
>
>If the system is under heavy load, it might skip onStop(), especially in low memory scenarios. But this is rare.

        

## 2.2 Fragment

# Fragment

## Fragment lifecycle methods:

| Method            | Called When…                                                        | Use Case                                                                   |
|-------------------|---------------------------------------------------------------------|----------------------------------------------------------------------------|
| `onAttach()`      | Fragment is associated with its host activity                       | Use this to get `Context` or communicate with the activity (via interface) | 
| `onCreate()`      | Fragment is being created (non-UI logic)                            | Initialize non-UI resources (e.g., ViewModels, arguments)                  | 
| `onCreateView()`  | UI layout is being created                                          | Inflate layout and initialize root views                                   |
| `onViewCreated()` | View is created, fully accessible                                   | Setup UI logic, bind views, observe LiveData, etc.                         | 
| `onStart()`       | Fragment is visible (UI is on screen)                               | Start animations, audio, or visible-related tasks                          |
| `onResume()`      | Fragment is fully interactive and in foreground                     | Start live actions (camera, sensors, gestures, listeners)                  | 
| `onPause()`       | Fragment is partially hidden or another fragment/activity is on top | Pause ongoing tasks, save UI state                                         | 
| `onStop()`        | Fragment is fully hidden                                            | Stop animations, unregister listeners                                      | 
| `onDestroyView()` | Fragment’s view hierarchy is removed                                | Clean up views, avoid memory leaks (set binding/view = null)               | 
| `onDestroy()`     | Fragment instance is about to be destroyed                          | Clean up remaining resources                                               | 
| `onDetach()`      | Fragment is disassociated from Activity                             | Final cleanup, reference to Activity should be cleared                     | 


## Activity And Fragment lifecycle calls as per launch

**When you run the app**
 
- MainActivity: onCreate
- SampleFragment: onAttach
- SampleFragment: onCreate
- SampleFragment: onCreateView
- SampleFragment: onViewCreated
- MainActivity: onStart
- SampleFragment: onStart
- MainActivity: onResume
- SampleFragment: onResume

**And during exit:**

- MainActivity: onPause
- SampleFragment: onPause
- MainActivity: onStop
- SampleFragment: onStop
- SampleFragment: onDestroyView
- SampleFragment: onDestroy
- SampleFragment: onDetach
- MainActivity: onDestroy


## 2.3 Service

# Service

## What is a Service in Android?

A Service is an Android component that runs in the background to perform long-running operations.

**Key Points:**

- No UI (unlike Activities).
- Useful for downloading files, playing music, handling network calls, etc.
- Keeps running even if the app is closed (depending on the type).
- By default, Service runs on **Main** thread

## Types of Services

### Started Service

- Started using startService() or ContextCompat.startForegroundService().
- Runs indefinitely until you stop it.
- **START_STICKY** - Restarts the service if the system kills it.
- **START_NOT_STICKY** - Doesn’t restart service unless explicitly started again.
-

**Create a Kotlin class extending Service**

```kotlin
class MyStartedService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Do your long-running work here
        Log.d("MyStartedService", "Service Started")
        return START_STICKY // 	Restarts the service if the system kills it.
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not used in started service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MyStartedService", "Service Destroyed")
    }
}

```

**Register in AndroidManifest.xml:**

```xml

<service android:name=".MyStartedService"/>
```

**Start and stop the service:**

```kotlin
val intent = Intent(this, MyStartedService::class.java)
startService(intent) // Start

stopService(intent) // Stop

```

### Bound Service

- Bound using bindService().
- Lives as long as another component is bound to it.
- Good for client-server communication within the same app.
- Used when you want components (like an activity) to bind to the service and interact.

```kotlin
class MyBoundService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MyBoundService = this@MyBoundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun performAction(): String {
        return "Action performed!"
    }
}
```

**Activity code**

```kotlin
class MyActivity : AppCompatActivity() {
    private var service: MyBoundService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as MyBoundService.LocalBinder).getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MyBoundService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}

```

### Foreground Service

- Displays a notification and has higher priority.
- Required for long tasks on Android 8.0+.
- **startForeground()** - Promotes a service to foreground to prevent it from being killed.

```kotlin
class MyForegroundService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "channelId")
            .setContentTitle("Foreground Service")
            .setContentText("Running in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

```

### AIDL (Android Interface Definition Language)

It’s a way to create Inter-Process Communication (IPC) between different apps or different processes in Android.

**Why Use AIDL?**

Normally, a Bound Service allows components to bind and talk to it — but only within the same process.

If you want:

- An app (or process) to talk to a service in another app (or process)
- To share complex data across apps
- To make a cross-app service API

➡️ **_You need AIDL_**

**How AIDL Works (Under the Hood)**

- You define an `.aidl` file → This is the interface
- Android generates a Binder Stub → Handles IPC for you
- You implement the service and the interface methods
- Client apps bind to your service using bindService()
- You exchange primitive data or Parcelable objects

**Example: Create AIDL Service**

Step 1: Define an AIDL interface
`IMathService.aidl` in `src/main/aidl/com/example/aidlservice/`

```aidl
package com.example.aidlservice;

interface IMathService {
    int add(int a, int b);
}

```

Supported types:

- `int`, `long`, `float`, `double`, `boolean`, `String`
- `List`, `Map`
- Parcelable objects

Step 2: Implement the Service

```kotlin
class MathService : Service() {

    private val binder = object : IMathService.Stub() {
        override fun add(a: Int, b: Int): Int = a + b
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}
```

Step 3: Register Service in AndroidManifest.xml

```xml

<service
        android:name=".MathService"
        android:exported="true"
        android:enabled="true">
    <intent-filter>
        <action android:name="com.example.aidlservice.IMathService"/>
    </intent-filter>
</service>
```

Step 4: Bind From Another App (Client)

- Add the same `.aidl` file to the client app
- Bind to the service like this:

```kotlin
class ClientActivity : AppCompatActivity() {

    private var mathService: IMathService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            mathService = IMathService.Stub.asInterface(binder)
            val result = mathService?.add(10, 20)
            Log.d("Client", "Result from service: $result")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mathService = null
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent("com.example.aidlservice.IMathService")
        intent.setPackage("com.example.aidlservice") // Package of service
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }
}

```

**Since Android 10+, bound services with AIDL must be explicitly exported and may need permissions for security.**



---

# Work Manager

WorkManager is part of Android Jetpack and is designed to perform deferrable, asynchronous tasks that are guaranteed to
execute, even if the app exits or the device restarts.

**It's best for background tasks that:**

- Should run even if the app process dies
- Are guaranteed to be executed
- Can be delayed or scheduled with constraints

**Examples:**

- Syncing data
- Uploading logs
- Periodic backups

## Types of Work

### OneTimeWorkRequest

Runs the work once.

```kotlin
val request = OneTimeWorkRequestBuilder<MyWorker>().build()
WorkManager.getInstance(context).enqueue(request)
```

### PeriodicWorkRequest

Runs the work repeatedly at defined intervals (minimum 15 minutes).

```kotlin
val periodicRequest = PeriodicWorkRequestBuilder<MyWorker>(15, TimeUnit.MINUTES).build()
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "MyPeriodicWork",
    ExistingPeriodicWorkPolicy.KEEP,
    periodicRequest
)
```

### UniqueWorkRequest

Avoids duplication. You can define unique names and policies like:

- KEEP
- REPLACE
- APPEND

```kotlin
WorkManager.getInstance(context).enqueueUniqueWork(
    "uniqueName",
    ExistingWorkPolicy.KEEP,
    OneTimeWorkRequestBuilder<MyWorker>().build()
)

```

## Chaining and Constraints

You can chain multiple workers using .then() and add constraints like:

- Network type
- Charging state
- Battery not low
- Storage not low
- Device idle (API 23+)

```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .setRequiresCharging(true)
    .build()

val request = OneTimeWorkRequestBuilder<MyWorker>()
    .setConstraints(constraints)
    .build()

WorkManager.getInstance(context).enqueue(request)

```

## Combine worker

**Define Your Workers**

```kotlin
class UploadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("UploadWorker", "Uploading file...")
        // Simulate work
        Thread.sleep(1000)
        return Result.success()
    }
}

class CompressWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("CompressWorker", "Compressing file...")
        Thread.sleep(1000)
        return Result.success()
    }
}

class NotifyWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("NotifyWorker", "Sending notification...")
        Thread.sleep(1000)
        return Result.success()
    }
}

```

**Enqueue Workers Using `.then()`**

```kotlin
val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>().build()
val compressWork = OneTimeWorkRequestBuilder<CompressWorker>().build()
val notifyWork = OneTimeWorkRequestBuilder<NotifyWorker>().build()

WorkManager.getInstance(context)
    .beginWith(uploadWork)
    .then(compressWork)
    .then(notifyWork)
    .enqueue()

```

**Tips**

- If any worker returns `Result.failure()`, the chain stops.
- You can also create parallel work by combining `List<WorkRequest>` in .then().
- Use `beginWith()` to start with multiple workers in parallel if needed.
- Chain `.then()` to enforce sequential execution.
- Monitor each job via WorkManager.getWorkInfoByIdLiveData(...).

----

# Questions

**Q.** What happens if you don’t call startForeground() within 5 seconds of startForegroundService()?
> Android will crash your app with an IllegalStateException on Android 8+.
>

**Q.** Can a BoundService also be started as a StartedService? What happens then?
> Yes, a service can be both bound and started. It’ll only stop when:
> All clients unbind
> ou explicitly call stopSelf()

**Q.** What’s the risk of returning START_REDELIVER_INTENT in onStartCommand() in a service that downloads a file?
> The system will re-deliver the same intent, potentially downloading the same file twice if you're not careful.

**Q.** Can a Service run indefinitely in the background without being foreground or bound?
>Nope. Since Android 8+, background services will be stopped by the system unless promoted to foreground (with
>notification).

**Q.** Is it safe to perform heavy work directly in a service?
> No. Services run on the main thread by default. Heavy work should use Thread, HandlerThread, Coroutine, or
> WorkManager.

**Q.** How would you design a service to continue location tracking across app restarts, device reboots, and Android
power constraints?
> Foreground service with location type
>
>BOOT_COMPLETED receiver
>
>Exclusion from battery optimizations (request from user)
>
>Proper handling of Doze Mode

**Q.** Explain how you’d handle communication between an Activity and a Service (bi-directional)
> Techniques include:
>
>AIDL
>
>Messenger (with Handler)
>
>Local BroadcastReceiver
>
>LiveData via a bound service

**Q.** How can you prevent a foreground service from being killed if the user removes the app from Recent Apps?
> Make it foreground
>
>Return START_STICKY
>
>Use onTaskRemoved() to restart service

## Cheat Sheet

## 📊 Android Background Task Mechanisms Comparison

| Use Case                       | Foreground Service | Background Service | WorkManager        | AlarmManager   | AIDL (IPC)                  |
|--------------------------------|--------------------|--------------------|--------------------|----------------|-----------------------------|
| 🔄 Run Immediately             | ✅ Yes              | ⚠️ Limited         | ❌ No               | ❌ No           | ⚠️ Only for IPC use         |
| ⏳ Deferred Execution           | ❌ Not suitable     | ⚠️ Risky           | ✅ Best choice      | ❌ Not designed | ❌ Not for scheduling        |
| 🕒 Scheduled at Fixed Time     | ⚠️ Not ideal       | ❌ No               | ⚠️ Approximate     | ✅ Best suited  | ❌                           |
| 🔁 Repeated at Fixed Intervals | ❌                  | ❌                  | ✅ ≥15 mins         | ✅ Yes          | ❌                           |
| 📶 Works with Constraints      | ❌                  | ❌                  | ✅ Yes              | ❌              | ❌                           |
| 🔋 Battery Optimized           | ❌ No               | ❌ No               | ✅ Yes              | ❌ No           | ❌                           |
| 🔒 Survives Reboot             | ⚠️ Needs handling  | ❌ No               | ✅ Yes (limited)    | ✅ Yes          | ❌                           |
| 🔧 Runs in Background          | ✅ Yes              | ⚠️ Risky on 8+     | ✅ Yes              | ✅ Yes          | ⚠️ For background IPC       |
| 🧠 Use When                    | Real-time tasks    | Legacy use only    | Deferred, reliable | Timed events   | Cross-process communication |




## 2.4 Broadcast Receiver

# BroadcastReceiver

A BroadcastReceiver is a component that lets your app listen to and respond to system-wide events or app-specific broadcasts.

Think of it like a radio receiver—your app “listens” for specific intents (actions), and when it hears one, it responds accordingly.


## Types of Broadcasts
1. **System Broadcasts**
   - Sent by Android system. 
   - Examples:
        ```kotlin
        Intent.ACTION_BOOT_COMPLETED
        
        Intent.ACTION_BATTERY_LOW
        
        ConnectivityManager.CONNECTIVITY_ACTION
        ```
     
2. **Custom Broadcasts**
   - Sent by your app. 
   - Useful for communication within your app or between different apps.


## Types of BroadcastReceivers

1. **Manifest-declared (Static) Receiver**
   - Declared in AndroidManifest.xml 
   - Works even when the app is not running. 
   - Cannot receive some broadcasts in Android 8.0+ (background limitations).
   - **Receiver class**
   ```kotlin
     
        class BootReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    Log.d("BootReceiver", "Device Booted")
                }
            }
        }
      ```
   - AndroidManifest.xml
   ```xml
      <receiver android:name=".BootReceiver"
                android:enabled="true"
                android:exported="true">
          <intent-filter>
              <action android:name="android.intent.action.BOOT_COMPLETED"/>
          </intent-filter>
      </receiver>
        
      <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    ``` 
2. **Context-registered (Dynamic) Receiver**
   - Registered using registerReceiver() in code.
   - Active only while app/component is running. 
   - More flexible and efficient.
   - **Dynamic Broadcast Receiver**

    ```kotlin
    class MainActivity : AppCompatActivity() {
        private val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("BatteryReceiver", "Battery low!")
            }
        }
    
        override fun onResume() {
            super.onResume()
            val filter = IntentFilter(Intent.ACTION_BATTERY_LOW)
            registerReceiver(batteryReceiver, filter)
        }
    
        override fun onPause() {
            super.onPause()
            unregisterReceiver(batteryReceiver)
        }
    }
    ```

## When to Use
- When your app needs to respond to:
  - System events (connectivity, charging, boot completed)
  - Custom internal broadcasts 
- Decouple components using your own custom broadcasts 
- Use in MVVM architecture to notify ViewModel/UI of background events


## Best Practices
- Use dynamic receivers when listening only during specific app states (active UI). 
- Use manifest receivers only when required to receive at boot or for system-level events. 
- Don’t do heavy work in onReceive() — offload to services or workers. 
- In Android 8.0+, static receivers are limited unless for certain implicit actions (like BOOT_COMPLETED).



## Questions

**Q 1**:
Can you receive a broadcast in Android 8.0+ using a static receiver for CONNECTIVITY_ACTION? Why or why not?

**A** : No, starting with Android 8.0 (API level 26), manifest-declared receivers can't receive most implicit broadcasts like CONNECTIVITY_ACTION, SMS_RECEIVED, etc., unless your app is in the foreground.
This was done to improve performance and battery life.

Instead, register a dynamic receiver using registerReceiver() while the app is active.

---

**Q 2**:
What happens if you register a BroadcastReceiver dynamically but forget to unregister it?

**A** : If you don’t unregister a dynamically registered receiver (e.g., in onPause() or onStop()), it can cause:

- Memory leaks (since the context holds reference to the receiver)
- Unexpected behavior if it receives broadcasts while the Activity is destroyed 
- Crashes, especially during onReceive(), if the context is no longer valid

---
**Q 3**:
Can a BroadcastReceiver start a Service or Activity?

**A** : Yes, a `BroadcastReceiver` can start a Service or Activity using `startService()` or` startActivity()` inside `onReceive()`.

However, `onReceive()` runs on the main thread and must finish quickly.
If starting a long-running operation, it’s better to use:
- `startForegroundService()` for long background work
- `WorkManager` for deferred/background tasks

--- 
**Q 4**:
Can a BroadcastReceiver run when the app is killed?

**A** : Yes, but only if it's a static (manifest-declared) receiver and the broadcast is one of the allowed implicit broadcasts (e.g., BOOT_COMPLETED).

However, in Android 8.0+, most static receivers will not work unless the app has been started once or the device is rebooted with a valid permission like RECEIVE_BOOT_COMPLETED.

--- 

**Q 5**:
If two apps listen to the same broadcast, in what order are they notified?

**A** : **The broadcast is delivered based on priority:**

- Ordered Broadcasts (sendOrderedBroadcast()): Receivers are called in order of their priority (declared via intent filters).

- Normal Broadcasts (sendBroadcast()): All receivers get the broadcast, but order is not guaranteed.

**With ordered broadcasts, receivers can:**

- Modify the broadcast data 
- Stop the broadcast using abortBroadcast() (deprecated in newer Android versions)

--- 

**Q 6**:
What is the difference between sendBroadcast() and sendOrderedBroadcast()?

**A** :

| Method                 | -Behavior                                                                                               |
|------------------------|---------------------------------------------------------------------------------------------------------|
| sendBroadcast()        | 	All receivers receive it asynchronously and unordered                                                  |
| sendOrderedBroadcast() | 	Receivers are triggered one after another, based on priority.Can modify data or abort further delivery |


------

**Q 7**:
Can BroadcastReceiver receive a broadcast while the app is in Doze Mode?

**A** : In Doze mode, Android restricts background tasks, and most broadcasts (especially implicit ones) are deferred.

However, some high-priority system broadcasts (like `SMS_RECEIVED`, `PHONE_STATE`) can still be received.

To work reliably in Doze:

Use JobScheduler or WorkManager

Use `setAndAllowWhileIdle()` or `setExactAndAllowWhileIdle()` for alarms

## 2.5 Content Provider

# Content Providers

A Content Provider is one of the core Android components (alongside Activities, Services, and Broadcast Receivers) that manages access to structured data. It acts as a bridge between different apps or parts of the same app for sharing data securely.

It uses a URI-based interface and operates using CRUD (Create, Read, Update, Delete) operations.



## Types of Content Providers
There are two kinds:

1. Standard Providers (System-defined) Android provides several built-in content providers. For example:
   - `ContactsContract` → to access contacts 
   - `MediaStore` → to access images, videos, audio 
   - `CalendarContract`, `Settings`, etc.
2. Custom Providers (User-defined)
   You create your own provider by extending ContentProvider class to expose your app’s data to other apps or components.

## When and Why to Use Content Providers
**Use When:**
- You want to share data between apps (e.g., messaging, gallery, file manager). 
- You want data abstraction with permission control. 
- You want to access structured data in a consistent way using URIs. 
- You’re building something like a note-taking or task manager app that could be queried externally.

**Avoid When:**
- You don’t need inter-app communication. 
- Local data is not meant to be accessed outside your app (use Room or SQLite instead).

## How to Use Content Providers
Steps:
1. Create a ContentProvider subclass 
2. Define a content:// URI using UriMatcher 
3. Override CRUD methods (query(), insert(), etc.)
4. Register the provider in AndroidManifest.xml 
5. Access via ContentResolver

**Example**

**Step 1: Define Contract (URI, Columns)**
```kotlin
object BookContract {
    const val AUTHORITY = "com.example.bookprovider"
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/books")

    object BookEntry {
        const val TABLE_NAME = "books"
        const val COLUMN_ID = "_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_AUTHOR = "author"
    }
}

```

**Step 2: Create Provider Class**

```kotlin
class BookProvider : ContentProvider() {
    private lateinit var dbHelper: SQLiteOpenHelper

    override fun onCreate(): Boolean {
        dbHelper = object : SQLiteOpenHelper(context, "book.db", null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE ${BookContract.BookEntry.TABLE_NAME} (
                        ${BookContract.BookEntry.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                        ${BookContract.BookEntry.COLUMN_TITLE} TEXT,
                        ${BookContract.BookEntry.COLUMN_AUTHOR} TEXT
                    )
                """)
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val db = dbHelper.readableDatabase
        return db.query(BookContract.BookEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        val id = db.insert(BookContract.BookEntry.TABLE_NAME, null, values)
        context?.contentResolver?.notifyChange(uri, null)
        return ContentUris.withAppendedId(BookContract.CONTENT_URI, id)
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int {
        val db = dbHelper.writableDatabase
        return db.update(BookContract.BookEntry.TABLE_NAME, values, selection, selectionArgs)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = dbHelper.writableDatabase
        return db.delete(BookContract.BookEntry.TABLE_NAME, selection, selectionArgs)
    }

    override fun getType(uri: Uri): String? {
        return "vnd.android.cursor.dir/vnd.${BookContract.AUTHORITY}.books"
    }
}

```

**Step 3: Register in AndroidManifest.xml**

```xml
<provider
    android:name=".BookProvider"
    android:authorities="com.example.bookprovider"
    android:exported="true" />
```

**Step 4: Access Using ContentResolver**

```kotlin
val values = ContentValues().apply {
    put(BookContract.BookEntry.COLUMN_TITLE, "Kotlin 101")
    put(BookContract.BookEntry.COLUMN_AUTHOR, "JetBrains")
}

val uri = context.contentResolver.insert(BookContract.CONTENT_URI, values)

// Querying
val cursor = context.contentResolver.query(BookContract.CONTENT_URI, null, null, null, null)

```



-----

### Can you use Realm in a Content Provider?
✅ Technically: Yes
You can use Realm (or any custom database/storage) inside a ContentProvider, but it's not recommended or straightforward.

❌ Practically: Not Recommended
Realm is not designed to work well with Content Providers, because:

🔍 Why Realm doesn’t play nicely with Content Providers
1. Cursor Requirement
   The query() method of a Content Provider is expected to return a Cursor.
   Realm doesn’t return Cursor objects — it uses its own data access APIs (e.g., RealmResults).

2. Threading
   Realm instances are thread-bound. Content Providers are accessed from different threads (main thread, binder thread pool), which can cause thread issues unless you manage Realm instances carefully.

3. Data Exposure
   Content Providers expose data using a standardized interface (URI + Cursor). Realm is optimized for in-app usage, not external data sharing.

4. No Out-of-the-box Integration
   Realm does not provide built-in support for Content Providers, so you'd have to manually map Realm data to Cursors, which is messy and error-prone.

⚙️ If you must use Realm with a Content Provider
You’d have to:

- Manually create a MatrixCursor (a subclass of Cursor)

- Fill it with data from your Realm query

- Return it from the query() method

```kotlin
override fun query(uri: Uri, ...): Cursor? {
    val cursor = MatrixCursor(arrayOf("_id", "name"))
    val realm = Realm.getDefaultInstance()
    val results = realm.where(Person::class.java).findAll()
    results.forEach {
        cursor.addRow(arrayOf(it.id, it.name))
    }
    return cursor
}
```



## 2.6 Intent

# Intent and Intent filter


## What is an Intent?

An Intent is a messaging object used to request an action from another app component. It is used to start activities, services, or deliver a broadcast.

**Basic Uses of Intent:**
- Start an android 
- Start a service 
- Deliver a broadcast

### Types of Intent
- **Explicit Intent**
  - Used to start a specific component (Activity/Service) within the same application. 
  - Use Case: Navigating from one android to another
  
```kotlin
val intent = Intent(this, SecondActivity::class.java)
intent.putExtra("username", "JohnDoe")
startActivity(intent)
```

- **Implicit Intent**
  - Used when you want any app that can perform a specific action (e.g., send an email, view a webpage) to handle the intent. 
  - Use Case: Open a URL in the browser or send an email.
```kotlin
val intent = Intent(Intent.ACTION_VIEW)
intent.data = Uri.parse("https://www.google.com")
startActivity(intent)
```

###  Intent Filters

Intent Filters are declared in the AndroidManifest.xml and tell the system what types of intents a component can handle.

**Structure of an Intent Filter:**

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:scheme="http" />
</intent-filter>
```

**Intent Components Breakdown**

| Component | Purpose                                                              |
|-----------|----------------------------------------------------------------------|
| Action    | The general action to perform (e.g., `VIEW`, `SEND`, `MAIN`)         |
| Data      | URI data the intent is acting on                                     |
| Category  | Gives additional info about the action (e.g., `DEFAULT`, `LAUNCHER`) |




### More example

**Open Dialer (Implicit)**
```kotlin
val phone = "tel:1234567890"
val intent = Intent(Intent.ACTION_DIAL).apply {
    data = Uri.parse(phone)
}
startActivity(intent)

```

**Send Email (Implicit)**

```kotlin
val intent = Intent(Intent.ACTION_SEND).apply {
    type = "message/rfc822"
    putExtra(Intent.EXTRA_EMAIL, arrayOf("example@email.com"))
    putExtra(Intent.EXTRA_SUBJECT, "Subject Here")
    putExtra(Intent.EXTRA_TEXT, "Body Here")
}
startActivity(Intent.createChooser(intent, "Send Email"))

```


### Intent Actions

| Action                             | Description                                                                                        |
|------------------------------------|----------------------------------------------------------------------------------------------------|
| `Intent.ACTION_MAIN`               | Entry point of the app. Used to launch the main android. Usually paired with `CATEGORY_LAUNCHER`. |
| `Intent.ACTION_VIEW`               | Display data to the user (e.g., open a URL, image, or contact).                                    |
| `Intent.ACTION_SEND`               | Send data to another app (e.g., share text or files).                                              |
| `Intent.ACTION_SENDTO`             | Send data to a specific recipient (e.g., email or SMS app).                                        |
| `Intent.ACTION_DIAL`               | Open the phone dialer with a number filled in, without making a call.                              |
| `Intent.ACTION_CALL`               | Directly make a phone call (requires permission).                                                  |
| `Intent.ACTION_EDIT`               | Edit the given data (e.g., open a contact for editing).                                            |
| `Intent.ACTION_INSERT`             | Insert new data (e.g., add a new contact or event).                                                |
| `Intent.ACTION_DELETE`             | Delete the given data.                                                                             |
| `Intent.ACTION_PICK`               | Allow the user to pick an item from data (e.g., image or contact picker).                          |
| `Intent.ACTION_GET_CONTENT`        | Allow user to select a piece of content (e.g., image, video).                                      |
| `Intent.ACTION_CHOOSER`            | Display a chooser dialog to let user select an app to handle the intent.                           |
| `Intent.ACTION_BOOT_COMPLETED`     | Broadcast sent after the system finishes booting.                                                  |
| `Intent.ACTION_TIME_TICK`          | Broadcast every minute (cannot be registered in manifest).                                         |
| `Intent.ACTION_POWER_CONNECTED`    | Broadcast when device is connected to power.                                                       |
| `Intent.ACTION_POWER_DISCONNECTED` | Broadcast when device is disconnected from power.                                                  |


### Intent Categories

| Category                               | Description                                                                             |
|----------------------------------------|-----------------------------------------------------------------------------------------|
| `Intent.CATEGORY_DEFAULT`              | Must be included for any android that responds to an implicit intent.                  |
| `Intent.CATEGORY_LAUNCHER`             | Indicates that this android should be displayed in the app launcher as an entry point. |
| `Intent.CATEGORY_BROWSABLE`            | Allows the android to be started from a web browser (e.g., via an HTTP link).          |
| `Intent.CATEGORY_ALTERNATIVE`          | Provides an alternative action the user can perform on data.                            |
| `Intent.CATEGORY_SELECTED_ALTERNATIVE` | Indicates the user has selected an alternative action.                                  |
| `Intent.CATEGORY_HOME`                 | Main home screen android (like launchers).                                             |
| `Intent.CATEGORY_APP_EMAIL`            | Designates app as an email client.                                                      |
| `Intent.CATEGORY_APP_MESSAGING`        | Designates app as a messaging client.                                                   |
| `Intent.CATEGORY_APP_CONTACTS`         | Designates app as a contacts app.                                                       |
| `Intent.CATEGORY_APP_BROWSER`          | Designates app as a web browser.                                                        |


## 2.7 Launch Modes

# Launch mode

In Android, Launch Modes define how a new instance of an Activity is associated with the current task and back stack when it's started. Understanding launch modes is crucial for managing how your app navigates between activities and handles the back stack.


### When an activity is started, Android can either:

- Create a new instance of the activity (even if it already exists),
- Or bring an existing instance to the front (depending on launch mode).

This affects how the activity stack behaves.

| Launch Mode    | Description                                                                                 |
|----------------|---------------------------------------------------------------------------------------------|
| standard       | Default mode. New instance is always created.                                               |
| singleTop      | If the activity is already on top, it is reused instead of creating a new one.              |
| singleTask     | A single instance in the entire task. If exists, all other activities above it are cleared. |
| singleInstance | Similar to singleTask but the activity lives in its own separate task.                      |


### You can define launch modes in two ways:

- Manifest file using android:launchMode 
- Intent flags at runtime using Intent.FLAG_ACTIVITY_*


## Real-World Example Using 4 Activities: A → B → C → D

### 1.  `standard` (default)
```text
Start A → Start B → Start C → Start D
Stack: A → B → C → D
```

### 2.  `singleTop` 
If an activity is launched and it is already on top, no new instance is created; otherwise, new one is added.
```text
Start A → Start B → Start C → Start D
Stack: A → B → C → D

Now again start D
Stack: A → B → C → D (new instance not added, but onNewIntent() is called)

Now start C again
Stack: A → B → C → D → C (new instance added, since C was not on top)
```

Only works if you're launching the same activity already at the top.


### 3.  `singleTask`
Only one instance is allowed in the task. If it already exists in the stack, it is brought to front, and all activities on top of it are cleared
```text
Start A → B → C → D
Stack: A → B → C → D

Now start B again (singleTask)
Result: B is brought to top with calling onNewIntent(), C and D are removed from stack 
Stack: A → B

```
This is useful when you want only one instance and clear all above it.


### 4.  `singleInstance`
Same as singleTask, but this activity lives in a completely separate task.

```text
Start A → B → C 
Now start D (singleInstance)

Result:
Task 1: A → B → C  
Task 2: D

Now if you press back in D → You’ll go to last opened activity in Android stack if it's empty then it will got Home screen

```

Used for activities like login screens, video players, or call UIs that should live outside the normal flow.

```xml
<activity
    android:name=".ActivityE"
    android:launchMode="singleInstance"
    android:taskAffinity="com.example.e_task"
    android:excludeFromRecents="false">
</activity>
```

 - **singleInstance**: puts it in its own task 
 - **taskAffinity**: creates true separation in Recents 
 - **excludeFromRecents**=false: so it appears in Recents



## Questions

1: You have Activity A → B → C. Now C launches B again, and B has launchMode="singleTask". What will the stack look like?

>B already exists in the stack.
>
>Since launchMode="singleTask", the existing B will be brought to the top, and C will be removed.
>
>️ Final Stack: A → B

2: If an activity has launchMode="singleTop" and it launches itself, will a new instance be created?
>
>No, if it's already on top, onNewIntent() will be called instead.
>
>Yes, if it's not on top (then it will create a new instance).

3: Which launch mode is best when you want to avoid creating multiple instances of the same activity in Recent Apps (Overview Screen)?

>singleInstance — since it uses a separate task, only one recent entry exists.

4: Can singleTop activity have multiple instances in the back stack?


>Yes, if it's not on top during each launch, it will create multiple instances.
>
>Example:
>Stack: A → B → B → B
> 
>If B is not on top when it's started, new instance is created even if it's singleTop.

5: What's the difference between using launchMode="singleTop" and setting Intent.FLAG_ACTIVITY_SINGLE_TOP?

>No difference in behavior, both ensure that if the activity is already on top, it is reused.
>
>Difference is where it's declared:
>
>launchMode → in manifest, permanent
>
>FLAG → in code, dynamic/per-launch

6: If an activity has launchMode="singleTask" and you start it from a different task, what happens?


>Android will bring the existing instance to the front (if any).
>
>If not found, a new instance is created in the task that launches it.
>
>So, it does not force a new task, unlike singleInstance.

7: What callback method is called when an already-running singleTop or singleTask activity is brought to the front?


>onNewIntent(intent: Intent?) is called.
>
>onCreate() is not called again.

8: What's the effect of Intent.FLAG_ACTIVITY_CLEAR_TOP?


>If the activity already exists in the stack, all activities above it are cleared.
>
>If used with FLAG_ACTIVITY_SINGLE_TOP, it reuses the existing activity and calls onNewIntent().
>
>Example:
>
>Stack: A → B → C
>Start A with FLAG_ACTIVITY_CLEAR_TOP
 Final Stack: A (B and C cleared)

9: Can singleInstance activity share task with other activities?

>No, singleInstance lives in its own task and cannot share it with others.
>
>Even if you start another activity from it, that activity opens in a new task.




## 2.8 ViewModel

# ViewModel

A ViewModel is a component from Android Jetpack’s Architecture Components. It is designed to store and manage UI-related data in a lifecycle-conscious way. The ViewModel survives configuration changes such as screen rotations, so your data does not need to be reloaded or recalculated.

### Why Use ViewModel?

When an Activity or Fragment is recreated (like during screen rotation), its data (like lists, UI state) is lost unless it's saved manually. A ViewModel allows you to:
 - Avoid unnecessary re-fetching or recalculating data. 
 - Decouple UI data from the view logic. 
 - Easily share data between fragments.

 ---

### How ViewModel Works Internally

**Basic Flow:**
- **Creation**: When an Activity or Fragment is created, it can request a ViewModel from a ViewModelProvider.
- **Storage**: Android uses a special internal object called ViewModelStore (attached to the Activity or Fragment) to keep the ViewModel.
- **Retention**: When the configuration changes (e.g., rotation), the Activity/Fragment is destroyed and recreated, but ViewModelStore persists, and so does your ViewModel.
- **Reuse**: The new instance of the Activity or Fragment gets the same ViewModel from the ViewModelProvider.

---

### Scope of ViewModel
- `ViewModelProvider(this)` → scoped to current Activity or Fragment.
- `ViewModelProvider(requireActivity())` → shared between fragments in same activity.


----

### Code for Simple ViewModel creation 

```kotlin
class MyViewModel : ViewModel() {
    val counter = MutableLiveData<Int>(0)

    fun increment() {
        counter.value = (counter.value ?: 0) + 1
    }
}

```

```kotlin
class MyActivity : AppCompatActivity() {
    private lateinit var viewModel: MyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(MyViewModel::class.java)

        viewModel.counter.observe(this) { count ->
            // Update UI
        }

        // Even after rotation, this ViewModel will be the same instance
    }
}

```


------

### Manual ViewModel with Factory (for ViewModel with constructor params)

ViewModel with constructor

```kotlin
 class MyViewModel(private val repository: MyRepository) : ViewModel() {
    val data = MutableLiveData<String>()

    fun loadData() {
        data.value = repository.fetchData()
    }
}

```

Custom ViewModel Factory

```kotlin
class MyViewModelFactory(private val repository: MyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyViewModel::class.java)) {
            return MyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

```

Create in Activity

```kotlin
class MyActivity : AppCompatActivity() {

    private lateinit var myViewModel: MyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val repository = MyRepository()
        val factory = MyViewModelFactory(repository)

        myViewModel = ViewModelProvider(this, factory).get(MyViewModel::class.java)

        myViewModel.data.observe(this) {
            println("Data: $it")
        }

        myViewModel.loadData()
    }
}

```


### What is SavedStateHandle in ViewModel?
SavedStateHandle is a Jetpack class used inside a ViewModel to store and retrieve key-value pairs of data that can survive process death and configuration changes.

It acts like a bundle that is lifecycle-aware and works with ViewModel to help you retain small pieces of UI state (like form inputs, scroll position, tab selection, etc.).


### Why use SavedStateHandle?
ViewModel retains data only across configuration changes, but not across process death (e.g., when the OS kills the app due to memory).

With SavedStateHandle, your ViewModel can automatically restore state even after the app is killed and restarted.



````kotlin
class MyViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val KEY_COUNTER = "counter"
    }

    // LiveData tied to SavedStateHandle
    val counter: MutableLiveData<Int> =
        savedStateHandle.getLiveData(KEY_COUNTER, 0)

    fun increment() {
        val current = counter.value ?: 0
        savedStateHandle[KEY_COUNTER] = current + 1
    }
}

````



## 2.9 LiveData, StateFlow & SharedFlow

# Live Data , State Flow and Share Flow


## Live Data
**LiveData** is an observable data holder class. Unlike a regular observable, LiveData is lifecycle-aware, meaning it respects the lifecycle of other app components, such as activities, fragments, or services. This awareness ensures LiveData only updates app component observers that are in an active lifecycle state.


**LiveData** considers an observer, which is represented by the Observer class, to be in an active state if its lifecycle is in the STARTED or RESUMED state. LiveData only notifies active observers about updates. Inactive observers registered to watch LiveData objects aren't notified about changes.

### Ways to set data in LiveData

1. **setValue(T value)**
    - Must be called from the main (UI) thread.
    - Sets the value immediately and notifies all active observers synchronously.
2. **postValue(T value)** - 
   - Can be called from any thread (background or main).
   - Posts the value asynchronously to the main thread. If multiple postValue calls happen in quick succession, only the last value is delivered.

### Declare the Live Data
```kotlin
class NameViewModel : ViewModel() {

    // Create a LiveData with a String
    val currentName: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
   
   // or 
   val currentName: MutableLiveData<String> = MutableLiveData()
   
   // or with default value
   val currentName: MutableLiveData<String> = MutableLiveData("NJ")


   // Rest of the ViewModel...
}

```

### Observe Live Data

```kotlin
 // Create the observer which updates the UI.
val nameObserver = Observer<String> { newName ->
    // Update the UI, in this case, a TextView.
    nameTextView.text = newName
}

// Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
model.currentName.observe(this, nameObserver)

Or

val liveData = MutableLiveData<String>()
liveData.observe(viewLifecycleOwner) { data ->
    // UI update
}

```


-----

## StateFlow 
* Hot Flow from Kotlin Coroutines
* Holds a state (always has a current value)
* Similar to LiveData but fully coroutine-based



### Ways to set data to StateFlow 

1. **.value**
    - The simplest way to set or update the current value.
    - immediately updates the value and emits it to collectors.
    ```kotlin
        val stateFlow = MutableStateFlow<Int>(0)
        
        // Update value
        stateFlow.value = 42
     ```
2. **.update {}** -
    - A thread-safe and atomic way to update the value based on the current one.
    - Useful for transformations that depend on the current value.
    
    ```kotlin
     stateFlow.update { currentValue ->
            currentValue + 1
        }
    ```



### Declare StateFlow:

```kotlin
val _stateFlow = MutableStateFlow("Initial")
val stateFlow: StateFlow<String> = _stateFlow

viewModelScope.launch {
    _stateFlow.value = "New Value"
}

```

### Observe / Collect

```kotlin
// Collecting in Activity (lifecycleScope required)
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.stateFlow.collect { data ->
            // update UI
        }
    }
}
```

---

## SharedFlow

* A hot flow that does not store state by default.
* Designed for events, one-time actions, and shared emissions.

### Setting Values in SharedFlow

1. Using emit(value: T) (suspend function)
   - This is the primary way to send values to a MutableSharedFlow. 
   - Suspend function → must be called inside a coroutine. 
   - Suspends if the buffer is full (depending on replay and buffer size).
    ```kotlin
    val sharedFlow = MutableSharedFlow<String>()
    
    // Inside coroutine
    sharedFlow.emit("Hello from SharedFlow")
    
    ```
2. Using tryEmit(value: T) (non-suspending function)
   - Attempts to emit a value immediately without suspension. 
   - Returns true if emission was successful, false otherwise (e.g., buffer full). 
   - Useful for fire-and-forget emissions where you don't want to suspend.
    ```kotlin
    val sharedFlow = MutableSharedFlow<String>(
         replay = 2,                    // Replays last 2 values to new collectors
         extraBufferCapacity = 64,     // Allows 64 more values beyond replay
         onBufferOverflow = BufferOverflow.DROP_OLDEST // Optional strategy
    )
    
    val emitted = sharedFlow.tryEmit("Try emit message")
    if (!emitted) {
        // handle failed emission (optional)
    }
    
    ```



### Declare SharedFlow:

```kotlin
val _sharedFlow = MutableSharedFlow<String>(replay = 0)
val sharedFlow: StateFlow<String> = _sharedFlow

viewModelScope.launch {
    _sharedFlow.emit("New Value")
}

```

### Observe / Collect

```kotlin
// Collecting in Activity (lifecycleScope required)
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.sharedFlow.collect { data ->
            // update UI
        }
    }
}
```


## 2.10 Serializable & Parcelable

# Serialize / Parcelable


| Feature       | `Serializable`                                   | `Parcelable`                                                  |
|---------------|--------------------------------------------------|---------------------------------------------------------------|
| Purpose       | Java standard interface for object serialization | Android-specific interface for high-performance serialization |
| Package       | `java.io.Serializable`                           | `android.os.Parcelable`                                       |
| Speed         | **Slower** – uses Java Reflection                | **Faster** – optimized for Android                            |
| Use Case      | Small objects, or not performance-critical       | Recommended for Android app IPC and Bundles                   |
| Code Required | Minimal – Just `implements Serializable`         | Verbose – need to implement multiple methods                  |
| Boilerplate   | Almost none                                      | A lot, though Kotlin `Parcelize` helps                        |
| Efficiency    | Creates temporary objects, uses reflection       | Manual serialization; more memory-efficient                   |




---- 

**Use Parcelable when:**

- You are passing objects through Intents, Bundles, or IPC in Android. 
- You care about performance. 
- You are working on Android components (Activities, Fragments, Services).

**Use Serializable when:**

- You're dealing with Java code, or using file/network I/O. 
- You want quick prototyping or need Java compatibility. 
- Performance isn't critical.




-----------


## Serializable implementation


```kotlin
import java.io.Serializable

data class User(
    val name: String,
    val age: Int
) : Serializable

```

--------

## Parcelable implementation


| Method / Member                         | Purpose                                                                                             |
|-----------------------------------------|-----------------------------------------------------------------------------------------------------|
| `writeToParcel(Parcel dest, int flags)` | Used to serialize the object to a `Parcel`. You write each property manually here.                  |
| `describeContents()`                    | Describes special objects in the `Parcelable` (e.g., file descriptors). Most of the time returns 0. |
| `CREATOR` object                        | Companion object that generates instances of your `Parcelable` class from a `Parcel`.               |


### Manual Parcelable – Traditional Way (Verbose)

```kotlin
data class User(val name: String, val age: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(age)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}

```

*
*How to Keep It Safe**
- Always match write and read order 
- Document the order if it's a large class 
- Use @Parcelize – it handles this automatically and safely
- In manual implementation, write unit tests for Parceling



### @Parcelize – Kotlin Way (Recommended)

```kotlin
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val name: String,
    val age: Int
) : Parcelable

```

#### Plugin

```groovy
plugins {
    id 'kotlin-parcelize'
}

```


--------




---

# 3. Jetpack Compose


## 3.1 Compose Fundamentals

## What are annotations?

In Jetpack Compose, annotations are special markers you add to code to give the compiler extra instructions.

- Most important: `@Composable`
  This tells the compiler that the function can be used in Compose UI code.

    ```kotlin
    @Composable
    fun Greeting(name: String) {
        Text(text = "Hello, $name!")
    }
    
    ```


## What is a composable function?

A Composable function is a function marked with @Composable and is used to describe part of the UI.

- Think of it like a widget or view.
- Compose builds the UI by calling these functions

    ```kotlin
    @Composable
    fun MyButton() {
        Button(onClick = { /* Do something */ }) {
            Text("Click Me")
        }
    }
    
    ```

## What is Preview?

`@Preview` is an annotation that lets you see what your composable looks like inside Android Studio, without running the app.


```kotlin
    @Preview(showBackground = true)
    @Composable
    fun PreviewGreeting() {
        Greeting("Compose")
    }
 ```   

## What are containers? Box, Column, Row?

In Compose, containers are layout composables used to arrange children.

Box → Overlapping children (like FrameLayout).

Column → Vertically aligned children.

Row → Horizontally aligned children.

```kotlin
@Composable
fun LayoutExample() {
    Column {
        Text("Line 1")
        Text("Line 2")
    }
}

```

## What is LazyColum

In Jetpack Compose, a scrollable list can be made using the LazyColumn composable. The difference between a LazyColumn
and a Column is that a Column should be used when you have a small number of items to display, as Compose loads them all
at once. A Column can only hold a predefined, or fixed, number of composables. A LazyColumn can add content on demand,
which makes it good for long lists and particularly when the length of the list is unknown. A LazyColumn also provides
scrolling by default, without additional code. Declare a LazyColumn composable inside of the AffirmationList() function.
Pass the modifier object as an argument to the LazyColumn.

## What is a scaffold?
Scaffold provides a basic layout structure with slots like:

topBar, bottomBar, floatingActionButton, drawerContent, etc.

It’s like a pre-built layout skeleton that follows Material guidelines.

Example:

```kotlin
@Composable
fun MyScaffoldScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("My App") }) },
        floatingActionButton = { FloatingActionButton(onClick = {}) { Text("+") } }
    ) {
        Text("Hello, world!", modifier = Modifier.padding(it))
    }
}

```

## What is a Modifier?

`Modifier` is how you style and position UI elements in Compose.

Used for padding, size, alignment, background, click events, etc.

Example:

```kotlin
Text(
    text = "Styled Text",
    modifier = Modifier
        .padding(16.dp)
        .background(Color.Yellow)
)

```

Modifiers are chained, and the order matters!

## What is state hoist?
State hoisting is moving state from a Composable to its caller.

Makes the Composable stateless and reusable.

Encourages separation of UI and business logic.

**Without hoisting:**

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}

```

**With hoisting:**

```kotlin
@Composable
fun Counter(count: Int, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("Count: $count")
    }
}

```


## What is composition?
Composition is the process of describing the UI by calling composable functions.

```java
@Composable
fun Greeting(name: String) {
    Text("Hello, $name!")
}

```
## What is the composition cycle? enter -> recompose -> exit
- Enter: When a composable enters the composition. 
- Recompose: When input to a composable changes, it re-runs to reflect new data. 
- Exit: When it's no longer needed and removed from the composition.

In below example each button click will trigger Re-composition
```kotlin
@Composable
fun CounterExample() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Clicked $count times")
    }
}

```

## What is recomposition?

Recomposition is the process where Compose re-executes a composable function to update the UI based on state changes.


## How does recomposition trigger?
When a @Composable function reads a state and that state changes, recomposition is triggered automatically.


## When recomposition trigger?

- When a State object changes. 
- When derivedStateOf changes. 
- When a lambda passed as a parameter changes.

## What is the state of your app, state of data, and state wrapper?
- App state: High-level state like login, theme, etc. 
- Data state: UI-specific data like a list of items. 
- State wrapper: Compose provides wrappers like mutableStateOf, remember, State<T>.


## What is mutableStateOf?
It creates an observable state object.

```kotlin
var name by remember { mutableStateOf("Jetpack") }
```

## What is remember? 
The remember function helps store a value in memory across recompositions. 

However, it does not persist the state during configuration changes, such as screen rotation or process recreation.

```kotlin
@Composable
fun RememberExample() {
    val count = remember { mutableStateOf(0) }
    Button(onClick = { count.value++ }) {
        Text("Count: ${count.value}")
    }
}
```

## In how many ways can you use remember?

- With mutableStateOf 
- With derived state 
- With lambdas 
- With any computed value
```kotlin
val greeting = remember { "Hello!" }
val derived = remember(count) { count * 2 }
```

## What is rememberSaveable? 

rememberSaveable is an extension of remember that retains the state across configuration changes by saving it into a Bundle, which is part of Android’s saved instance state mechanism.

```kotlin
val count = rememberSaveable { mutableStateOf(0) }
```

## What is Saver?
A Saver helps rememberSaveable store and restore complex objects by converting them into a format that can be saved into a Bundle (e.g., a Map or List).


### Steps to Create a Custom Saver:
- Define the Object You Want to Save:
  - Create a data class or object for your state.
- Implement a Saver:
  - Write a Saver that converts the object into a savable format and restores it when needed.
- Use the Saver in rememberSaveable:
  - Pass the Saver to rememberSaveable manage your custom object’s state

```kotlin
data class User(val name: String, val age: Int)

val UserSaver = Saver<User, Map<String, Any>>(
    save = { mapOf("name" to it.name, "age" to it.age) },
    restore = { User(it["name"] as String, it["age"] as Int) }
)

@Composable
fun CustomSaverExample() {
    var user by rememberSaveable(stateSaver = UserSaver) {
        mutableStateOf(User(name = "Akshay", age = 28))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Name: ${user.name}, Age: ${user.age}")
        Button(onClick = { user = user.copy(age = user.age + 1) })  
        { Text("Increase Age") }
     }
}
```

## How does rememberSaveable internally work?
- Uses Android’s SavedStateRegistry. 
- Converts the state into a Bundle via a Saver. 
- Restores it after config changes.

-------

## Testing

What is composite Test rule

## 3.2 Side Effects in Compose

# Side Effects

In Jetpack Compose, side effects refer to operations that cause a change in the outside world or system state.

In the context of Jetpack Compose, side effects are necessary to interact with the environment, like triggering a network request, modifying a UI element, or interacting with a system service.

Simple words non compose thing in compose function is side effects.

Jetpack Compose provides a set of APIs for handling side effects, and knowing when and how to use them is crucial for writing maintainable and efficient applications.


## What is Launched Effect?
`LaunchedEffect` is a special composable that allows you to launch coroutines in response to changes in the composition or input keys.

It’s used when you want to perform a side effect (like a network call, delay, animation, etc.) only once or every time a key changes.

```kotlin
@Composable
fun SideEffectsExample() {
    val count by remember { mutableIntStateOf(0) }
    LaunchedEffect(key1 = count) { // Whenever count state changes this coroutine is cancelled and re-launched
        delay(100)
        println("")
    }
}
```

If key1, key2, etc. change, the block is canceled and restarted

-----

## What is rememberCoroutineScope?

`rememberCoroutineScope` gives you a `CoroutineScope` that is tied to the Composable’s lifecycle. It survives recompositions and is canceled when the Composable leaves the composition.

- Useful when you want to launch coroutines in response to events (e.g., button click).
- Unlike LaunchedEffect, it doesn’t launch automatically—you control when to use it.
- Mostly we will not using this as we have View Model to handle the states

**Example**

```kotlin
@Composable
fun SaveButton() {
    val coroutineScope = rememberCoroutineScope()

    Button(onClick = {
        coroutineScope.launch {
            // Do something suspendable, like saving data
            saveDataToDatabase()
        }
    }) {
        Text("Save")
    }
}
```

-----

## What is rememberUpdatedState?

`rememberUpdatedState` is a side-effect utility in Jetpack Compose that lets you "remember" the latest value of a variable across recompositions, especially inside long-lived side-effect scopes like LaunchedEffect, DisposableEffect, or produceState.

It helps prevent stale values being captured by lambdas or coroutines in Compose.

**Why we need?**

In Compose, recomposition can happen often. If you launch a coroutine (e.g., in LaunchedEffect) and capture a lambda or value before recomposition, you might end up using old or outdated values.

rememberUpdatedState ensures your coroutine or callback always accesses the latest version of that value or lambda.

**Basic Example (Without rememberUpdatedState)**

```kotlin
@Composable
fun Greeting(onHello: () -> Unit) {
    // Suppose onHello changes due to recomposition

    LaunchedEffect(Unit) {
        delay(5000)
        onHello() // ⚠️ Might call the OLD lambda if recomposition happened
    }
}
```
Above code might call an outdated onHello after 5 seconds if recomposition changed it in the meantime.

**Fixed Example (With rememberUpdatedState)**

```kotlin
@Composable
fun Greeting(onHello: () -> Unit) {
    val currentOnHello by rememberUpdatedState(newValue = onHello)

    LaunchedEffect(Unit) {
        delay(5000)
        currentOnHello() // ✅ Always calls the latest lambda
    }
}
```
`rememberUpdatedState` keeps track of the most recent onHello.

Even if `onHello` changes, `currentOnHello` will always reflect the new value.

------


## What is DisposableEffect?

`DisposableEffect` is used when you need to perform side effects with cleanup. It runs a block of code when the Composable enters the composition and provides a onDispose callback for cleanup when the Composable is removed or a key changes.
- Use it for setting up and cleaning up listeners, broadcast receivers, observers, etc.
- Re-run and clean up when the key changes.

**Example**
```kotlin
@Composable
fun NetworkListener() {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Handle network change
            }
        }

        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(receiver, intentFilter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}
```

-----

## What is SideEffect Block?

SideEffect is a Composable function that allows you to run non-suspending, synchronous code after every successful recomposition.

It’s useful when you want to interact with parts of your app that are outside of Compose, like:

- Updating external classes
- Logging
- Analytics
- Triggering callbacks or changes to imperative code

📌 It always runs after a recomposition completes successfully.

**Real Use Case: Update an external object**
```kotlin
class UserTracker {
    var currentScreen: String = ""
}

val tracker = UserTracker()

@Composable
fun HomeScreen() {
    SideEffect {
        tracker.currentScreen = "Home"
    }

    Text("Home")
}
```

**What happens if you don't use SideEffect?**

```kotlin
@Composable
fun WrongExample(userName: String) {
    tracker.currentUser = userName // ❌ Do not update during composition!
    Text("Hello $userName")
}
```

------



## What is produceState?
`produceState` allows you to launch a coroutine in the composition and expose its result as a State, which can then be observed by the UI. It’s especially handy when fetching data asynchronously and binding it to UI state.

- Coroutine launched inside it is automatically canceled when the Composable leaves the composition.
- Good for integrating APIs, database queries, or long-running jobs into Compose.

**Example**

```kotlin
@Composable
fun UserProfile(userId: String) {
    val userState by produceState<User?>(initialValue = null, userId) {
        value = fetchUserFromNetwork(userId) // suspending function
    }

    if (userState == null) {
        CircularProgressIndicator()
    } else {
        Text("Hello, ${userState!!.name}")
    }
}
```
------
## What is derivedStateOf?

`derivedStateOf` is used to create computed values that only recompute when their inputs change. This helps avoid unnecessary recompositions by memoizing the derived value.
- Only recalculates the result when the dependent state(s) actually change.
- Optimizes performance in scenarios like filtering, sorting, or formatting data.

**Example**
```kotlin
@Composable
fun FilteredList(searchQuery: String, items: List<String>) {
    val filteredItems by remember(items, searchQuery) {
        derivedStateOf {
            items.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    LazyColumn {
        items(filteredItems) { item ->
            Text(item)
        }
    }
}
```



## What is the order of execution of all?

1. SideEffect

Called after every successful recomposition.

Useful for committing side-effects that must run after Compose has applied changes to the UI tree.

1. SideEffect
    - Called after every successful recomposition.
    - Useful for committing side-effects that must run after Compose has applied changes to the UI tree.

2. LaunchedEffect
    - Called after the first composition or when the key(s) change.
    - Starts a coroutine that runs independently from recompositions unless keys change.

3. DisposableEffect
    - Called after the first composition or when the key(s) change.
    - Also provides a onDispose callback for cleanup before the next effect or when leaving composition.

4. produceState
    - Runs a coroutine to initialize and update a State object from suspend functions or async data sources.
    - Typically starts after the composition, like LaunchedEffect, but is specifically meant to emit State.

5. rememberUpdatedState
    - Always runs during recomposition.
    - Used to hold the latest lambda or value to avoid stale captures in long-running effects (e.g., in LaunchedEffect or callbacks).

6. rememberCoroutineScope
    - Initializes once per composition; provides a coroutine scope but doesn’t launch anything itself.

7. derivedStateOf
- Used during recomposition.

Only recomputes when its dependencies change, for optimal performance

## When to use which side effect?

| **Use Case**                          | **Use This**             |
|---------------------------------------|--------------------------|
| Start coroutine when key changes      | `LaunchedEffect`         |
| Trigger code after each recomposition | `SideEffect`             |
| Cleanup resources on exit             | `DisposableEffect`       |
| Use latest value in long-running code | `rememberUpdatedState`   |
| Launch coroutine manually             | `rememberCoroutineScope` |
| Derive value from state               | `derivedStateOf`         |
| Convert async data to state           | `produceState`           |

---

## Summary

These side-effect APIs in Jetpack Compose help manage lifecycle-aware and state-aware operations. Use them to handle side-effects properly during recompositions and state updates.


---

# 4. Kotlin Coroutines & Flow


## 4.1 Coroutines

# Coroutine


Coroutines are a powerful feature in modern programming languages that allow for asynchronous and concurrent programming

>A coroutine is an instance of suspendable computation. It is conceptually similar to a thread, in the sense that it takes a block of code to run that works concurrently with the rest of the code. However, a coroutine is not bound to any particular thread. It may suspend its execution in one thread and resume in another one.

Coroutines are a lightweight alternative to threads.


## Suspended function
>Suspended function is function that could be started, paused, and resume. One of the most important points to remember about the suspend function is that they are only allowed to be called from a coroutine or another suspend function
 
When a coroutine is suspended, that thread is free for other coroutines. The continuation of the coroutine doesn't have to be on the same thread. Here we conclude that we can simultaneously run many coroutines with a small number of threads.


## Coroutine Builder

### launch

- It is typically used when you want to fire off a coroutine and don't need to wait for its result immediately.
- Launches a new coroutine without blocking the current thread and returns a reference to the coroutine as a `Job`.
    ```kotlin
    import kotlinx.coroutines.*
    
    fun main() {
        println("Start")
        
        // Launch a coroutine
        val job = GlobalScope.launch {
            delay(1000)
            println("Coroutine executed")
        }
        
        // Do some other work while the coroutine is running
        println("Do some other work")
        
        // Wait for the coroutine to finish
        runBlocking {
            job.join()
        }
        
        println("End")
    }
    
    ```

### async

- This coroutine builder is used when you want to perform a computation asynchronously and obtain a result.
- The `async` builder returns a `Deferred` object that represents the result of the computation
    ```kotlin
    import kotlinx.coroutines.*
    
    fun main() = runBlocking {
        println("Start")
    
        // Start two coroutines asynchronously
        val deferred1 = async {
            delay(1000)
            "Result from coroutine 1"
        }
        val deferred2 = async {
            delay(2000)
            "Result from coroutine 2"
        }
    
        // Do some other work while the coroutines are running
        println("Do some other work")
    
        // Wait for the coroutines to complete and retrieve the results
        val result1 = deferred1.await()
        val result2 = deferred2.await()
    
        println("Results: $result1, $result2")
        println("End")
    }
    
    ```

### runBlocking

- This coroutine builder is used to create a new coroutine and block the current thread until the coroutine completes.
- It is typically used in top-level code, such as the main function, to bridge non-coroutine code with coroutine-based
  code.
    ```kotlin
    import kotlinx.coroutines.*
    
    fun main() = runBlocking {
        println("Start")
    
        // Start a coroutine using runBlocking
        runBlocking {
            delay(1000)
            println("Coroutine executed")
        }
    
        println("End")
    }
    
    ```

---

## Coroutine Dispatchers

- The coroutine context includes a coroutine dispatcher that determines what thread or threads the corresponding
  coroutine uses for its execution.
- The coroutine dispatcher can confine coroutine execution to a specific thread, dispatch it to a thread pool, or let it
  run unconfined.
- All coroutine builders like `launch` and `async` accept an optional `CoroutineContext` parameter that can be used to
  explicitly specify the dispatcher for the new coroutine and other context elements.
- [Dispatchers Example Code](DispatchersExample.kt)

### Dispatchers.Default

- This is default dispatcher is used when no other dispatcher is explicitly specified in the scope.
- It used shared background pool of threads

### newSingleThreadContext

- Create a thread for the coroutine to run
- Dedicated thread is very expensive resource.
- This should be closed using `close`  when no longer in use or should be keep at top level throughout the application

### Dispatchers.Unconfined

- A coroutine dispatcher that is not confined to any specific thread.
- It starts a coroutine in the caller thread, but only until the first suspension point.
- The unconfined dispatcher is appropriate for coroutines which neither consume CPU time nor update any shared data (
  like UI) confined to a specific thread.
- It executes the initial continuation of a coroutine in the current call-frame and lets the coroutine resume in
  whatever thread that is used by the corresponding suspending function, without mandating any specific threading
  policy.
    - [Nested coroutines launched in this dispatcher form an event-loop to avoid stack overflows](UnConfinedEventLoop.kt)
        - Event loop semantics is a purely internal concept and has no guarantees on the order of execution except that
          all queued coroutines will be executed on the current thread in the lexical scope of the outermost unconfined
          coroutine.
          ```kotlin
            fun main() = runBlocking { 
                withContext(Dispatchers.Unconfined) { 
                    println(1)
                    launch(Dispatchers.Unconfined) {
                        println(2)
                     }
                     println(3)
                }
                println("Done")
            }
          ```
        - Can print both "1 2 3" and "1 3 2".
        - This is an implementation detail that can be changed. However, it is guaranteed that **Done** will only be
          printed once the code in both `withContext` and `launch` completes.
    - [Unconfined example](UnConfinedDispatcher.kt)

---

## Coroutine Scope

- It defines a context in which coroutines are executed and provides a way to manage the lifecycle of coroutines.
- Coroutine scope is typically used to launch coroutines and ensure their proper cancellation.
- CoroutineScope is an interface with only one property `coroutineContext`
- Every **coroutine builder** is extension of **CoroutineScope**
- **CoroutineScope** has two main builders `launch` and `async`
- To create a coroutine scope, you can use the `coroutineScope` or `supervisorScope` function
- [Coroutine Scope example](CoroutineScopeExample.kt)

### coroutineScope

- Function creates a new child scope, and the cancellation of this scope will propagate to all child coroutines

### supervisorScope

- function creates a new child scope but provides a supervisor job, allowing child coroutines to fail independently
  without canceling the whole scope.

**Difference table**

|                       | `coroutineScope`                                                                   | `supervisorScope`                                                                      |
|-----------------------|------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| Error Propagation     | Propagates exceptions to its parent scope                                          | Does not propagate exceptions to its parent scope                                      |
| Cancellation          | Cancels all child coroutines on cancellation                                       | Does not cancel other child coroutines on cancellation                                 |
| Completion            | Waits for all child coroutines to complete                                         | Does not wait for all child coroutines to complete                                     |
| Parent Responsibility | Parent is responsible for handling exceptions and cancellation of child coroutines | Parent is not responsible for handling exceptions and cancellation of child coroutines |
| Code Example          | [Example code](CoroutineScopeInBuildExample.kt)                                    | [Example Code](SupervisorScopeExample.kt)                                              |


---- 

1. What are Kotlin Coroutines, and why are they used?

## 4.2 Flow & Channels

# Flow and Channel 

# Flow
In coroutines, a flow is a type that can emit multiple values sequentially, as opposed to suspend functions that return
only a single value. For example, you can use a flow to receive live updates from a database

Flows are built on top of coroutines and can provide multiple values. A flow is conceptually a stream of data that can
be computed asynchronously. The emitted values must be of the same type. For example, a Flow<Int> is a flow that emits
integer values.

## What are Terminal Operators in Kotlin Flow

* Terminal operators start the flow collection.
* Without a terminal operator, the flow will not emit anything.
* Most use cases call collect as the primary terminal operator.

| Operator            | Description                                                                                                                                |
|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| **`collect`**       | Collects all emitted values and processes them                                                                                             |
| **`collectLatest`** | Collects only the latest emission, cancels previous collector if new value comes                                                           |
| **`toList`**        | Collects all emitted values into a `List`                                                                                                  |
| **`toSet`**         | Collects all emitted values into a `Set`                                                                                                   |
| **`single`**        | Expects exactly one value and returns it. If more than one value throws java.lang.IllegalArgumentException: Flow has more than one element |
| **`singleOrNull`**  | Returns the single value or null if none emitted or more than one value                                                                    |
| **`first`**         | Returns the first emitted value                                                                                                            |
| **`firstOrNull`**   | Returns the first emitted value or null if none emitted                                                                                    |
| **`last`**          | Returns the last emitted value                                                                                                             |
| **`lastOrNull`**    | Returns the last emitted value or null if none emitted                                                                                     |
| **`reduce`**        | Accumulates values into a single value (terminal)                                                                                          |
| **`fold`**          | Like reduce, but with an initial value                                                                                                     |

## Intermediate Operators (or Transforming Operators)

Operators that are not terminal are called Intermediate Operators (or Transforming Operators).

* They transform, filter, or modify the emitted values from a flow.
* They return a new flow and do not trigger execution on their own.
* You can chain multiple intermediate operators before collecting.

| Operator                   | Description                                                                                                                               |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| **`map`**                  | Transforms each emitted value                                                                                                             |
| **`filter`**               | Emits only values that match a condition                                                                                                  |
| **`take`**                 | Takes only the first N values                                                                                                             |
| **`drop`**                 | Skips the first N values                                                                                                                  |
| **`distinctUntilChanged`** | Emits only when value changes                                                                                                             |
| **`onEach`**               | Performs an action on each emitted value (side effect)                                                                                    |
| **`buffer`**               | Buffers emissions to allow concurrency                                                                                                    |
| **`flatMapConcat`**        | Maps values to flows and concatenates emissions. It waits for the previous inner flow to complete before starting the next one.           |
| **`flatMapMerge`**         | Maps values to flows and merges emissions concurrently. It starts all inner flows immediately and emits values as they arrive, unordered. |
| **`flatMapLatest`**        | Maps values to flows and switches to latest only.This is great for UI or search — only the latest result matters                          |
| **`debounce`**             | Emits value only if a specified time passed without new emission                                                                          |
| **`sample`**               | Emits the latest value periodically                                                                                                       |
| **`retry`**                | Retries flow collection on error                                                                                                          |
| **`flowOn`**               | Changes the coroutine dispatcher the flow runs on                                                                                         |
| **`zip`**                  | Combines emissions of two flows pairwise if extra value in one of flow it will be ignored                                                 |
| **`combine`**              | Combines latest emissions of multiple flows if extra value in one of flow always matched with last value of other flow                    |

### Creating  and collecting Flow

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    flowOf(1, 2, 3, 3, 6, 7, 8)
        .flowOn(Dispatchers.IO) // // Change the upstream context to IO
        .distinctUntilChanged() // takes only distinct value (1, 2, 3, 6, 7, 8)
        .drop(2)// drops first two element (3, 6, 7, 8)
        .take(10) // takes only 10 element (3, 6, 7, 8) as size if less than 10
        .filter { it % 2 == 0 }  // Only even numbers (6,8)
        .map { it * 2 }  // Multiply each number by 2 (12, 16)
        .onEach { it / 2 } // nothing happens on actual value (12,16)
        .collect { println(it) }  // 12 16
}
```

### Reduce or fold

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    val result = flowOf(1, 2, 3, 3, 6, 7, 8)
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged() // 1, 2, 3, 6, 7, 8
        .drop(2)                // 3, 6, 7, 8
        .take(10)               // 3, 6, 7, 8
        .filter { it % 2 == 0 } // 6, 8
        .map { it * 2 }         // 12, 16
        .onEach { it / 2 }      // has no effect, consider logging here
        //.reduce { acc, value -> acc + value } // 12 + 16 = 28
        .fold(30) { acc, value -> acc + value } // 30 + 12 + 16 = 58

    println(result) // Output: 28
}

```

### Combine, Zip Flow

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    val flow1 = flowOf(1, 2, 3, 4)
    val flow2 = flowOf("A", "B")


    /***
     * 1 -> A
     * 2 -> B
     * 3 -> B
     * 4 -> B
     ***/
    flow1
        .combine(flow2) { num, letter ->
            "$num -> $letter"
        }
        .collect { println(it) }


    /***
     * 1 -> A
     * 2 -> B
     ***/
    flow1
        .zip(flow2) { number, letter ->
            "$number -> $letter"
        }
        .collect { println(it) }
}

```

### flatMapConcat, flatMapMerge and flatMapLatest

**flatMapConcat**

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    flowOf(1, 2)
        .flatMapConcat { value ->
            flow {
                emit("Start $value")
                delay(100)
                emit("End $value")
            }
        }
        .collect { println(it) }
}
/***
 * Start 1
 * End 1
 * Start 2
 * End 2
 */
```

**flatMapMerge**

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    flowOf(1, 2)
        .flatMapMerge { value ->
            flow {
                emit("Start $value")
                delay(100)
                emit("End $value")
            }
        }
        .collect { println(it) }
}

/***
 * Start 1
 * Start 2
 * End 1
 * End 2
 */
```

**flatMapLatest**

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    flow {
        emit(1)
        delay(50)
        emit(2)
        delay(50)
        emit(3)
    }
        .flatMapLatest { value ->
            flow {
                emit("Start $value")
                delay(100)
                emit("End $value")
            }
        }
        .collect { println(it) }
}

/****
 * Start 1
 * Start 2
 * Start 3
 * End 3
 */
```

---

## Difference ways to create flow in Kotlin

### Using flowOf
 - Creates a flow that emits given values sequentially.

```kotlin
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

fun main() = runBlocking {
    flowOf(1, 2, 3).collect { println(it) }
}

```

### Using flow {} builder
- Custom flow that can emit values manually and support suspend functions.
```kotlin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

fun main() = runBlocking {
    flow {
        emit(1)
        delay(100)
        emit(2)
    }.collect { println(it) }
}

```

### Using asFlow()
 - Convert collections or sequences into a flow.
```kotlin
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

fun main() = runBlocking {
    listOf(10, 20, 30).asFlow().collect { println(it) }
}

```

### Using channelFlow
 - For flows where you want to emit values from multiple coroutines or channels.

```kotlin
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

fun main() = runBlocking {
    channelFlow {
        launch { send(1) }
        launch { send(2) }
    }.collect { println(it) }
}

```

### Using callbackFlow
- To convert callback-based APIs into flows.

```kotlin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

fun main() = runBlocking {
    callbackFlow<Int> {
        val listener = object {
            fun onEvent(value: Int) {
                trySend(value)
            }
        }
        // Simulate event emission
        listener.onEvent(100)
        awaitClose { /* cleanup listener here */ }
    }.collect { println(it) }
}

```


| Method         | Use Case                                  |
|----------------|-------------------------------------------|
| `flowOf`       | Simple fixed set of values                |
| `flow {}`      | Manual emission, suspendable code         |
| `asFlow()`     | Convert collections/sequences             |
| `channelFlow`  | Multi-coroutine or channel-based emission |
| `callbackFlow` | Convert callback APIs to Flow             |


----


# Channel

| Aspect             | Channels                                                                            | Flows                                                                                              |
|--------------------|-------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| **Concept**        | A **hot** stream, like a queue for sending and receiving values between coroutines. | A **cold** asynchronous stream of data that emits values when collected.                           |
| **Hot vs Cold**    | **Hot:** Active even if no one is receiving. Sends values to whoever is listening.  | **Cold:** Starts producing values only when collected (like a sequence).                           |
| **Backpressure**   | Supports backpressure explicitly via suspending send/receive.                       | Handles backpressure implicitly by suspending upstream emissions until downstream is ready.        |
| **Use case**       | Useful for communication between coroutines, event bus, or pipelines.               | Used for representing asynchronous streams of data (e.g., UI updates, network responses).          |
| **API Style**      | Uses **send** and **receive** or **offer** and **poll**.                            | Uses declarative operators like `map`, `filter`, `combine`, and terminal operators like `collect`. |
| **Lifecycle**      | Has to be explicitly closed to avoid leaks.                                         | Lifecycle managed by collection; no explicit closing needed.                                       |
| **Error Handling** | Requires manual closing and exception handling on both ends.                        | Flow operators provide structured error handling with `catch`.                                     |


## 4.3 Coroutine Code Examples

### Basic Coroutine Example

```kotlin
package coroutine

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    launch {
        delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
        println("World!") // print after delay
    }
    println("Hello") // main coroutine continues while a previous one is delayed
}```


### Coroutine Scope Example

```kotlin
package coroutine

import kotlinx.coroutines.runBlocking

import kotlinx.coroutines.*

fun main() {
    runBlocking {
        println("Main coroutine starts")

        /***
         * Create a new coroutine scope
         * We pass a Job and a Dispatcher as parameters.
         * The Job represents the lifecycle of the scope, and the Dispatcher determines the execution context for
         * the coroutines within the scope (in this case, Dispatchers.Default).
         */
        val scope = CoroutineScope(Job() + Dispatchers.Default)


        // Launch coroutines within the scope
        scope.launch {
            delay(1000)
            println("Coroutine 1 executed")
        }

        // Launch coroutines within the scope
        scope.async {
            delay(2000)
            println("Coroutine 2 executed")
            "Coroutine 2 result"
        }.await()

        println("Main coroutine ends")
    }
}
```


### Built-in Coroutine Scope Example

```kotlin
package coroutine

import kotlinx.coroutines.*

fun main() = runBlocking {
    println("Main coroutine starts")

    coroutineScope {
        launch {
            delay(500)
            println("Coroutine 1 executed")
        }

        launch {
            delay(1000)
            throw Exception("Coroutine 2 failed")
        }

        launch {
            delay(1500)
            throw Exception("Coroutine 3 executing")
        }
    }

    println("Main coroutine ends")
}


/****
Main coroutine starts
Coroutine 1 executed
Exception in thread "main" java.lang.Exception: Coroutine 2 failed
 at FileKt$main$1$1$2.invokeSuspend (File.kt:15) 
 at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith (ContinuationImpl.kt:33) 
 at kotlinx.coroutines.DispatchedTaskKt.resume (DispatchedTask.kt:235) 
***/
```


### Dispatchers Example

```kotlin
package coroutine

import kotlinx.coroutines.*

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    runBlocking {
        launch {
            println("Main Run Blocking => Working on ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Unconfined) {
            println("Unconfined => Working on ${Thread.currentThread().name}")
        }

        launch(Dispatchers.Default) {
            println("Default => Working on ${Thread.currentThread().name}")
        }

        launch(newSingleThreadContext("MyThread")) {
            println("newSingleThreadContext => Working on ${Thread.currentThread().name}")
        }
    }
}```


### Supervisor Scope Example

```kotlin
package coroutine

import kotlinx.coroutines.*

fun main() = runBlocking {
    println("Main coroutine starts")

    supervisorScope {
        launch {
            delay(1000)
            println("Coroutine 1 executed")
        }

        launch {
            delay(1500)
            throw Exception("Coroutine 2 failed")
        }

        launch {
            delay(2000)
            println("Coroutine 3 executed")
        }
    }

    println("Main coroutine ends")
}


/***
Main coroutine starts
Coroutine 1 executed
Exception in thread "main @coroutine#3" java.lang.Exception: Coroutine 2 failed
	at FileKt$main$1$1$2.invokeSuspend(File.kt:15)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTaskKt.resume(DispatchedTask.kt:235)
	at kotlinx.coroutines.DispatchedTaskKt.dispatch(DispatchedTask.kt:168)
	at kotlinx.coroutines.CancellableContinuationImpl.dispatchResume(CancellableContinuationImpl.kt:474)
	at kotlinx.coroutines.CancellableContinuationImpl.resumeImpl(CancellableContinuationImpl.kt:508)
	at kotlinx.coroutines.CancellableContinuationImpl.resumeImpl$default(CancellableContinuationImpl.kt:497)
	at kotlinx.coroutines.CancellableContinuationImpl.resumeUndispatched(CancellableContinuationImpl.kt:595)
	at kotlinx.coroutines.EventLoopImplBase$DelayedResumeTask.run(EventLoop.common.kt:493)
	at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:280)
	at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:85)
	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking(Builders.kt:59)
	at kotlinx.coroutines.BuildersKt.runBlocking(Unknown Source)
	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking$default(Builders.kt:38)
	at kotlinx.coroutines.BuildersKt.runBlocking$default(Unknown Source)
	at FileKt.main(File.kt:4)
	at FileKt.main(File.kt)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
	at java.base/java.lang.reflect.Method.invoke(Unknown Source)
	at executors.JavaRunnerExecutor$Companion.main(JavaRunnerExecutor.kt:27)
	at executors.JavaRunnerExecutor.main(JavaRunnerExecutor.kt)
	Suppressed: kotlinx.coroutines.internal.DiagnosticCoroutineContextException: [CoroutineId(3), "coroutine#3":StandaloneCoroutine{Cancelling}@6b143ee9, BlockingEventLoop@1936f0f5]
Coroutine 3 executed
Main coroutine ends
***/
```


### Unconfined Dispatcher Example

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


/***
 * -The dispatcher is inherited from the outer CoroutineScope by default.
 * - The default dispatcher for the runBlocking coroutine, in particular, is confined to the invoker thread,
 *   so inheriting it has the effect of confining execution to this thread with predictable FIFO scheduling.
 */
fun main() {
    runBlocking {
        launch(Dispatchers.Unconfined) { // not confined -- will work with main thread
            println("Unconfined      : I'm working in thread ${Thread.currentThread().name}")
            delay(500)
            println("Unconfined      : After delay in thread ${Thread.currentThread().name}")
        }
        launch { // context of the parent, main runBlocking coroutine
            println("main runBlocking: I'm working in thread ${Thread.currentThread().name}")
            delay(1000)
            println("main runBlocking: After delay in thread ${Thread.currentThread().name}")
        }
    }
}```


### Unconfined Event Loop Example

```kotlin
package coroutine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun main() = runBlocking {
    withContext(Dispatchers.Unconfined) {
        println(1)
        launch(Dispatchers.Unconfined) {
            println(2)
        }
        println(3)
    }
    println("Done")
}```


---

# 5. Threading


## 5.1 Thread, Looper & Handler

# Thread, Handler, Looper , Executor, Message Queue


## Thread

A thread is a unit of execution within a process. 
In Android, the main thread (UI thread) is responsible for handling UI updates and interactions. Running long tasks (e.g., network requests, database operations) on the main thread can lead to ANR (Application Not Responding) errors.

Threads in Android can be created using the `Thread` class or `Runnable` interface.

**Example**

```kotlin
class MyThread : Thread() {
    override fun run() {
        // Background task
        for (i in 1..5) {
            println("Thread running: $i")
            Thread.sleep(1000) // Simulating work
        }
    }
}

val thread = MyThread()
thread.start()

```

or

```kotlin
val runnable = Runnable {
    for (i in 1..5) {
        println("Runnable running: $i")
        Thread.sleep(1000)
    }
}

val thread = Thread(runnable)
thread.start()

```

## Handler
A Handler allows you to send and process Message and Runnable objects associated with a thread's MessageQueue. 

Each Handler instance is associated with a single thread and that thread's message queue. When you create a new Handler it is bound to a Looper. 

It will deliver messages and runnables to that Looper's message queue and execute them on that Looper's thread.


There are two main uses for a Handler: 
1. to schedule messages and runnables to be executed at some point in the future; and 
2. to enqueue an action to be performed on a different thread than your own.


## Looper
Class used to run a message loop for a thread. Threads by default do not have a message loop associated with them; to create one, call prepare() in the thread that is to run the loop, and then loop() to have it process messages until the loop is stopped.

Most interaction with a message loop is through the [Handler](#handler) class.

This is a typical example of the implementation of a Looper thread, using the separation of prepare() and loop() to create an initial Handler to communicate with the Looper.

```kotlin
internal class LooperThread : Thread() {
    var mHandler: Handler? = null

    override fun run() {
        Looper.prepare() // initialise Message queue for thread

        mHandler = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                // process incoming messages here
            }
        }

        Looper.loop() //  enters an infinite loop and starts waiting for messages in the MessageQueue
    }
}
```


## Message Queue

Low-level class holding the list of messages to be dispatched by a [Looper](#looper). 
Messages are not added directly to a `MessageQueue`, but rather through [Handler](#handler) objects associated with the [Looper](#looper).

You can retrieve the `MessageQueue` for the current thread with `Looper.myQueue()`.



Example for Thread , Handler, Looper and Message Queue

**Creation of Thread**
```kotlin
        // Start the LooperThread
        val looperThread = LooperThread()
        looperThread.start()

        // Wait for thread to be ready
        Thread.sleep(500)

        // Post a delayed stop after 5 seconds
        looperThread.mHandler?.postDelayed({
            Looper.myLooper()?.quit()
            Log.d("LooperThread","Looper stopped!")
        }, 5000)


        // send message to handler using 500 ms delay
        looperThread.mHandler?.post {
            for (i in 1..50) {
                val msg = Message.obtain()
                msg.what = i
                looperThread.mHandler?.sendMessage(msg)
                Thread.sleep(500)
            }
        }
```



```kotlin
   internal class LooperThread : Thread() {
        var mHandler: Handler? = null

        override fun run() { 
            Looper.prepare() // initialise Message queue for thread

            mHandler = object : Handler(Looper.myLooper()!!) {
                override fun handleMessage(msg: Message) {
                    Log.d("LooperThread","Processing message: ${msg.what}")
                }
            }

            Looper.loop() //  enters an infinite loop and starts waiting for messages in the MessageQueue
        }
    }
```


**Do not call Looper.loop() Before Creating a Handler**

```kotlin
internal class LooperThread : Thread() {
    var mHandler: Handler? = null

    override fun run() {
        Looper.prepare()

        // Calling Looper.loop() before creating a Handler
        Looper.loop()  // ❌ Thread is stuck here! The handler will never be created.

        mHandler = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                println("Processing message: ${msg.what}")
            }
        }
    }
}

val looperThread = LooperThread()
looperThread.start()

// Trying to post a message (This will fail!)
Thread.sleep(500)
looperThread.mHandler?.sendMessage(Message.obtain().apply { what = 1 })

```


## Executor

### 🔹 Types of Executors in Android

An Executor is a higher-level thread management framework that simplifies background task execution. It is part of the Java Concurrency API (java.util.concurrent.Executor).

Instead of manually creating and managing threads, an Executor efficiently handles thread pooling and task scheduling.

| Executor Type               | Description                                             | Usage                                          |
|-----------------------------|---------------------------------------------------------|------------------------------------------------|
| `newSingleThreadExecutor()` | One background thread for all tasks.                    | Serial execution, background database updates. |
| `newFixedThreadPool(n)`     | Pool of `n` threads.                                    | Parallel execution of tasks.                   |
| `newCachedThreadPool()`     | Creates threads as needed and reuses idle threads.      | Dynamic workloads with short tasks.            |
| `newScheduledThreadPool(n)` | Runs tasks **after a delay** or **at fixed intervals**. | Periodic tasks, scheduled background sync.     |

### ✅ Explanation:
- **`newSingleThreadExecutor()`** → Useful for sequential execution (one task at a time).
- **`newFixedThreadPool(n)`** → Good for parallel execution, limits the number of threads.
- **`newCachedThreadPool()`** → Best for dynamic workloads (threads created as needed).
- **`newScheduledThreadPool(n)`** → Used for periodic or delayed execution.

### ✅ Example: Using `Executor` to Run Background Tasks
```kotlin
import java.util.concurrent.Executors

val executor = Executors.newSingleThreadExecutor()

executor.execute {
    println("Running task in background thread: ${Thread.currentThread().name}")
}
```

#### 🔹 How It Works:
- `Executors.newSingleThreadExecutor()` creates an **Executor with a single background thread**.
- `execute {}` submits a **runnable task** to the executor.
- The task runs **asynchronously** on a separate thread.

### ✅ Example: Running Multiple Background Tasks
```kotlin
val executor = Executors.newFixedThreadPool(3)

repeat(5) { taskNumber ->
    executor.execute {
        println("Task $taskNumber running on ${Thread.currentThread().name}")
    }
}
```

#### 🔹 Expected Output (Thread Names May Vary)
```
Task 0 running on pool-1-thread-1
Task 1 running on pool-1-thread-2
Task 2 running on pool-1-thread-3
Task 3 running on pool-1-thread-1
Task 4 running on pool-1-thread-2
```

#### 🔹 Explanation:
- A **pool of 3 threads** is created.
- The first **3 tasks** run in parallel on **separate threads**.
- Remaining tasks reuse the available threads.

### ✅ Example: Scheduled Executor (Task Runs Every 2 Seconds)
```kotlin
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val scheduler = Executors.newScheduledThreadPool(1)

scheduler.scheduleAtFixedRate({
    println("Task running at: ${System.currentTimeMillis()}")
}, 0, 2, TimeUnit.SECONDS)
```

#### 🔹 Explanation:
- The task **runs every 2 seconds**.
- `scheduleAtFixedRate()` ensures the **same interval between executions**.

### ❌ When NOT to Use Executors?
1. **Long-running background tasks** → Use **WorkManager**.
2. **Simple one-time tasks** → Use **Kotlin Coroutines** instead.
3. **UI Updates** → Executors run in the background, so you need to **use `Handler` or `MainThread` to update UI**.

### 🔹 Conclusion
✔ **Executors are better than manually managing threads.**  
✔ **Use thread pools for better performance.**  
✔ **Choose the right Executor type for your use case.**  
✔ **Prefer Kotlin Coroutines for modern async programming.**




---

# 6. Dependency Injection


## 6.1 DI and Service Locator

# Service Locator and Dependency Injection (DI) Framework

## Service Locator

- A central registry (or locator) that provides dependencies on request.
- The class asks for its dependencies from the locator. (pull model)
- Testability- Lower as classes are tightly coupled to the locator.
- Considered an anti-pattern by some because it's like a global variable

```kotlin
object ServiceLocator {
    private val userRepository = UserRepositoryImpl()

    fun provideUserRepository(): UserRepository {
        return userRepository
    }
}

class UserViewModel {
    private val repo = ServiceLocator.provideUserRepository()
}
```

Note:

- Easy to implement
- Harder to test because UserViewModel cannot be easily tested with a fake UserRepository.

---

## DI Framework

- A technique where dependencies are provided to a class (usually via constructor, field, or method).
- The framework provides dependencies to the class. (push model)
- Testability- higher as you can inject mocks/stubs in tests.
- Clean code pattern endorsed by SOLID principles

```kotlin
class UserViewModel(private val repo: UserRepository)

val viewModel = UserViewModel(FakeUserRepository()) // for test
```

---

## Which one should I use?

| Situation                                         | Recommendation                        |
|---------------------------------------------------|---------------------------------------|
| Writing a small utility app                       | Service locator *may* be OK for speed |
| Writing testable, scalable production code        | Use **Dependency Injection**          |
| Using modern Android architecture (Jetpack, MVVM) | Use **Hilt** or **Koin**              |

------ 

## Koin
- Koin is a popular dependency injection (DI) framework for Kotlin, offering a modern and lightweight solution for managing your application’s dependencies with minimal boilerplate code.
- Koin supports both DI and the Service Locator pattern, offering flexibility to developers. However, it strongly encourages the use of DI, particularly constructor injection, where dependencies are passed as constructor parameters. This approach promotes better testability and makes your code easier to reason about.
- **Koin As SL (Service Locator)**
  - **Global Context Usage:** By default, Koin provides a globally accessible component that acts like a service locator. This allows you to retrieve dependencies from a central registry using `KoinComponent` or `inject` functions.
  - **SL in Android Components:** In Android development, Koin often uses SL internally within components such as Application and Activity for ease of setup. From this point, Koin recommends DI, especially constructor injection

------ 

## Hilt


------ 

## Dagger
- Dagger is fully static,  compile time dependency injection framework
- Annotation based DI

### Core Concepts of Dagger 2
- `@Inject` annotation 
  - By marking constructors with @Inject, Dagger knows how to build them.
  ```kotlin
  class Engine @Inject constructor() {
      fun start(): String = "Engine started"
  }
  
  class Car @Inject constructor(private val engine: Engine) {
      fun drive(): String = engine.start()
  }
  ```

- `@Module` and `@Provides`
  - When You Can't Use `@Inject` on Constructor
  - Some classes like `Retrofit`, `RoomDatabase`, or 3rd-party libraries can’t be modified or don’t have `@Inject` constructors. For such cases, use `@Module`.
  - Modules are used in Components
  ```kotlin
  class Wheels(val size: Int)
  
  @Module
  class WheelsModule {
      @Provides
      fun provideWheels(): Wheels {
          return Wheels(18)
      }
  }
  
  @Component(modules = [WheelsModule::class])
  interface CarComponent {
    fun getWheels(): Wheels
  }
  ```
  
- `@Component` and how it connects everything
  - It tells Dagger where and how to inject dependencies.
  - In below example Dagger will generate code for this interface → `DaggerCarComponent`.
  ```kotlin
  @Component
  interface CarComponent {
      fun getCar(): Car
  }
  
  fun main() {
    val carComponent = DaggerCarComponent.create()
    val car = carComponent.getCar()
    println(car.drive()) // Output: Engine started
  }
  ```



### Scoping In Dagger 2
- `@Singleton` Scope
  - A special scope provided by Dagger. Ensures only one instance is created and shared.



---

# 7. Networking


## 7.1 Networking in Android

## Networking

### REST API

REST stands for Representational State Transfer. It is an architectural style for designing networked applications,
particularly web services. REST was introduced by Roy Fielding in his 2000 Ph.D. dissertation.

Key Concepts of REST
REST is based on six architectural principles:

1. Stateless
    - Each API call contains all the information needed for the server to fulfill it.
    - The server does not remember any previous interactions.
2. Client-Server Architecture
    - The client and server are separate entities that communicate via HTTP.
    - This separation improves scalability and simplifies development.
3. Uniform Interface Uses standard HTTP methods:
    - GET – Retrieve data
    - POST – Create data
    - PUT – Update data
    - DELETE – Delete data

4. Cacheable
    - Responses can be cached to improve performance.

5. Layered System
    - A REST API can be composed of multiple layers (e.g., caching, authentication) without the client knowing.

6. Code on Demand (Optional)
    - The server can send executable code (e.g., JavaScript), though rarely used in practice.

**RESTful API**

A RESTful API is an API that follows REST principles. It uses HTTP to perform CRUD operations on resources.

### REST vs RESTful

- REST is the design principle.
- RESTful API is an implementation that follows REST principles.

---

### HTTP status code

#### Success code

| HTTP code | Meaning         | 
|-----------|-----------------|
| 200       | Server success  | 
| 201       | Created         |
| 204       | No content      |
| 206       | Partial content |

#### Redirection message

| HTTP code | Meaning     | 
|-----------|-------------|
| 400       | Bad Request | 

#### Client error code

| HTTP code | Meaning            | 
|-----------|--------------------|
| 300       | Multiple Choices   | 
| 301       | Moved permanently  |
| 302       | Found              |
| 307       | Temporary ReDirect |
| 308       | Permanent ReDirect |

#### Server error response

| HTTP code | Meaning               | 
|-----------|-----------------------|
| 501       | Internal Server error | 
| 502       | Bad Gateway           |
| 502       | Service Unavailable   |
| 504       | Gateway timeout       |

---

### Retrofit

Retrofit is a type-safe HTTP client for Android and Java, developed by Square. It's commonly used in Android development
to simplify the process of making network requests and handling APIs (like REST APIs).

**Key Features of Retrofit:**

* Converts HTTP API into a Kotlin or Java interface
* Supports GET, POST, PUT, DELETE, etc.
* Handles JSON serialization (usually with converters like Gson, Moshi, or Kotlinx.serialization)
* Supports coroutines, RxJava, and callbacks
* Makes error handling and response parsing easier
  
Retrofit uses annotations to describe how HTTP requests are made and how data is sent/received. Here's a categorized list of all major Retrofit annotations, with examples and use cases:


**HTTP Method Annotations**

| Annotation | Description           | Example                 |
|------------|-----------------------|-------------------------|
| `@GET`     | GET request           | `@GET("users")`         |
| `@POST`    | POST request          | `@POST("users/create")` |
| `@PUT`     | Replace resource      | `@PUT("users/{id}")`    |
| `@PATCH`   | Partially update      | `@PATCH("users/{id}")`  |
| `@DELETE`  | Delete resource       | `@DELETE("users/{id}")` |
| `@HEAD`    | Header only (no body) | `@HEAD("users")`        |
| `@OPTIONS` | Returns HTTP options  | `@OPTIONS("users")`     |


**URL and Path Annotations**

| Annotation | Description                  | Example                              |
|------------|------------------------------|--------------------------------------|
| `@Path`    | Replace part of the URL path | `@GET("users/{id}")` → `@Path("id")` |
| `@Url`     | Pass full dynamic URL        | `@GET` + `@Url url: String`          |

**Query Annotations**

Used to append parameters to the URL.

| Annotation  | Description                   | Example                                         |
|-------------|-------------------------------|-------------------------------------------------|
| `@Query`    | Single query param            | `@GET("users") fun get(@Query("age") age: Int)` |
| `@QueryMap` | Multiple query params via map | `@QueryMap params: Map<String, String>`         |


**Form and Field Annotations**

Used with @FormUrlEncoded to send application/x-www-form-urlencoded data.

| Annotation        | Description                | Example                                 |
|-------------------|----------------------------|-----------------------------------------|
| `@FormUrlEncoded` | Marks request as form data | `@POST("login") @FormUrlEncoded`        |
| `@Field`          | Send single form field     | `@Field("username")`                    |
| `@FieldMap`       | Send form fields via a map | `@FieldMap fields: Map<String, String>` |



**Body and Multipart Annotations**

Used to send raw JSON or files.

| Annotation   | Description                                     | Example                                      |
|--------------|-------------------------------------------------|----------------------------------------------|
| `@Body`      | Send raw object (JSON/XML)                      | `@POST("login") fun login(@Body user: User)` |
| `@Multipart` | Send file or form data as `multipart/form-data` | `@Multipart @POST("upload")`                 |
| `@Part`      | Individual file/form field                      | `@Part file: MultipartBody.Part`             |
| `@PartMap`   | Send multiple parts as a map                    | `@PartMap parts: Map<String, RequestBody>`   |



**Header Annotations**

Used to send static or dynamic headers.

| Annotation   | Description                      | Example                                   |
|--------------|----------------------------------|-------------------------------------------|
| `@Header`    | Dynamic header value             | `@Header("Authorization")`                |
| `@HeaderMap` | Multiple headers via a map       | `@HeaderMap headers: Map<String, String>` |
| `@Headers`   | Static headers (can be multiple) | `@Headers("Cache-Control: no-cache")`     |


**Streaming and Encoding**

| Annotation        | Description                          | Example                         |
|-------------------|--------------------------------------|---------------------------------|
| `@Streaming`      | Don't load entire response in memory | `@Streaming @GET("file.zip")`   |
| `@FormUrlEncoded` | Used for `@Field`-based form posts   | `@FormUrlEncoded @POST("form")` |
| `@Multipart`      | Used for `@Part` file uploads        | `@Multipart @POST("upload")`    |


---

### OkHttp

---

### Interceptors

In Retrofit (via OkHttp), interceptors are components that can observe, modify, and even short-circuit HTTP requests and
responses. They act like filters or middleware for your network calls.

**Types of Interceptors:**

1. Application Interceptor (addInterceptor)
    - Runs once, before the request is sent.
    - Useful for adding headers, logging, modifying requests, etc.

2. Network Interceptor (addNetworkInterceptor)
    - Runs only if the request goes over the network (not when from cache).
    - Useful for response-based logic like caching headers.

| Use Case                 | Interceptor Type |
|--------------------------|------------------|
| Add headers (auth token) | Application      |
| Log request/response     | Application      |
| Handle offline cache     | Application      |
| Modify response cache    | Network          |

---

### Caching

Caching in Retrofit is handled through OkHttp (which Retrofit uses internally). By enabling HTTP caching in OkHttp,
Retrofit can automatically cache the responses of network requests, which can be reused for subsequent requests,
improving performance and reducing network load.

**Create a Cache Instance**

```kotlin
val cacheSize = (5 * 1024 * 1024).toLong()  // 5 MB
val httpCacheDirectory = File(context.cacheDir, "httpCache")
val cache = Cache(httpCacheDirectory, cacheSize)

```

**Add Cache to OkHttpClient**

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .cache(cache)
    .addInterceptor { chain ->
        val response = chain.proceed(chain.request())
        // Cache responses for 60 seconds
        response.newBuilder()
            .header("Cache-Control", "public, max-age=60")  // cache for 60 seconds
            .build()
    }
    .build()

```

**Handle Offline Cache (Optional)**

You can also create a network interceptor to handle offline caching when the user is not connected to the internet. For
this, you'll need to modify the request headers to force fetching from cache if the network is unavailable.

```kotlin
val offlineInterceptor = Interceptor { chain ->
    var request = chain.request()
    if (!isNetworkAvailable()) {
        request = request.newBuilder()
            .header("Cache-Control", "public, only-if-cached, max-stale=86400")  // 1 day stale cache
            .build()
    }
    chain.proceed(request)
}

val okHttpClient = OkHttpClient.Builder()
    .cache(cache)
    .addInterceptor(offlineInterceptor)
    .addInterceptor(networkInterceptor)  // To handle network responses
    .build()

```

**Note:**

For effective caching, your server needs to send appropriate Cache-Control and ETag headers. Common cache headers
include:

Cache-Control: Directives such as max-age, no-cache, public, private, etc.

ETag: A tag that helps to determine if the response has changed since the last request.

Last-Modified: Timestamp when the resource was last modified.

If the server doesn't send the right caching headers, the caching mechanism will not work efficiently.

---

### Multi-part requests

A multipart request is an HTTP request that can upload files (images, videos, documents) along with form data (like text fields) using the multipart/form-data content type. This is commonly used for:

Uploading a profile picture with user info

Sending logs/files to a server

**When to Use @Multipart in Retrofit?**

Use @Multipart when you want to send:

   - One or more files
   - Text + files together


**API Interface**
```kotlin
interface ApiService {
    @Multipart
    @POST("upload/multiple")
    fun uploadFiles(
        @Part files: List<MultipartBody.Part>
    ): Call<ResponseBody>
}
```

**Create the List of Files**
```kotlin
val file1 = File("path_to_file1.jpg")
val file2 = File("path_to_file2.jpg")
val file3 = File("path_to_file3.jpg")
val file4 = File("path_to_file4.jpg")

fun createPartFromFile(file: File, fieldName: String): MultipartBody.Part {
    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(fieldName, file.name, requestFile)
}

val files = listOf(
    createPartFromFile(file1, "files"),
    createPartFromFile(file2, "files"),
    createPartFromFile(file3, "files"),
    createPartFromFile(file4, "files")
)
```

---

### Socket connection

WebSockets are a protocol that enables full-duplex communication between a client (like an Android app) and a server
over a single, long-lived TCP connection.

Unlike HTTP, where the client must initiate every request, WebSockets allow both client and server to send messages
independently at any time.

**Use Cases of WebSockets in Mobile Apps**

1. Real-Time Chat Applications
    - Apps like WhatsApp, Messenger, Slack use WebSockets.
    - Enables instant message delivery and reception without polling the server.

2. Live Notifications
    - Push notifications for new emails, alerts, or app updates.
    - Faster than polling and more efficient.

3. Real-Time Collaboration
    - Apps like Google Docs or whiteboard apps.
    - Multiple users edit content together and see updates live.

4. Live Gaming / Multiplayer Games
    - Synchronize player actions instantly.
    - Game state updates and position tracking in real time.

5. Live Sports Scores / Stock Ticker
    - Real-time updates without reloading the UI.
    - Common in trading apps or sports tracking dashboards.

6. IoT and Device Control
    - Smart home devices, cameras, or sensors sending and receiving updates instantly.

7. Real-Time Location Sharing
    - Uber-like apps, where drivers and passengers share locations in real-time.

| Feature           | REST API                          | WebSocket                          |
|-------------------|-----------------------------------|------------------------------------|
| Direction         | One-way (client → server)         | Two-way (client ⇄ server)          |
| Real-Time Updates | ❌ Needs polling or long-polling   | ✅ Instant                          |
| Overhead          | High (each call = new connection) | Low (single persistent connection) |
| Ideal For         | CRUD, short-lived interactions    | Continuous data exchange           |

----

### Server Side Event (SSE)

SSE (Server-Sent Events) is a web technology that allows servers to send real-time updates to the client (browser) over
a single HTTP connection. It is commonly used for applications that need to display live information, such as news
feeds, stock market updates, or live chat messages.

**Key Features of SSE:**

- Unidirectional Communication: The data flows from the server to the client only, unlike WebSockets, which allow
  bidirectional communication.
- Built on HTTP: SSE uses the HTTP protocol, making it easier to implement than WebSockets in environments where only
  HTTP is supported.
- Automatic Reconnection: If the connection drops, the browser will attempt to reconnect automatically.
- Event Stream: The server sends a continuous stream of data in a specific format, typically in JSON or plain text.

In the SSE connection is kept live using

1. Long-Lived HTTP Connection:
    - When an SSE connection is established, the client sends a standard HTTP GET request to the server, just like any
      other HTTP request.
    - The server responds with a text/event-stream content type, and the connection is kept open.
    - Instead of sending a single response and closing the connection (like in typical HTTP responses), the server keeps
      the connection open and continuously sends updates in the form of event data.
    - The client is expected to keep reading the data as long as the server is sending it.
2. HTTP Keep-Alive Header:
    - The HTTP/1.1 protocol uses the Connection: keep-alive header to indicate that the server should keep the
      connection open.
    - In the case of SSE, this header is typically sent by the server to ensure the connection is maintained open until
      the server decides to close it or the client disconnects.
3. Heartbeats
    - To ensure that the connection is still alive and prevent it from being closed by intermediate proxies or load
      balancers (which may close idle connections), the server can send heartbeat messages. These are usually just a
      comment or an empty message sent at regular intervals (e.g., every 30 seconds). The client can ignore these
      heartbeat messages.

Http header example from server

```text
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

---

### Auth refresh tokens

An auth refresh token is a key concept in modern authentication systems that helps maintain a secure and seamless user session without requiring the user to log in repeatedly.

- Access Token: Short-lived token (e.g., valid for 15 minutes) used to authenticate API requests.
- Refresh Token: Long-lived token (e.g., valid for days/weeks) used to obtain a new access token when it expires.


#### How Refresh Token Works
1. Login 
   - User logs in with username/password (or OAuth). 
   - Server responds with:
     - Access Token (short-lived)
     - Refresh Token (long-lived)

2. Accessing API
   - The app uses the access token to access protected resources. 
   - Once the access token expires, API calls fail with a 401 Unauthorized.

3. Token Refresh
   - The app sends the refresh token to the server (usually a /refresh-token endpoint). 
   - If the refresh token is valid, the server responds with a new access token (and optionally a new refresh token). 
   - The app resumes API calls with the new access token.

4. Logout or Expiry
   - If the refresh token is invalid (expired, revoked, or tampered), the user must log in again.

#### Why Use Refresh Tokens?

| Feature            | Benefit                                 |
|--------------------|-----------------------------------------|
| Short access token | Limits risk if a token is leaked        |
| Long refresh token | Improves user experience (fewer logins) |
| Decouples access   | No need to store long-lived credentials |
| Revokable          | Can blacklist refresh tokens on logout  |


#### Security Best Practices
- Store access token in memory (not localStorage). 
- Store refresh token securely (e.g., in HttpOnly cookie). 
- Rotate refresh tokens on each use (prevents reuse). 
- Invalidate refresh token on logout or suspicious activity.


```text
Client ---------------------> Server
       (Login credentials)
            <-------------------------
     Access Token + Refresh Token

Client ------ [Bearer access_token] ---> API
API responds <---------------------------

After expiry:
Client ---- [refresh_token] ---> /refresh-token
            <----------------------- New tokens

```


---

# 8. Android Security


## 8.1 Android Keystore System

# Android Keystore System

- Generating and storing keys securely
- Hardware-backed keystore (StrongBox)
- Usage for cryptography (RSA, AES, HMAC)


**KeystoreCryptoUtil.kt**
```kotlin

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

object KeystoreCryptoUtil {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "MySecureKeyAlias"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12 // GCM recommended IV size
    private const val TAG_SIZE = 128

    // Step 1: Generate key if not present
    fun generateKeyIfNecessary() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keySpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        }
    }

    // Step 2: Get the secret key from Keystore
    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    // Step 3: Encrypt data
    fun encryptData(plainText: String): String {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Combine IV + ciphertext and encode
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    // Step 4: Decrypt data
    fun decryptData(encryptedData: String): String {
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)
        val iv = combined.copyOfRange(0, IV_SIZE)
        val encryptedBytes = combined.copyOfRange(IV_SIZE, combined.size)

        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}

```

## 8.2 Encrypted Shared Preferences

# EncryptedSharedPreferences

A wrapper over SharedPreferences that encrypts keys and values using keys from Android Keystore.

- Storing passwords, tokens, API keys
- Flags for login status, etc.
- Uses AES encryption under the hood 
- Encrypts both keys and values 
- Stores data in standard SharedPreferences file 
- Encrypts with a master key stored in Android Keystore




**Dependency:**

```xml
implementation "androidx.security:security-crypto:1.1.0-alpha03"
```


**Sample code**

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

encryptedPrefs.edit().putString("password", "password_to_store").apply()

```


## Note

**API Level Support**

| Feature	                    | Minimum API Level                                 |
|-----------------------------|---------------------------------------------------|
| EncryptedSharedPreferences	 | API 23 (Android 6.0)                              |
| MasterKey	                  | API 23 (uses Android Keystore internally)         |
| StrongBox-backed keys	      | API 28+ (Android 9.0) — optional hardware support |

---

# 9. OOP Concepts


## 9.1 Classes & Objects

# Class - Object


## Abstraction
- Talks obout general thing and avoid the specific case


## Encapsulation
- Encapsulation refers to integrating data (variables) and code (methods) into a single unit. 
- In encapsulation, a class's variables are hidden from other classes and can only be accessed by the methods of the class in which they are found
- Is used to limit the access to variable inside the 
- User of object should not know how it internally works
- Modifier are used to limit the access


## Inheritance


## Polymorphism
 - Having many form
 - Types of polymorphism
   - Dynamic polymorphism
     - Method overriding
   - Compile time polymorphism
     - Method overloading - same name but different set of parameter

---

## Function vs Method

| Function                                                              | Method                                                       |
|-----------------------------------------------------------------------|--------------------------------------------------------------|
| Function is set of instruction or procedures to perform specific task | Method is set of instruction that are associated with object |
| Function is independent functionality                                 | Method lies in object}                                       |
| Function can work with provided data                                  | Method can access all the data provided in given class       |




---

## Parameter vs Argument

| Parameter                                                              | Argument                                               |
|------------------------------------------------------------------------|--------------------------------------------------------|
| Parameter are the variable that can be defined in function declaration | Arguments are the variable given to function execution |
| Referred as the formal parameters                                      | Referred as actual parameter                           |

## 9.2 OOP Principles

# Object-oriented principal

## Clas and object
- Class is blue-print or template that defines properties and behaviour
- Object is instance of class , created using class definition

## Encapsulation
- Encapsulates data and functions together as single unit.
- Protects the data from unauthorized access and modification.
- Provides a public interface for interaction.
- hiding the internal state and requiring all interactions to be performed through an object's methods.

```kotlin
class Car(private var speed: Int) {

    fun accelerate(amount: Int) {
        speed += amount
    }

    fun brake(amount: Int) {
        speed -= amount
    }

    fun getSpeed(): Int {
        return speed
    }
}
```
## Abstraction
- Exposes only relevant data and methods.
- Focuses on what an object does rather than how it does it.
-  simplifying complex reality by modeling classes based on the essential properties and behaviors of the objects
```kotlin
abstract class Car(val name: String, val year: Int) {
    abstract fun start()
    abstract fun stop()
}

class Sedan(name: String, year: Int) : Car(name, year) {
    override fun start() {
        println("$name - $year Sedan starts.")
    }

    override fun stop() {
        println("$name - $year Sedan stops.")
    }
}

class SUV(name: String, year: Int) : Car(name, year) {
    override fun start() {
        println("$name - $year SUV starts.")
    }

    override fun stop() {
        println("$name - $year SUV stops.")
    }
}
```

## Inheritance
- Mechanism that allows the class to inherit  properties and methods from another class
- Provide way to create class from another class
- It promotes code reuse
```kotlin
open class Vehicle(val brand: String, val model: String, val year: Int) {
    fun accelerate() {
        println("$brand $model is accelerating.")
    }

    fun brake() {
        println("$brand $model is braking.")
    }
}

class Car(brand: String, model: String, year: Int) : Vehicle(brand, model, year) {
    fun park() {
        println("$brand $model is parked.")
    }
}

class Truck(brand: String, model: String, year: Int, val payloadCapacity: Int) : Vehicle(brand, model, year) {
    fun loadCargo() {
        println("$brand $model is loading cargo with capacity $payloadCapacity kg.")
    }
}

```

## Polymorphism
- Ability to take multiple forms
- Types of polymorphism
  1. Dynamic polymorphism
     - Method overriding
  2. Static polymorphism
     - Method overloading
     - Operating overloading
```kotlin
open class Shape {
    open fun draw() {
        println("Drawing a shape")
    }
}

class Circle : Shape() {
    override fun draw() { // Method overriding 
        println("Drawing a circle")
    }
    
    fun draw(radius: Double) { // Method over loading same method name in same class
        println("Drawing a circle with radius $radius")
    }
}
```

---

# 10. SOLID Principles


## 10.1 Single Responsibility Principle

# Single Responsibility Principal 

A class should have only one reason to change. In other words, each class should do one thing and do it well.


### Bad Code example

```kotlin
class UserManager {
    fun createUser(user: User) {
        // create user in database
    }
    
    fun updateUser(user: User) {
        // update user
    }

    fun sendEmail(user: User) {
        // send email to user
    }
}
```

In above example, the UserManager class is responsible for  creating , updating user in the database and sending a welcome email to the user. This violates the SRP because the class has more than one reason to change.


### Good Code Example

```kotlin
class UserDatabaseService {
    fun createUser(user: User?) {
        // create user in database
    }
    
    fun updateUser(user: User) {
        // update user
    }
}


class EmailService {
    fun sendWelcomeEmail(user: User?) {
        // send welcome email to user
    }
}


class UserManager {
    private val userDatabase: UserDatabaseService? = null
    private val emailService: EmailService? = null
    fun createUser(user: User?) {
        userDatabase!!.createUser(user)
        emailService!!.sendWelcomeEmail(user)
    }
}
```

In the improved code, the responsibilities of creating, updating a user and sending a welcome email have been separated into two separate classes (UserDatabase and EmailService). The UserManager class now delegates those responsibilities to the appropriate classes


>Single Responsibility principal does not mean Class should have single method. It can have multiple method as long as it adheres to single common responsibility in Above case UserDataBase 



## 10.2 Open/Closed Principle

# Open Closed Principal

Software entities (classes, modules, functions, etc.) should be open for extension but closed for modification. This means that you should be able to add new functionality without changing existing code.


### Bad Code Example

```agsl
class Shape(private val type: String) {
    fun area(): Double {
        if (type == "rectangle") {
            // calculate area of rectangle
        } else if (type == "circle") {
            // calculate area of circle
        }
        // more if/else statements for other types of shapes
    }
}
```

In above example, the Shape class is not closed for modification because if a new type of shape is added, the area() method will need to be modified.


### Good Code Example


```agsl
abstract class Shape {
    abstract fun area(): Double
}

class Rectangle(private val width: Double, private val height: Double) : Shape() {
    override fun area(): Double {
        return width * height
    }
}

class Circle(private val radius: Double) : Shape() {
    override fun area(): Double {
        return Math.PI * radius * radius
    }
}

```


In the improved code, the Shape class has been made abstract and an abstract `area()` method has been added. Concrete classes for each type of shape have been created, and each class implements the `area()` method. This allows new types of shapes to be added without modifying existing code.

## 10.3 Liskov Substitution Principle

# Liskov Substitution Principal

Subtypes must be substitutable for their base types. In simpler terms, any instance of a parent class should be able to be replaced by an instance of one of its child classes without affecting the correctness of the program.


### Bad Code Example

```kotlin
open class Bird {
    open fun fly() {
        println("Bird is flying")
    }
}

class Ostrich : Bird() {
    override fun fly() {
        // ❌ Ostriches can't fly — this breaks expected behavior
        throw UnsupportedOperationException("Ostrich can't fly")
    }
}

fun makeBirdFly(bird: Bird) {
    bird.fly()
}

```

- Ostrich is a Bird, but it can’t fly.
- Calling makeBirdFly(Ostrich()) will crash — it violates the expectations of the base type.

### Good Code Example

```kotlin
interface Bird {
    fun eat()
}

interface FlyingBird : Bird {
    fun fly()
}

class Sparrow : FlyingBird {
    override fun eat() {
        println("Sparrow eating")
    }

    override fun fly() {
        println("Sparrow flying")
    }
}

class Ostrich : Bird {
    override fun eat() {
        println("Ostrich eating")
    }
}

fun makeBirdFly(bird: FlyingBird) {
    bird.fly()
}

```


- Sparrow can fly.
- Ostrich doesn't implement fly() — so can't be passed to makeBirdFly(). 
- LSP is respected.

## 10.4 Interface Segregation Principle

# Interface Segregation Principal


Clients should not be forced to depend on interfaces they do not use. This means that you should break up interfaces into smaller, more focused interfaces so that clients only need to implement the methods they care about.


### Bad Code Example

```kotlin
interface Worker {
    fun work()
    fun eat()
}

class HumanWorker : Worker {
    override fun work() {
        println("Human working")
    }

    override fun eat() {
        println("Human eating")
    }
}

class RobotWorker : Worker {
    override fun work() {
        println("Robot working")
    }

    override fun eat() {
        // ❌ Robot doesn’t eat — forced to implement irrelevant method
        throw UnsupportedOperationException("Robot doesn't eat")
    }
}

```
RobotWorker doesn’t need eat() — but it's forced to implement it, which violates ISP.



### Good Code Example

```kotlin
interface Workable {
    fun work()
}

interface Eatable {
    fun eat()
}

class HumanWorker : Workable, Eatable {
    override fun work() {
        println("Human working")
    }

    override fun eat() {
        println("Human eating")
    }
}

class RobotWorker : Workable {
    override fun work() {
        println("Robot working")
    }
}

```

- RobotWorker only implements what it needs.
- Code is clean, extendable, and respects Interface Segregation.

## 10.5 Dependency Inversion Principle

# Dependency Inversion Principal

High-level modules should not depend on low-level modules. Both should depend on abstractions. This means that you should depend on abstractions (interfaces, abstract classes, etc.) rather than concrete implementations, which allows for greater flexibility and easier testing.



### Bad Code

```agsl
class DatabaseService {
    fun connect() { ... }
    fun disconnect() { ... }
    fun executeQuery(query: String): List<Any> { ... }
}

class UserService {
    private val db = DatabaseService()

    fun getUsers(): List<User> {
        val query = "SELECT * FROM users"
        val results = db.executeQuery(query)
        return results.map { row -> User(row) }
    }
}

```

In above example, the `UserService` depends on the `DatabaseService` implementation. If we want to switch to a different database implementation, we would need to modify the `UserService` class, which violates the **DIP**.

### Good Code Example

```agsl
interface Database {
    fun connect()
    fun disconnect()
    fun executeQuery(query: String): List<Any>
}

class DatabaseService : Database {
    override fun connect() { ... }
    override fun disconnect() { ... }
    override fun executeQuery(query: String): List<Any> { ... }
}

class UserService(private val db: Database) {
    fun getUsers(): List<User> {
        val query = "SELECT * FROM users"
        val results = db.executeQuery(query)
        return results.map { row -> User(row) }
    }
}

```

---

# 11. Design Patterns

## 11.1 Creational Patterns

### 11.1.1 Singleton Pattern

# Singleton

**Singleton is creation design pattern. It ensures that you have only one instance.It is widely used design pattern.**


### Steps to create Singleton
1. Create private constructor for the class
2. Create static method for that will call the private constructor and save that in static instance. Whenever in feature we call static method it should return the previously cached(stored) object.


### Cons
1. Singleton has same pros and cons as global variables. It breaks modularity and Single Responsibility Principle.
2. Most of limitation comes with while creating the unit test

### UML

[PlantUML for Singleton pattern](https://www.plantuml.com/plantuml/uml/TOwnKe8n48JxFCMMEX8VWB6WfTONE9E3PBnScEHIAFBkvaS6i70cnMPdzzlRTADceuXxpReNjgfu-VOUrwzpGQZtct5q0E1nd5NBuOIiKsdWYtyDFZoUdIVisRQNpFHEpcarXl3EK7Ut_7fzHtfBkMC-mkyC2fTtRi-EDMBf_q_vEmTi5OGonBAtr9WYHz3X83bCSDKLRDagRAd6rSptL8T467qm0ZLUyFrJGsfLyzUvStV5PifHv0S0)

![Alt text](http://www.plantuml.com/plantuml/png/TOsnKiKW44LxlkAMEeeVyCRYjQdr1v1i4aPOCh0fDERVtIIEQF4OYkLozjnh5LTaIT6y11uZIMOyVrcBtwSnFhfQMx0QwB5OTLM2tn1O9_PqvWsykp-I1umatG-ZsMOCPo546WHvQFpbyiVqEeKAlGvd3HAbDofFXhZ7ld_RyhyEw5WjQOaIh4o5N4yzt3p7u5MS-L4HSUy4xmhjglh1VYgPm-VntnglEYqfVWC0 "a title")

----

### Java code

```
public final class Singleton {
    private static volatile Singleton instance;

    public static Singleton getInstance() {

        /**
         * This is DCL method more details at https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
         */
        Singleton result = instance;
        if (result != null) {
            return result;
        }

        synchronized (Singleton.class) {
            if (instance == null) {
                instance = new Singleton();
            }
            return instance;
        }
    }
}
```

#### Singleton - Java Implementation

```java
public final class Singleton {
    private static volatile Singleton instance;

    public static Singleton getInstance() {

        /**
         * This is DCL method more details at https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
         */
        Singleton result = instance;
        if (result != null) {
            return result;
        }

        synchronized (Singleton.class) {
            if (instance == null) {
                instance = new Singleton();
            }
            return instance;
        }
    }

    public static void main() {

    }
}

 class DemoMultiThread {
    public static void main(String[] args) {
        Thread threadFoo = new Thread(new Thread1());
        Thread threadBar = new Thread(new Thread2());
        threadFoo.start();
        threadBar.start();
    }

    static class Thread1 implements Runnable {
        @Override
        public void run() {
            Singleton singleton = Singleton.getInstance();
            System.out.println(singleton);
        }
    }

    static class Thread2 implements Runnable {
        @Override
        public void run() {
            Singleton singleton = Singleton.getInstance();
            System.out.println(singleton);
        }
    }
}
```


### 11.1.2 Factory Method Pattern

# Factory method

1. **Factory is Creational design pattern. It uses factory method to deal object creation without having to specify the actual class of the object that will be created.**
2. **A Factory Pattern or Factory Method Pattern says that just define an interface or abstract class for creating an object but let the subclasses decide which class to instantiate.**
3. **The Factory Method Pattern is also known as  Virtual Constructor.**
4. **Adheres to Open closed principle.**
5. **It heavily relies on inheritance**




### Steps to create factory method 

1. Declare interface(it can be abstract class or interface) as **Product**
2. Make all concrete product follows the same interface **(Product)**
3. Add empty factory method inside the creator class that returns type of Product
4. Implement concrete creator for each product type
5. Added factory class to create object based on parameter to function


### Pros
1. Single Responsibility Principle. Makes it easy to extend product construction code independently from rest of application
2. Open/Closed Principle. Allows adding new product without breaking the existing code



### Cons
1. The code may become more complicated since you need to introduce a lot of new subclasses to implement the pattern


### UML

[PlantUML for Factory Pattern](http://www.plantuml.com/plantuml/uml/fPA_JiGm3CPtFyKtmd2mLrLrqaqCI0oyG9gwj2X_Jeax8E3T2Rcqfmj_bfCwM9_zsJ-xsKTHGHPhw2duOT62zcEtmy4x7LYuY7G8hCI7osGHYIe1F_nea7CSqH7Hx8J3g3IZvDjsaxhkdN0yoOyianzkRkapcRHHCTQDVwdDnQgF4fGMRE3jAkK1QL9It_uPK8nLxJR0UM64-MKI-B6cQfeBA0AUfpaYVJ8nPdiqRDc9ajcxPC_gHcop1sFmbh0U2Djzxi-L9s2Pb22mBC55j7rN_pfs0xvf2xFXCIDNUpPDlUOLqANdtz04FAImF0dba3ABCl3zAsjPkvSZtfrNsATeiUOB)

![Alt text](http://www.plantuml.com/plantuml/png/fPA_JiGm3CPtFyKtmd2mLrLrqaqCI0oyG9gwj2X_Jeax8E3T2Rcqfmj_bfCwM9_zsJ-xsKTHGHPhw2duOT62zcEtmy4x7LYuY7G8hCI7osGHYIe1F_nea7CSqH7Hx8J3g3IZvDjsaxhkdN0yoOyianzkRkapcRHHCTQDVwdDnQgF4fGMRE3jAkK1QL9It_uPK8nLxJR0UM64-MKI-B6cQfeBA0AUfpaYVJ8nPdiqRDc9ajcxPC_gHcop1sFmbh0U2Djzxi-L9s2Pb22mBC55j7rN_pfs0xvf2xFXCIDNUpPDlUOLqANdtz04FAImF0dba3ABCl3zAsjPkvSZtfrNsATeiUOB "a title")


----


### Kotlin code


Creator interface

```agsl
interface PlanCreator {
    fun createPlan(): Plan
}

```
Concrete creators
```agsl
class DomesticPlanCreator : PlanCreator {
    override fun createPlan() = DomesticPlan()
}

class CommercialPlanCreator : PlanCreator {
    override fun createPlan() = CommercialPlan()
}

class InstitutionalPlanCreator : PlanCreator {
    override fun createPlan() = InstitutionalPlan()
}
```


Product abstract class

```agsl
abstract class Plan {
    private val baseRate = 100f
    fun getPlanDetails(): Float {
        return baseRate + getRate()
    }

    abstract fun getRate(): Float
}

```

Concrete Product

```agsl
class DomesticPlan : Plan() {
    override fun getRate(): Float {
        return 200f
    }
}

class CommercialPlan : Plan() {
    override fun getRate(): Float {
        return 400f
    }
}

class InstitutionalPlan : Plan() {
    override fun getRate(): Float {
        return 600f
    }
}
```

Factory Utility

```agsl
class ByTypePlanFactory : PlanFactory() {
    override fun getPlan(plan: PlansEnum): Plan? {
        return when (plan) {
            PlansEnum.Domestic -> DomesticPlanCreator().createPlan()
            PlansEnum.Commercial -> CommercialPlanCreator().createPlan()
            PlansEnum.Institutional -> InstitutionalPlanCreator().createPlan()
            else -> null
        }

    }

}
```

Client code

```agsl
    val planFactory = ByTypePlanFactory()
    val institutionalPlan = planFactory.getPlan(PlanFactory.PlansEnum.Institutional)
    val commercialPlan = planFactory.getPlan(PlanFactory.PlansEnum.Commercial)
    val domesticPlan = planFactory.getPlan(PlanFactory.PlansEnum.Domestic)
    println()
    println("Institutional Plan detail = ${institutionalPlan?.getPlanDetails()}")
    println("Commercial Plan detail = ${commercialPlan?.getPlanDetails()}")
    println("Domestic Plan detail = ${domesticPlan?.getPlanDetails()}")
```


### 11.1.3 Abstract Factory Pattern

# Abstract Factory

- **Abstract Factory is a creational design pattern that lets you produce families of related objects without specifying their concrete classes.**




### Steps to create factory method 

1. **Abstract Factory** : Defines an interface of operations to create abstract objects
2. **Concrete Factory** : Implement the operation to create the concrete objects
3. **Abstract Product** : Define an interface for the specific type of object
4. **Concrete Object**  : Class definition of the object to be created by the Concrete Factory
5. **Client**: Class that uses interface defined in abstract factory


### Pros
1. You can be sure that the products you’re getting from a factory are compatible with each other. 
2. Single Responsibility Principle. Makes it easy to extend product construction code independently from rest of application
3. Open/Closed Principle. Allows adding new product without breaking the existing code

### Cons
1. The code may become more complicated since you need to introduce a lot of new subclasses to implement the pattern


### UML

[PlantUML for Factory Pattern](http://www.plantuml.com/plantuml/uml/fPCnJyCm48Lt_mgFC7Y8HrHPjIinzoyOzqeZ1GUThmEY_7SCf2Qnhn4IOr_VU_VT9TacyaZdbq7inXWG2FOu7iw1N2ULkXXv_IaVP1zFW3qbKRJDHIbP1_NSMceVVQ0HtwPM-vG8ipAfyqJzrsB6978Z2e1Tabk6tBBaTbdBiGzm9CNitENa3i4J9Fdweyai6gO2--SJrs6vZYoQXaFVvj1CHnPDKx72mNNOhmuX-aY0Hnz0_XvoKqHZtiKrOU_knKTEXC6drAxQ67cVRdmpXlebQb32znBxagyKnqmkp-b_SVtdU7DHT-kYRksYhckCl4_NlYt4G_xyFm40)

![Alt text](http://www.plantuml.com/plantuml/png/fPCnJyCm48Lt_mgFC7Y8HrHPjIinzoyOzqeZ1GUThmEY_7SCf2Qnhn4IOr_VU_VT9TacyaZdbq7inXWG2FOu7iw1N2ULkXXv_IaVP1zFW3qbKRJDHIbP1_NSMceVVQ0HtwPM-vG8ipAfyqJzrsB6978Z2e1Tabk6tBBaTbdBiGzm9CNitENa3i4J9Fdweyai6gO2--SJrs6vZYoQXaFVvj1CHnPDKx72mNNOhmuX-aY0Hnz0_XvoKqHZtiKrOU_knKTEXC6drAxQ67cVRdmpXlebQb32znBxagyKnqmkp-b_SVtdU7DHT-kYRksYhckCl4_NlYt4G_xyFm40 "a title")

---

### Kotlin code

Product 1
```agsl
/***
 * Product 1 and its concrete implementation
 */
interface Refill {
    fun color(): String
}

class BlueRefill : Refill {
    override fun color(): String {
        return "Blue"
    }

}

class BlackRefill : Refill {
    override fun color(): String {
        return "Black"
    }

}
```

Product 2

```agsl
/***
 * Product 2 and its concrete implementation
 */
interface Body {
    fun metal(): String
}

class GelPenBody : Body {
    override fun metal(): String {
        return "Steel"
    }

}

class BallPenBody : Body {
    override fun metal(): String {
        return "Plastic"
    }

}
```

Abstract factory
```agsl
***
 * Abstract factory and its concrete implementations
 */
interface PenFactory {
    fun getBody(): Body
    fun getRefill(): Refill
}

class BlueGelPenFactory : PenFactory {
    override fun getBody(): Body {
        return GelPenBody()
    }

    override fun getRefill(): Refill {
        return BlueRefill()
    }

}

class BlackBallPenFactory : PenFactory {
    override fun getBody(): Body {
        return GelPenBody()
    }

    override fun getRefill(): Refill {
        return BlackRefill()
    }

}
```
Client code

```agsl
fun main() {

    val type: Pen.PenType = Pen.PenType.Ball

    val penFactory: PenFactory = if (type == Pen.PenType.Ball) {
        BlackBallPenFactory()
    } else {
        BlueGelPenFactory()
    }

    Pen().write(penFactory)

}


class Pen {
    enum class PenType {
        Ball, Gel
    }

    fun write(factory: PenFactory) {
        print("Writing using˳${factory.getRefill().color()} ink and ${factory.getBody().metal()} body")
    }

}
```


### 11.1.4 Builder Pattern

# Builder

1. **Builder is creational design pattern**
2. **It allows to create complex object step by step**
3. **This pattern allows to create the different object from same construction code**


### Steps to create builder pattern

1. Declare interface(it can be abstract class or interface) as **Builder**
2. Make concrete builder follows the Builder
3. Define product 
4. Client



### Pros
1. You can create object step by step
2. You can reuse the same code construction code for various representation of product



### Cons
1. Overall complexity increases as it needs extra classes




### UML

[PlantUML for Factory Pattern](http://www.plantuml.com/plantuml/uml/TP312eCm44Jl-nLxr4C2hJSHaVe3fNyGuhOLqWZPpKdzzwOqpK7ePPYyC3Em0ui94byEBd5s4mNiDgrnNBmD99GXm3KiausIVfN2J5jy6aO3CBgPlA1IMtzCjXYP663sGk5kBFt2NLTGtw_WJrKD_loH9ic3v4OSdLHrYtaRTW2mpZ0Naf-7pM_RghNUKsLnJNR_oWVoKph46m00)

![Alt text](http://www.plantuml.com/plantuml/png/TP312eCm44Jl-nLxr4C2hJSHaVe3fNyGuhOLqWZPpKdzzwOqpK7ePPYyC3Em0ui94byEBd5s4mNiDgrnNBmD99GXm3KiausIVfN2J5jy6aO3CBgPlA1IMtzCjXYP663sGk5kBFt2NLTGtw_WJrKD_loH9ic3v4OSdLHrYtaRTW2mpZ0Naf-7pM_RghNUKsLnJNR_oWVoKph46m00)









### kotlin code

Builder interface
```agsl
interface HouseBuilder {
    fun buildWalls(numWalls: Int)
    fun buildDoor(numDoors: Int)
    fun buildWindows(numWindows: Int)
    fun buildGarden()
    fun buildGarage()
    fun buildSteps(numSteps: Int)
    fun getResult(): House
}
```


Concrete builder

```agsl
class MyHouseBuilder : HouseBuilder {

    private var walls = 0
    private var doors = 0
    private var windows = 0
    private var garden = false
    private var garage = false
    private var steps = 0
    override fun buildWalls(numWalls: Int) {
        this.walls = numWalls
    }

    override fun buildDoor(numDoors: Int) {
        this.doors = numDoors
    }

    override fun buildWindows(numWindows: Int) {
        this.windows = numWindows
    }

    override fun buildGarden() {
        this.garden = true
    }

    override fun buildGarage() {
        this.garage = true
    }

    override fun buildSteps(numSteps: Int) {
        this.steps = numSteps
    }

    override fun getResult(): House {
        return House(walls, doors, windows, garden, garage, steps)
    }


}
```

Product 

```agsl
data class House(
    private val walls: Int = 0,
    private val doors: Int = 0,
    private val windows: Int = 0,
    private val garden: Boolean = false,
    private val garage: Boolean = false,
    private val steps: Int = 0
)
```

client

```agsl
    val gardenHouseBuilder = MyHouseBuilder().apply {
        this.buildDoor(2)
        this.buildGarden()
        this.buildWalls(4)
        this.buildSteps(9)
        this.buildWindows(4)
    }


    val gardenHouse = gardenHouseBuilder.getResult()
    println("Garden House  = $gardenHouse")

    val gardenWithGarageHouseBuilder = MyHouseBuilder().apply {
        this.buildDoor(2)
        this.buildGarden()
        this.buildGarage()
        this.buildWalls(4)
        this.buildSteps(25)
        this.buildWindows(4)
    }


    val gardenWithGardenHouse = gardenWithGarageHouseBuilder.getResult()
    println("Garden with garage House  = $gardenWithGardenHouse")
```

### 11.1.5 Prototype Pattern

# Prototype

1. **Prototype is Creational design pattern**
2. **It lets you to copy existing object even complex once without depending on their specific classes**

### Steps to create prototype pattern

1. Add **Prototype** interface/abstract class
2. Implement **Prototype** interface/abstract to **ConcretePrototype** which clones data to new cloned object

### Pros

1. Clones object without coupling to class
2. Can get rid of repeated initializing code.

### Cons

1. It gets tricky when cloning of complex object which has circular dependency

### UML

[PlantUML for Prototype Pattern](https://www.plantuml.com/plantuml/uml/ROv1ImCn48NlyolUKIakw74ffU39cmV_G9gTTID99YJJWwpzxwuRkb4gv32yz-NZ7OkQhEVWwJMBYducnlwhwpeyIxIPbRUSo4NbF1hB-0Nlj_Hmm4oxsPoAPcFr0kibyjqz-iRJXSXwKygjVwO7Z8xzy7WzdeXs_pkYA2ExzqaXOslwViK9U9jSmV8CqZOri4uoTCHnm8-0_chQeblTYyA5v3bmr0AT66jpLQ8YLfBZMijYTT5uoEc3hKAb_gZbchKTn-Kw1_yD)

![Alt text](http://www.plantuml.com/plantuml/png/ROv1ImCn48NlyolUKIakw74ffU39cmV_G9gTTID99YJJWwpzxwuRkb4gv32yz-NZ7OkQhEVWwJMBYducnlwhwpeyIxIPbRUSo4NbF1hB-0Nlj_Hmm4oxsPoAPcFr0kibyjqz-iRJXSXwKygjVwO7Z8xzy7WzdeXs_pkYA2ExzqaXOslwViK9U9jSmV8CqZOri4uoTCHnm8-0_chQeblTYyA5v3bmr0AT66jpLQ8YLfBZMijYTT5uoEc3hKAb_gZbchKTn-Kw1_yD)

### Kotlin code

Prototype interface

```agsl
interface Duck {
    fun clone(): Duck
}

```

ConcretePrototype class

```agsl
class ToyDuck : Duck {

    var color: String = ""
    var mfDate: String = ""

    override fun clone(): Duck {
        return ToyDuck().also {
            it.color = this.color
            it.mfDate = this.mfDate
        }
    }

    override fun toString(): String {
        return "ToyDuck(color='$color', mfDate='$mfDate')"
    }
}
```

Client code

```agsl
    val toyDuck = ToyDuck()
    toyDuck.color = "red"
    toyDuck.mfDate = "12/3/2023"

    val toyDuckClone = toyDuck.clone() as ToyDuck
    toyDuckClone.color = "redCloned"

    println("toyDuck  = $toyDuck")
    println("toyDuckClone  = $toyDuckClone")
```

## 11.2 Structural Patterns

### 11.2.1 Adapter Pattern



# Adapter Pattern

* This patterns helps as a connection between two different types of interfaces. 
  * Most famous example of US and UK sockets is taken here, as the adapter is the one which helps to connect your specific US plug into UK socket(Adaptee).
    * Example, existing system(Client) might need a function from an older library(Adaptee), here with no code changes from client and adaptee, adapter helps to perform client request by converting it into the available adaptee options
    * Existing system(No code changes) --> Adapter(Medium) --> Old Code(No Code Changes) 
  
    * Usually, there are two types of Adapters

### Object Adapters
  * Adapts the Adaptee by the process of composition.
  * **Composition** :  Is basically having has-A relationship between two classes. 
    * House has-A Bathroom, Car has-A Engine
    * **Class diagram** for Object Adapter
    * ![Unable to load image](https://www.plantuml.com/plantuml/dpng/TSv12W8n38NXVKxHfIvoWrcCU0Fn0c7cAOMqr2HTgNSNeGCgkCxtVZ9xY4KlIekAUpfgHeqx9SEjmsEtfoTHhW6xo89q5hiYqWyOuyCgBO3trHkMB7hwH5_AKYvDKL33_rMttlHWtcP40q_CFDwb6NNjHLy0) 
### Class Adapters
  * Adapts the Adaptee by the process of multiple Inheritance
  * **Multiple Inheritance**: having multiple is-A relations - [Ref](MultipleInheritance.kt)
  * not applicable in JAVA
  **Class diagram** for class Adapter
  * ![Unable to load image](https://www.plantuml.com/plantuml/dpng/SoWkIImgAStDuKhEIImkLd3EoKpDAwdcKYXABInDBIxHqEIgvOBAXIGMfQUMA62NT4n9B2X9JGN95XUa9cScvWGXAK9LLQIGMb6IcfU2qqYOOJOrkhemFLnSKCKska11Y3kv782c01qF0000)
  
More info on Composition vs Inheritance -  [Ref](https://www.adservio.fr/post/composition-vs-inheritance#el1)

#### Adapter - Kotlin Implementation

```kotlin
/**
 * Real world Adapters
 * Iterators and Enumerators
 * Iterators are more recent part of Collections
 *   - hasNext() - checks if any more elements are present
 *   - next() - get's the next element
 *   - remove() - removes the current element
 * Enumerators are older interfaces allowing
 *  - hasMoreElements() - checks- if any more elements are present
 *  - nextElement() - returns next element from collection
 *
 *
 *  For both these interfaces having been implemented in two different versions of Java,
 *  Compatibility based on the requirement can be made possible with Adapters
 */

```


### 11.2.2 Bridge Pattern




### 11.2.3 Facade Pattern



## 11.3 Behavioral Patterns

### 11.3.1 Observer Pattern

# Observer Pattern

- **Is a Behavioral design pattern that allows objects to be notified of changes to the state of another object, known as the subject. In Kotlin, the Observer pattern can be implemented using a combination of interfaces and classes.**

### Type of observer 
1. **Push model:**
<br>In the push model, the subject sends the update directly to the observers, without requiring the observer to request it. This means that the subject "pushes" the update to the observers, hence the name.

2. **Pull model:**
<br>In the pull model, the observers request the update from the subject when they need it. This means that the observer "pulls" the update from the subject, hence the name.


### Steps to create observer method

1. **Observer** : Defines an updating interface for objects that should be notified ofchanges
   in a subject.
2. **Concrete Observer** : implements the Observer updating interface to keep its state consistent
   with the subject's
3. **Subject** : knows its observers. An y number o fObserver objects ma y observe a subject.



### UML

[PlantUML for Strategy Pattern](http://www.plantuml.com/plantuml/uml/XT31Ii0m383XUvuYn-smFi0ePU1LyE0JT5scbQs5D1j8tRlhS9c81s_bxnTeCnJnBDkwmDDOY7TYj1_6u4DEWKb8Apnluni5mOxZowjq3lMcntoT2W24nPU25ww07UwUVqIFZx68rLa7WFEMq4-JfaPjZvhQRyIqvft-TobBdDyrbgP5E01Y4kBP1xgvjNTphWAUm_hIfeL3V_Dd5nL-qsgCEKu_K9g-Lla9)

![Alt text](http://www.plantuml.com/plantuml/png/XT31Ii0m383XUvuYn-smFi0ePU1LyE0JT5scbQs5D1j8tRixSPc81s_bxnTeCnJnBDkwmDDOY7TYj1_6u4DEWKb8Apnluni5mOxZowjq3lMcntoT2W24nPU25ww07UwUVqIFZx68rLa7WFEMq4-JfaPjZvhQRyIqvft-TobBdDyrbgP5E01Y4kBP1xgvjNTphW9-3UjBcnOE_SsVNLJuJQinvZZzG6awD_a9)


---


### kotlin code


Push base code

```agsl
interface Observer {
    fun update(value: Int)
}

class Subject {
    private val observers = mutableListOf<Observer>()

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    fun setValue(value: Int) {
        notifyObservers(value)
    }

    private fun notifyObservers(value: Int) {
        observers.forEach { it.update(value) }
    }
}

class ConcreteObserver : Observer {
    override fun update(value: Int) {
        println("Observer updated with value $value")
    }
}


```
Pull base code

```agsl
interface Observer {
    fun update()
}

class Subject {
    private val observers = mutableListOf<Observer>()
    private var value: Int = 0

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    fun setValue(value: Int) {
        this.value = value
    }

    fun getValue(): Int {
        return value
    }

    fun notifyObservers() {
        observers.forEach { it.update() }
    }
}

class ConcreteObserver(private val subject: Subject) : Observer {
    private var value: Int = 0

    init {
        value = subject.getValue()
    }

    override fun update() {
        value = subject.getValue()
        println("Observer updated with value $value")
    }
}

```



### 11.3.2 Strategy Pattern

# Strategy Pattern

- **Is a Behavioral design pattern that allows you to define a family of interchangeable algorithms, encapsulate each one as an object, and make them interchangeable at runtime.**


### Steps to create strategy method

1. **Strategy** : Defines an interface of operations to create strategy
2. **Concrete Strategy** : Implement the operation to create the concrete strategy
3. **Context/ Base** : Define a base for inheritance
4. **Client**  : Client code to decide the specific strategy to implement


### Pros
- Can swipe algorithms at runtime based
- Can replace inheritance with composition
- Open/Closed Principle. You can introduce new strategies without having to change the context.


### Cons
- Client should know the different strategies 

### UML

[PlantUML for Strategy Pattern](http://www.plantuml.com/plantuml/uml/XP51IyGm48NlyolUqJqau1wbXiMzUFSNn7Iw3SHKakcATFlVRIsfO2tUXY_p7czUJbbialujq8iaAtpvH7NJ6kSkkiJ2XTx_ytwa9viPAlbBAygWy0SysmnBT0wUeq3rxqhT3EPj8XTipj_pG52useIy2ei5eUaZCai1TzkT0ur4FWgdnZfUSLMjp9Y1i3WLcrbKCcpk7UZNyg2r7cdS4npt1R4JHk16q3LOUY2WUZNBZ9gThpdZe_MkNGqW7Vgv9E1gmmz-t6QTBW6azLd_rTA1EDQOGz1fcgQ__W80)

![Alt text](http://www.plantuml.com/plantuml/png/XP51IyGm48NlyolUqJqau1wbXiMzUFSNn7Iw3SHKakcATFlVRIsfO2tUXY_p7czUJbbialujq8iaAtpvH7NJ6kSkkiJ2XTx_ytwa9viPAlbBAygWy0SysmnBT0wUeq3rxqhT3EPj8XTipj_pG52useIy2ei5eUaZCai1TzkT0ur4FWgdnZfUSLMjp9Y1i3WLcrbKCcpk7UZNyg2r7cdS4npt1R4JHk16q3LOUY2WUZNBZ9gThpdZe_MkNGqW7Vgv9E1gmmz-t6QTBW6azLd_rTA1EDQOGz1fcgQ__W80)

---


How to identity when to user strategy pattern


1. The need to switch behavior dynamically: If you have a situation where you need to switch the behavior of an object at runtime, depending on certain conditions or user inputs, then the Strategy pattern can be a good choice. In contrast, normal inheritance is a static relationship between classes, and the behavior of an object is fixed at compile time. 
2. The need for different variations of behavior: If you have a situation where you need to support multiple variations of a behavior, then the Strategy pattern can be useful. For example, if you have multiple payment methods with different algorithms for processing payments, then you can encapsulate each algorithm in a separate payment strategy class. In contrast, normal inheritance typically involves creating subclasses that inherit the behavior of the parent class. 
3. The need to avoid code duplication: If you have a situation where you need to duplicate code across multiple classes that share similar behavior, then the Strategy pattern can be a good choice. By encapsulating the behavior in separate strategy classes, you can avoid duplicating code across different classes that implement the same behavior. In contrast, normal inheritance can lead to code duplication if you need to override the same behavior in multiple subclasses. 
4. The need for decoupling: If you have a situation where you want to decouple the implementation of a behavior from the class that uses it, then the Strategy pattern can be a good choice. By encapsulating the behavior in separate strategy classes, you can change the implementation of the behavior without affecting the class that uses it. In contrast, normal inheritance can lead to tight coupling between classes, making it difficult to change the behavior without affecting other parts of the system.

---

### Kotlin code

Strategy

```agsl
interface PaymentStrategy {
    fun pay(amount: Double)
}

```
Concrete Strategy

```agsl
class CreditCardStrategy(
    private val name: String,
    private val cardNumber: String,
    private val cvv: String,
    private val dateOfExpiry: String
) :
    PaymentStrategy {
    override fun pay(amount: Double) {
        // logic for processing credit card payment
    }
}


class PayPalStrategy(private val emailId: String, private val password: String) : PaymentStrategy {
    override fun pay(amount: Double) {
        // logic for processing PayPal payment
    }
}


class ApplePayStrategy(private val deviceToken: String) : PaymentStrategy {
    override fun pay(amount: Double) {
        // logic for processing Apple Pay payment
    }
}
```

Context 

```agsl
open class Base {
    private var strategy: PaymentStrategy? = null
    fun setStrategy(strategy: PaymentStrategy) {
        this.strategy = strategy
    }

    fun doPayment(amount: Double) {
        strategy?.pay(amount)
    }
}
```

Client

```agsl
class Client : Base() {
    fun doApplePayment() {
        setStrategy(ApplePayStrategy("token"))
        doPayment(40.0)
    }
}
```

### 11.3.3 Iterator Pattern

# Iterator Pattern

- ** behavioral design pattern that lets you traverse elements of a collection without exposing its underlying
  representation**

### Steps to create observer method

1. **Iterator interface** : Declares operation required to traversing collection
2. **Concrete Iterator** : Implementation of specific algorithm for traversing collection.
3. **Iterable Interface** : Declares the methods for getting iterators compatible with collection. Return types of the
   method must be `Iterator` interface
4. **Concrete Collection** : Implements method to return iterator instance when client requests

### UML

[PlantUML for Strategy Pattern](https://www.plantuml.com/plantuml/uml/XT31Ii0m383XUvuYn-smFi0ePU1LyE0JT5scbQs5D1j8tRlhS9c81s_bxnTeCnJnBDkwmDDOY7TYj1_6u4DEWKb8Apnluni5mOxZowjq3lMcntoT2W24nPU25ww07UwUVqIFZx68rLa7WFEMq4-JfaPjZvhQRyIqvft-TobBdDyrbgP5E01Y4kBP1xgvjNTphWAUm_hIfeL3V_Dd5nL-qsgCEKu_K9g-Lla9)

![Alt text](https://www.plantuml.com/plantuml/png/TOuzoiCm38PtdKBZzmtn0K88WKvPklK0RghyWEqKMu62wxit868QJ4UzUjwVBh3sl5a3FPFNl7fitbM5bzKDdfYoMcDlduOiEGx2tWqLhAW5AxdABzFC2h-uMw000B0u9Z_gWVApQRBMjX4GnytXH7p4_Vr30XYC3g4O4A6aQyn3eb8gGIfhz-UlsYkzUA-KUzP1RaXGRf2W26Uhb9xjnv2Et6EpvWC0)


---

### kotlin code

Iterator interface
```kotlin
interface Iterator<E>{
    fun hasNext() : Boolean
    fun next() : E
}
```

Concrete Iterator
```kotlin
private class ListIterator : Iterator<E> {
    
    fun hasNext() : Boolean{
        
    }
    fun next() : E{
        
    }
}
```

Iterable interface
```kotlin
interface Iterable<E>{
    fun iterator() : Iterator<E>
    fun add(item : E)
}
```

Concrete Iterable
```kotlin
private class OwnList<E> : Iterable<E> {
    val list = mutableListOf<E>()
    fun hasNext() : Boolean{
    }
    fun iterator() : Iterator<E>{
        
    }
    fun add(item : E){
        list.add(item)
    }
}
```

Client code
```kotlin
val ownList = OwnList<Int>()
ownList.iterator()
```


---

# 12. Object-Oriented Design & UML


## 12.1 Class UML Relationships

# Class UML representation

### Association -------> Has A

1. Association is a relation between two separate classes which establishes through their Objects.
2. Association can be one-to-one, one-to-many, many-to-one, many-to-many.
3. In Object-Oriented programming, an Object communicates to another object to use functionality and services provided by that object.
4. Composition and Aggregation are the two forms of association.


Kotlin code 

```agsl
class OwnedClass{ 
// class body
}


class OwnerClass{
    private val ownedClass : OwnedClass
    private val ownerClass : OwnerClass
}
```

UML representation

![Alt text](http://www.plantuml.com/plantuml/png/SoWkIImgAStDuV8lpqijSiv9B2vMiD7LrLK0yLCWV2i5NOLyE1UhoY8abN0k5rSoY49I01D06EQGcfS2j1u0)

---

#### Aggregation ----> has a

In Aggregation, both entries can survive individually which means ending one entity will not affect the other entity.

UML representation

![Alt text](http://www.plantuml.com/plantuml/png/SoWkIImgAStDuGekAKr9pIjHqDNLKd19BG2AveAuLWgEwUb5gUc99Qd5N0wfUIb0jG00)


--- 

#### Composition ----> has a (part of )

It represents part-of relationship.
In composition, both entities are dependent on each other.
When there is a composition between two entities, the composed object cannot exist without the other entity


UML representation

![Alt text](http://www.plantuml.com/plantuml/png/SoWkIImgAStDuNBDIy_CIrNGrTLIS4uiKh1IA01AAEIdSJcavgK05G00)

---


### Extend  -------> Is A

```agsl
class Parent{
    private var data = 0
    fun method(){
        //body
    }
}


class Child : Parent{
    override fun method(){
        // overriden method
    }
}
```

UML representation

![Alt text](http://www.plantuml.com/plantuml/png/POx12i9034Jl-Oevwc71iwobu0_u2y54MjYkagH5gFwxPSMJns4URwRtO8jvAdHXqnh8hvGIpiDO14T0dw5L74JNmkxuKZctr7Kh0S33neL3iR11E1Wd75fncoicZU4kkpszwITfzfN-WNxRshvy0000)

---
### Implements  -------> Is A

```agsl
interface Parent{
    fun method()
}


class Child : Parent{
    override fun method(){
        // overriden method
    }
}
```

UML representation

![Alt text](http://www.plantuml.com/plantuml/png/POwn3i8m44Ft-mgFi4JCA5IL-05-uTHSrKX9g2xNBeL_9o9iUBIVR8_LM6tBYHwiKWp-vPp3VOafN86OroIv-HL4K-9QqJdlOp7HcISPXeFGj6hSsGGxAm8RuuRBDvYtWYosFCFfJ6_w5VMDW_w1KKfeZpu0)


---

### Dependency  -------> 

UML representation

![Alt text](http://www.plantuml.com/plantuml/png/SoWkIImgAStDuNBAJrBGqzEpKt3CoKnELR1II4ajICrBALR8prFWSW2oW8e0)


## 12.2 Use Case Diagrams

# Use case UML representation

## Component of Usecase Diagram

### Actor

User are called actors. They interact with system. Actor can be humans, system, machine/hardware or other external
system.

- **Primary actor** - These are the humans or external system that interact with system
- **Secondary actor** - These are used by system to assist the primary actor. They need primary actor to initiate
  usecase

### Usecase

These are the actions performed on system by actor.

## Relationships in use case diagrams

- **Association**: This shows the relationship between and among actor(s) and use case(s). It represents how an actor
  can perform certain functions. It is denoted by a solid line without arrows. All the actors in a use case diagram must
  have at least one association with any use case. More than one actor can be associated with the same use case, and a
  single actor can be associated with more than one use case.

- **Generalization**: This relationship is also known as inheritance. In a use case diagram, we have parent and child
  use cases. The child use case has generalization
  with the parent use case. Each child inherits the behavior of its parent. It is denoted by a solid line with an arrow
  on only one side (toward the parent use case).

- **Include**: We use this to show the relationship between two use cases. It shows that one use case includes the
  behavior of another use case. The included use case will execute only after the execution of the base use case. We can
  also say that the base use case requires an included use case in order to be completed. It is represented by a dashed
  line with an arrow on only one side (toward the included use case), and we write **<< include >>** above the line.

- **Extend**: We use this to show the relationship between two use cases. It shows that one use case extends the
  behaviors of another use case. The extended use case does not execute every time. It always depends on certain
  conditions. It is used to extend the functionality of the base use case. It is represented by a dashed line with an
  arrow on only one side (toward the base use case), and we write **<< extend >>** above the line.



---

# 13. Data Structures & Algorithms

## 13.1 Algorithms & Techniques

### Miscellaneous Algorithms

# Miscellaneous Of Algorithm

## Array and Subarray Algorithms
* Prefix Sum
* Kadane’s Algorithm – Maximum Subarray Sum
* Boyer-Moore Voting Algorithm – Majority Element (appears more than ⌊n/2⌋ times)
* Moore’s Voting Algorithm – General version of Boyer-Moore for majority
* Dutch National Flag Algorithm – Sorting an array of 0s, 1s, and 2s
* Sliding Window Technique – Maximum/Minimum of all subarrays of size k

## 🔍 Searching Algorithms
* Binary Search – Efficient search in a sorted array
* Ternary Search – Used when function is unimodal (single peak/minimum)
* Jump Search – Improved linear search
* Exponential Search – Fast search for unbounded or infinite lists

## 📊 Sorting Algorithms
* Merge Sort – Divide and conquer, stable sort
* Quick Sort – Divide and conquer, not stable but efficient
* Heap Sort – Uses a heap data structure
* Radix Sort – Non-comparison-based sorting
* Counting Sort – For small integer ranges
* Tim Sort – Hybrid sorting algorithm (used in Python's sort())

## 🧮 Mathematical Algorithms
* Sieve of Eratosthenes – Prime number generation
* Euclidean Algorithm – GCD (Greatest Common Divisor)
* Extended Euclidean Algorithm – GCD and modular inverse
* Fermat’s Little Theorem – Modular inverse (when mod is prime)
* Miller-Rabin Primality Test – Fast probabilistic primality test
* Modular Exponentiation – (a^b mod m)

## 📐 Geometry Algorithms
* Graham’s Scan – Convex Hull
* Jarvis March (Gift Wrapping) – Convex Hull
* Line Sweep Algorithm – Intersection of line segments
* Rotating Calipers – Closest pair, farthest pair, diameter, etc.

## 📈 Dynamic Programming Algorithms
* Floyd-Warshall Algorithm – All-pairs shortest path
* Bellman-Ford Algorithm – Single-source shortest path with negative weights
* Knuth’s Optimization – For certain types of DP recurrence
* Matrix Chain Multiplication – Optimal parenthesization

## 📉 Greedy Algorithms
* Huffman Coding – Minimum average code length (data compression)
* Kruskal’s Algorithm – Minimum Spanning Tree (MST)
* Prim’s Algorithm – MST using greedy growth
* Dijkstra’s Algorithm – Shortest Path (non-negative weights)

## 🧠 Backtracking Algorithms
* N-Queens Problem – Placing N queens on a chessboard
* Sudoku Solver – Solves Sudoku using backtracking
* Hamiltonian Path/Cycle – Visits each node exactly once
* Subset Sum – Can a subset sum to a given value?

## 🧵 String Matching & Processing
* Boyer-Moore String Search – Efficient string searching
* Knuth-Morris-Pratt (KMP) Algorithm – Pattern matching with preprocessing
* Rabin-Karp Algorithm – Hash-based string search
* Z-Algorithm – Efficient pattern matching using Z-values
* Aho-Corasick Algorithm – Multi-pattern string matching
* Suffix Array & LCP Array – Substring queries
* Manacher’s Algorithm – Longest palindromic substring in linear time

## 🌐 Graph Algorithms
* Tarjan’s Algorithm – Strongly Connected Components
* Kosaraju’s Algorithm – Strongly Connected Components (alternative)
* Kahn’s Algorithm – Topological Sorting
* Union-Find / Disjoint Set Union (DSU) – Connectivity, Kruskal’s, cycle detection
* Topological Sort – For Directed Acyclic Graphs (DAG)
* Edmonds-Karp Algorithm – Max Flow (using BFS)
* Dinic’s Algorithm – Max Flow (faster for dense graphs)

## 🧠 Miscellaneous
* Reservoir Sampling – Random sampling from streaming data
* Floyd’s Cycle Detection Algorithm (Tortoise and Hare) – Detect cycles in linked lists
* Segment Tree / Fenwick Tree (Binary Indexed Tree) – Range queries and updates
* Trie (Prefix Tree) – Efficient storage and retrieval of strings

https://zerotomastery.io/cheatsheets/data-structures-and-algorithms-cheat-sheet/#what-is-an-algorithm

### Avoiding TLE (Time Limit Exceeded)

# Avoid TLE (Time Limit Exceeded)

TLE is thrown on online compiler when programs does not run in specified time complexity.

### Example and explanation

If have given the constraints as
        `1 <= nums.length <= 10^5`
Then check substituting the worst case in time complexity like


- `10^5` in O(N^2) it will become`10^2^5` = `10^10` which is not acceptable as compiler support `10^7` or `10^8`
- `10^5` in O(N log N) it will become ~`10^6` which is acceptable
- `10^5` in O(N) it will become `10^5` which is acceptable 

So for  `10^5` we should use O(n log n) , O(N) , O(log n) or O(1) algorithms



### Below is cheat sheet

| Constraints | Worst Time Complexity | Algorithmic Solution                       | Examples                                    |
|-------------|-----------------------|--------------------------------------------|---------------------------------------------|
| n ≤ 12      | O(n!)                 | Recursion & Backtracking                   | Permutation 1….n                            |
| n ≤ 25      | O(2^n)                | Recursion, Backtracking & Bit Manipulation | All subsets of an array of size n           |
| n ≤ 100     | O(n^4)                | Dynamic Programming                        | 4Sum                                        |
| n ≤ 500     | O(n^3)                | Dynamic Programming                        | All triangles with side length less than n  |
| n ≤ 10^4    | O(n^2)                | Dynamic Programming, Graphs, Trees         | Bubble Sort (Slow comparison-based sorting) |
| n ≤ 10^6    | O(n log n)            | Sorting, Binary Search, Divide and Conquer | Merge Sort (Fast comparison-based sorting)  |
| n ≤ 10^8    | O(n)                  | Mathematical, Greedy                       | Min and max of element                      |
| n > 10^8    | O(log n) or O(1)      | Mathematical, Greedy                       | Binary Search                               |


---

Links referred :

1. https://www.geeksforgeeks.org/knowing-the-complexity-in-competitive-programming/


### Series

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


## 13.2 Array

### Array Concepts

# Array

- Array is data structure which allows to store the data of same type.
- Arrays store the data in continuous memory location
- It allows index base value access
- Arrays can be of different types
    - One-Dimensional - knows as Vector
    - Two-Dimensional - knows as Matrix
    - Multi Dimensional
- Array can have maximum approx  **2^31 – 1**. Based on this approximation, we can say that the array can theoretically
  hold 2,147,483,647 elements.

```kotlin
for (i in 2 downTo 0) {
    try {
        val arr = IntArray(Int.MAX_VALUE - i)
        println("Max-Size : ${arr.size}")
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}

```

| Operation | Non-Sorted | Sorted   |
|-----------|------------|----------|
| Insert    | O(N)       | O(N)     |
| Delete    | O(N)       | O(N)     |
| Search    | O(N)       | O(log N) |
| Access    | O(1)       | O(1)     |

----

## Tricks

- Can be solved easily by sorting ?
- Can be solved if we think from reverse order?
- Can we find the index relation ?
- Can it be solved by index as hash?
- Can it be solved using sliding window ?
- Can it be solved by two pointer

## Some Patterns

- Kadane's Algorithm / Array Sum or Prefix Sum
    - [Maximum subarray sum with negative](./KadaneAlgorithm.kt)
    - Find max profit" from subarrays
    - Contiguous elements + maximum sum
    - Sum of elements from i to j
    - Multiple queries for sum of elements
- Two pointer
  - Check if condition is satisfied between two values
  - Find pair/triplet with sum X
  - Palindrome
  - Sorted array
- [Sliding window](../sliding_window/sliding_window.md)
  - Maximum sum of k-length subarray
  - Longest substring with k distinct characters
  - Find window of size K with some condition
  - Contiguous subarrays but not necessarily max sum


### Kadane's Algorithm - Implementation

```kotlin
package array

/***
 * kaden's algorithm state that keep two sum one for running sum and other for max sum
 *
 * Better ignore the current sum if it's negative
 */
fun main() {
    val array = intArrayOf(-2, 1, -3, 4, -1, 2, 1, -5, 4)
    var maxSoFar = array[0]
    var runningSum = array[0]

    for (i in 1 until array.size) {
        runningSum = maxOf(array[i], runningSum + array[i])
        maxSoFar = maxOf(maxSoFar, runningSum)
    }

    println("max sum = $maxSoFar")
}
```


## 13.3 Linked List

### Linked List Concepts

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

### Singly Linked List Implementation

```kotlin
package linklist.singly


fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.printSinglyLL()
    singlyLL.addNode(3)
    singlyLL.addNode(5)
    singlyLL.addNode(6)
    singlyLL.addNode(8)
    singlyLL.addNode(12)
    singlyLL.addNode(1)
    singlyLL.addNode(25)
    singlyLL.printSinglyLL()
    println()
    singlyLL.deleteNode(12)
    println("After deletion 12")
    singlyLL.printSinglyLL()

    println("\nAfter adding head as 4")
    singlyLL.addFirst(4)
    singlyLL.printSinglyLL()
    println("\n Size of LL = ${singlyLL.length(singlyLL.head)}")
}


data class SinglyLLNode<T>(
    val data: T, var next: SinglyLLNode<T>? = null
)

class SinglyLL<T> {

    var head: SinglyLLNode<T>? = null

    /**
     * Time complexity O(N) as we have to traverse the link list
     */
    fun addNode(data: T) {
        head?.let {
            var runnerNext = head
            while (runnerNext?.next != null) {
                runnerNext = runnerNext.next
            }
            runnerNext?.next = getNode(data)
        } ?: run {
            head = getNode(data)
        }
    }


    /**
     * Time complexity O(1) as we have to traverse the link list
     */
    fun addFirst(data: T) {
        head = SinglyLLNode<T>(data = data, head)
    }


    /**
     * Time complexity O(N) as we have to traverse the link list
     */
    fun printSinglyLL() {
        head?.let {
            var runnerNext: SinglyLLNode<T>? = it
            while (runnerNext != null) {
                print(" ${runnerNext.data} ->")
                runnerNext = runnerNext.next
            }
            print(" NULL")
        } ?: run {
            println("Link list is empty")
        }
    }

    private fun getNode(data: T) = SinglyLLNode(data = data)

    /**
     * Time complexity O(N) as we have to traverse the link list
     */
    fun deleteNode(data: T) {
        head?.let {
            // check if user want to delete first node that is head
            if (it.data == data) {
                head = head?.next
                return
            }

            var current: SinglyLLNode<T>? = it
            var prev: SinglyLLNode<T>? = it

            while (current != null) {
                if (current.data == data) { // we found the node
                    prev?.next = current.next
                    return
                }
                prev = current
                current = current.next
            }

        } ?: run {
            println("Link list is empty")
        }
    }

    fun length(head: SinglyLLNode<Int>?): Int {
        if (head == null) {
            return 0
        }
        return 1 + length(head.next)
    }

}```


### Reverse Singly Linked List

```kotlin
package linklist.singly


fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.addNode(3)
    singlyLL.addNode(5)
    singlyLL.addNode(6)
    singlyLL.printSinglyLL()
    singlyLL.head = reverseSinglyLL(singlyLL.head)
    println("\nReverse the link list - Iterative")
    singlyLL.printSinglyLL()

    singlyLL.head = reverseSinglyLLRecursively(singlyLL.head)
    println("\nReverse the link list  - Recursive")
    singlyLL.printSinglyLL()
}

fun reverseSinglyLL(head: SinglyLLNode<Int>?): SinglyLLNode<Int>? {
    var curr = head
    var prev: SinglyLLNode<Int>? = null
    while (curr != null) {
        val next = curr.next
        curr.next = prev
        prev = curr
        curr = next
    }
    return prev
}

fun reverseSinglyLLRecursively(head: SinglyLLNode<Int>?): SinglyLLNode<Int>? {
    if (head?.next == null) {
        return head
    }
    val prev = reverseSinglyLLRecursively(head.next)
    head.next?.next = head
    head.next = null
    return prev
}

```


### Detect Cycle in Linked List

```kotlin
package linklist.singly


fun main() {
    val head = SinglyLLNode(1)

    val node2 = SinglyLLNode(2)
    val node3 = SinglyLLNode(2)
    val node4 = SinglyLLNode(2)


    head.next = node2
    node2.next = node3
    node3.next = node4
//    node4.next = head

    val cyclePresent = cyclic(head)
    println("Link list contains cycle $cyclePresent")
}

fun cyclic(head: SinglyLLNode<Int>?): Boolean {
    var hare = head?.next
    var tortoise = head
    while (hare?.next != null) {
        if (hare == tortoise) {
            return true
        }
        hare = hare.next?.next
        tortoise = tortoise?.next
    }
    return false
}```


### Find Middle Node

```kotlin
package linklist.singly

fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.printSinglyLL()
    singlyLL.addNode(1)
    singlyLL.addNode(2)
    singlyLL.addNode(3)
    singlyLL.addNode(4)
    singlyLL.printSinglyLL()

    println()
    println("Middle node data= ${middleNode(singlyLL.head)?.data}")
}


fun middleNode(head: SinglyLLNode<Int>?): SinglyLLNode<Int>? {

    var hare = head
    var tortoise = head
    while (hare != null) {
        hare = hare.next?.next
        tortoise = tortoise?.next
    }

    return tortoise
}```


### Intersection of Two Linked Lists

```kotlin
package linklist.singly

fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.addNode(1)
    singlyLL.addNode(2)
    singlyLL.addNode(3)
    singlyLL.addNode(4)
    singlyLL.printSinglyLL()


    println()
    val singlyLL2 = SinglyLL<Int>()
    singlyLL2.addNode(2)
    singlyLL2.addNode(3)
    singlyLL2.addNode(4)
    singlyLL2.addNode(6)
    singlyLL2.printSinglyLL()
    println()

    println("Intersection ${intersectionNode(singlyLL.head, singlyLL2.head)?.data}")
}


/**
 * Step	    ptr1	ptr2
 * 0	    1	    9
 * 1	    2	    4
 * 2	    3	    5
 * 3	    4	    null
 * 4	    5	    1
 * 5	    null	2
 * 6	    9	    3
 * 7	    4	    4 ✅ ← Intersection found
 */
fun intersectionNode(head1: SinglyLLNode<Int>?, head2: SinglyLLNode<Int>?): SinglyLLNode<Int>? {

    var ptr1: SinglyLLNode<Int>? = head1
    var ptr2: SinglyLLNode<Int>? = head2

    // If any one of the heads is NULL, there is no intersection

    // If any one of the heads is NULL, there is no intersection
    if (ptr1 == null || ptr2 == null) return null

    while (ptr1 != null || ptr2 != null) {
        ptr1 = if (ptr1 != null) ptr1.next else head2
        ptr2 = if (ptr2 != null) ptr2.next else head1
    }
    return ptr1
}```


### Merge Sorted Linked Lists

```kotlin
package linklist.singly

fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.addNode(1)
    singlyLL.addNode(2)
    singlyLL.addNode(3)
    singlyLL.addNode(4)
    singlyLL.printSinglyLL()


    println()
    val singlyLL2 = SinglyLL<Int>()
    singlyLL2.addNode(2)
    singlyLL2.addNode(3)
    singlyLL2.addNode(4)
    singlyLL2.addNode(6)
    singlyLL2.printSinglyLL()
    println()
//    val node = mergeSortedLinkList(singlyLL.head, singlyLL2.head)

    val nodeRecursion = mergeSortedLinkListRecursive(singlyLL.head, singlyLL2.head)


    var temp = nodeRecursion
    while (temp != null) {
        print("${temp.data} ->")
        temp = temp.next
    }

    print("Null")
}

fun mergeSortedLinkList(headFirst: SinglyLLNode<Int>?, headSecond: SinglyLLNode<Int>?): SinglyLLNode<Int>? {

    val newHead: SinglyLLNode<Int> = SinglyLLNode(data = headFirst?.data ?: headSecond?.data ?: return null)
    var current: SinglyLLNode<Int>? = newHead
    var head1 = headFirst
    var head2 = headSecond

    while (head2 != null && head1 != null) {
        if (head1.data <= head2.data) {
            current?.next = head1
            head1 = head1.next
        } else {
            current?.next = head2
            head2 = head2.next
        }
        current = current?.next
    }
    current?.next = head1 ?: head2
    return newHead.next
}

fun mergeSortedLinkListRecursive(l1: SinglyLLNode<Int>?, l2: SinglyLLNode<Int>?): SinglyLLNode<Int>? {
    if (l1 == null) return l2
    if (l2 == null) return l1

    println("L1 = ${l1.data}  L2 =${l2.data}")

    return if (l1.data <= l2.data) {
        l1.next = mergeSortedLinkListRecursive(l1.next, l2)
        l1
    } else {
        l2.next = mergeSortedLinkListRecursive(l1, l2.next)
        l2
    }
}```


### Nth Node from End

```kotlin
package linklist.singly


fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.addNode(3)
    singlyLL.addNode(5)
    singlyLL.addNode(6)
    singlyLL.addNode(8)
    singlyLL.addNode(12)
    singlyLL.addNode(1)
    singlyLL.addNode(25)

    singlyLL.printSinglyLL()
    printNthNodeFromLast(singlyLL.head, 3)
    removeNthNodeFromLast(singlyLL.head, 3)
    println()
    singlyLL.printSinglyLL()
}


fun printNthNodeFromLast(head: SinglyLLNode<Int>?, n: Int) {
    var fast = head
    var slow = head
    var cnt = 0
    while (cnt < n ) { // move faster pointer with 1 point ahead of position
        fast = fast?.next
        cnt++
    }
    while (fast != null) {
        slow = slow?.next
        fast = fast.next
    }
    print("\n $n the element from last is ${slow?.data}")

}
fun removeNthNodeFromLast(head: SinglyLLNode<Int>?, n: Int) {
    var fast = head
    var slow = head
    var cnt = 0
    while (cnt < n + 1) { // move faster pointer with 2 point extras using + 1 and cnt++
        fast = fast?.next
        cnt++
    }
    while (fast != null) {
        slow = slow?.next
        fast = fast.next
    }
    slow?.next = slow?.next?.next
}```


### Palindrome Linked List

```kotlin
package linklist.singly


fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.addNode(1)
    singlyLL.addNode(2)
    singlyLL.addNode(2)
    singlyLL.addNode(1)

    println("Is Palindrom ${isPalindrome(singlyLL.head)}")
}

    fun isPalindrome(head: SinglyLLNode<Int>?): Boolean {
        val stack = ArrayList<SinglyLLNode<Int>?>()

        var node: SinglyLLNode<Int>? = head
        stack.add(node)
        while (node != null) {
            val next = node.next
            stack.add(next)
            node = next
        }
        stack.removeAt(stack.size - 1)

        node = head
        val min = stack.size shr 1
        while (stack.size > min) {
            if (node?.data != stack.removeAt(stack.size - 1)?.data) return false
            node = node?.next
        }
        return true
    }
```


### Remove All Nodes with Given Value

```kotlin
package linklist.singly

fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.addNode(3)
    singlyLL.addNode(5)
    singlyLL.addNode(5)
    singlyLL.addNode(5)
    singlyLL.addNode(5)
    singlyLL.addNode(6)
    singlyLL.addNode(25)
    singlyLL.addNode(5)

    singlyLL.printSinglyLL()
    removeAllWith(singlyLL.head, 5)

    print("\nRemoved all 5\n")
    singlyLL.printSinglyLL()
}

fun removeAllWith(head: SinglyLLNode<Int>?, num: Int) {
    var curr = head
    var prev: SinglyLLNode<Int>? = null
    while (curr != null) {
        if (curr.data == num) {
            prev?.next = curr.next
        }else {
            prev = curr
        }
        curr = curr.next
    }
}
```


### Remove Duplicates from Sorted List

```kotlin
package linklist.singly

import kotlin.math.sin

fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.addNode(3)
    singlyLL.addNode(5)
    singlyLL.addNode(5)
    singlyLL.addNode(5)
    singlyLL.addNode(5)
    singlyLL.addNode(6)
    singlyLL.addNode(8)
    singlyLL.addNode(8)
    singlyLL.addNode(8)
    singlyLL.addNode(12)
    singlyLL.addNode(1)
    singlyLL.addNode(1)
    singlyLL.addNode(25)
    singlyLL.addNode(25)

    singlyLL.printSinglyLL()
    removeDuplicateFromSortedLinkList(singlyLL.head)

    print("\nRemoved duplicates\n")
    singlyLL.printSinglyLL()
}

fun removeDuplicateFromSortedLinkList(head: SinglyLLNode<Int>?) {
    var curr = head

    while (curr != null) {
        var temp = curr

        /**
         * Move temp until its null or we find another number that is not matching with current value
         */
        while (temp != null && temp.data == curr.data) {
            temp = temp.next
        }

        curr.next = temp
        curr = curr.next // go to next number to find duplicate
    }
}```


### Swap Pairs in Linked List

```kotlin
package linklist.singly




fun main() {
    val singlyLL = SinglyLL<Int>()
    singlyLL.addNode(1)
    singlyLL.addNode(2)
    singlyLL.addNode(3)
    singlyLL.addNode(4)

    singlyLL.printSinglyLL()
    println()
    val newHead = swapPairLL(singlyLL.head)


    var temp = newHead
    while(temp != null){
        print("${temp.data} ->")
        temp = temp.next
    }
    print("Null")
}


/***
 *
 *
 *  prev    curr    3
 *  1       2       3
 *
 *  curr    prev
 *  2       1       3
 *
 */
fun swapPairLL(head: SinglyLLNode<Int>?): SinglyLLNode<Int>? {

    var prev: SinglyLLNode<Int>? = head
    var curr: SinglyLLNode<Int>? = head?.next

    var newHead = head?.next // After first swap, this becomes the new head

    while (curr != null){
        prev?.next = curr.next
        curr.next = prev
        prev = prev?.next
        curr = prev?.next?.next
    }

    return newHead

}```


## 13.4 Stack

### Stack Concepts

# Stack

- Stack data structure which allow Last In Fist Out (LIFO)
- All the operation made at one end called top of the stack (same end)

### Operation of stack
- **Push** - Push item on the top
- **Pop** - Remove item from top
- **Peek/Top** - Return top element of the stack without removing it
- **isEmpty** - checks if stack is empty or not

### Application of stack
- Reverse the arithmetic operation
- Syntax parsing
- Recursion
- Undo and Redo


### Time complexity 


| Operation | Time complexity |
|-----------|-----------------|
| Insert    | O(1)            |
| Delete    | O(1)            |
| Search    | O(N)            |
| Access    | O(N)            |
| Peek      | O(1)            | 


### Question for the Stack
- [Implement stack using list](stack.kt)
- [Reverse string using stack](reverse_string.kt)
- [Reverse string word using stack](reverse_string_words.kt)
- Balancing Bracket
- Reverse stack without using another stack
- Sort stack without using another stack
- Delete intermediate item from stack 

### Stack Implementation

```kotlin
fun main() {

    val stack = Stack<Int>()
    stack.push(4)
    val peekElement = stack.peek()
    println("Top of stack is $peekElement")
    stack.push(3)
    stack.push(53)
    stack.push(536)
    stack.push(14)
    val poppedElement = stack.pop()
    println("Popped Element of stack is $poppedElement")
    stack.printStack()

}

fun <T> Stack<T>.printStack() {
    println("\nStack elements :")
    stk.reversed().forEach {
        print("$it \t")
    }
}

class Stack<T> {
    val stk = ArrayList<T>()
    var top = -1

    fun push(value: T) {
        stk.add(value)
        top++
    }

    fun pop(): T {
        if (!isEmpty()) {
            val poppedElement = stk.removeAt(top)
            top--
            return poppedElement
        } else {
            throw RuntimeException("Stack is empty")
        }
    }

    fun peek(): T {
        if (!isEmpty()) {
            return stk[top]
        } else {
            throw RuntimeException("Stack is empty")
        }
    }

    fun isEmpty(): Boolean {
        return top < 0
    }
}```


### Reverse String using Stack

```kotlin
fun main() {
    val str = "Reverse the string"
    val stack = Stack<Char>()
    str.forEach {
        stack.push(it)
    }

    while (!stack.isEmpty()) {
        print("${stack.pop()}")
    }

}
```


### Reverse Words in String

```kotlin
fun main() {
    val str = "Reverse the string"
    val stack = Stack<Char>()
    str.forEach {
        if (it == ' ') {
            // print the stack
            printStackUsingPop(stack)
            print(it)
        } else {
            stack.push(it)
        }
    }
    printStackUsingPop(stack)
}

private fun printStackUsingPop(stack: Stack<Char>) {
    while (!stack.isEmpty()) {
        print("${stack.pop()}")
    }
}```


## 13.5 Queue

### Queue Concepts

# Queue

- Stack data structure which allow Last In Fist Out (FIFO)
- Insertion the operation made at one end and remove operation made at front

### Operation of Queue
- **Enqueue** - The enqueue operation adds an element to the rear of the queue.
- **Dequeue** - The dequeue operation removes the element from the front of the queue
- **peek() or front()** - Acquires the data element available at the front node of the queue without deleting it.
- **rear() or front()** - This operation returns the element at the rear end without removing it.


### Application of stack


### Time complexity


| Operation | Time complexity |
|-----------|-----------------|
| Insert    | O(1)            |
| Delete    | O(1)            |
| Search    | O(N)            |
| Access    | O(N)            |
| Peek      | O(1)            | 


### Question for the Stack
- [Implement stack using list](stack.kt)
- [Reverse string using stack](reverse_string.kt)
- [Reverse string word using stack](reverse_string_words.kt)
- Balancing Bracket
- Reverse stack without using another stack
- Sort stack without using another stack
- Delete intermediate item from stack 

## 13.6 HashMap & Collections

### HashMap Concepts

# HashMap
- Stores data in key value pair
- Can contain only one null key
- Can contains multiple empty values again keys

| Operation | Average | Worst |
|-----------|---------|-------|
| Insert    | O(1)    | O(N)  |
| Delete    | O(1)    | O(N)  |
| Search    | O(1)    | O(N)  |
| Access    | O(1)    | O(N)  |



> **Average Case (O(1)):** This assumes that the hash function distributes keys uniformly across the hash table and the number of collisions is minimal. Under ideal conditions, the HashMap operations take constant time to execute.

> **Worst Case (O(n)):** In the worst-case scenario, all keys could hash to the same bucket due to hash collisions or poor hash function performance. When this happens, the hash table essentially degrades into a linked list of entries, making operations like put, remove, and get take linear time relative to the number of entries in the map (O(n)).

## 13.7 Hashing

### Hashing Concepts

# Hashing


Hashing is the process of taking an input (or 'message') and applying a mathematical function (hash function) to it to produce a fixed-size string of characters, which is typically a hexadecimal number. This output is called a hash value, hash code, or simply hash.

### Key Characteristics of Hashing:
1. **Deterministic**: For the same input, the hash function always produces the same hash value.
2. **Fast Computation**: Hash functions are designed to be computationally efficient, allowing them to process large amounts of data quickly.
3. **Fixed Output Size**: Regardless of the size of the input, the hash function produces a hash value of fixed size.
4. **Pre-image Resistance**: It should be computationally impractical to reverse the hash value to obtain the original input (one-way function).

### Uses of Hashing:
- **Data Integrity**: Hash functions are commonly used to verify data integrity. By comparing hashes before and after transmission or storage, one can check if the data has been altered.
- **Data Retrieval**: Hash tables use hash functions to map data (keys) to specific locations in memory, allowing for efficient retrieval.
- **Password Storage**: Hash functions are used to securely store passwords. Instead of storing passwords directly, systems store their hash values. During authentication, if the computed hash matches the stored hash, the password is considered correct.
- **Cryptographic Applications**: Hash functions are used in digital signatures, message authentication codes (MACs), and various cryptographic protocols.


---

A **hash collision** occurs when two different inputs into a hash function produce the same hash value as output. 
In other words, the hash function maps two distinct inputs to the same hash value, creating a collision.

### Key Points about Hash Collisions:
1. **Unavoidable with Finite Output**: Since hash functions map an infinite number of possible inputs to a finite number of hash values (due to the fixed size of the hash output), collisions are theoretically unavoidable.

2. **Probability**: A good hash function minimizes the likelihood of collisions occurring for any given set of inputs. This is achieved through properties like uniformity (where each output hash value is equally likely) and avalanche effect (where small changes in input produce drastically different hash outputs).

3. **Impact**: In many applications, such as cryptographic algorithms or hash tables used for data storage and retrieval, collisions are undesirable because they can lead to unexpected behavior or security vulnerabilities. For example, in a hash table, collisions can slow down retrieval times if not handled efficiently.

4. **Hash Function Quality**: The quality of a hash function is often assessed by its resistance to collisions. Cryptographically secure hash functions are designed to make it computationally difficult to find collisions intentionally.

5. **Handling Collisions**: Techniques like chaining (using linked lists or other structures to handle multiple entries with the same hash value) or open addressing (finding alternative slots for colliding entries within the same hash table) are used to manage collisions in hash tables.


### How to handle Hash Collisions
Hash collisions occur when two different inputs to a hash function produce the same hash value. Handling hash collisions depends on the specific hashing technique being used. Here are common approaches to handle collisions:

1. **Separate Chaining (Hash Table with Linked Lists)**:
    - Maintain a data structure like a linked list at each bucket of the hash table.
    - When a collision occurs, append the new key-value pair to the linked list at the corresponding bucket.
    - Lookup involves computing the hash, then searching the linked list for the key.

2. **Open Addressing**:
    - In open addressing, all elements are stored in the hash table itself, usually by probing into other slots if a collision occurs.
    - Common probing techniques include Linear Probing, Quadratic Probing, and Double Hashing.
    - When inserting, if a collision occurs, the algorithm searches for the next open slot (according to the probing strategy) to place the item.



## 13.8 Tree

### Tree Concepts

# Tree

Tree is hierarchical (Non-Linear) data structure.

## Tree concepts (terminology)

[PlatUml Tree](https://www.plantuml.com/plantuml/uml/SoWkIImgoStCIybDBE0goIzGACbNICelASdFLKZ9B4fDBidCp-FIKd3aqj9ISEBI0fAkG198UK8eBmYL3PPo0wbo1ZFT8U8-4CeN9E42bl3WSaZDIu7Q2000)

![alt](http://www.plantuml.com/plantuml/png/SoWkIImgoStCIybDBE0goIzGACbNICelASdFLKZ9B4fDBidCp-FIKd3aqj9ISEBI0fAkG198UK8eBmYL3PPo0wbo1ZFT8U8-4CeN9E42bl3WSaZDIu7Q2000)

- **Root** - Top most element in tree
    - A is root of tree
- **Node** - Which store the information
    - A,B,C,D,E,F,G,H,I,J,K,L,M
- **Parent** - who having child below it
    - G is parent of L and M
    - B is parent of E and F
- **Child** - having parent
    - L and M are child of the G
    - H is child of D
- **Leaf Node** - having no child
    - I,J,K,F,L,M and H
- **Non-Leaf Node (Internal Node)** - having at-least one child
    - A, B,C,D,E nad G are Non-Leaf node
- **Ancestor** - Any predecessor from root node till that node
    - Ancestor of L are G,C and A
    - Ancestor of H are D and A
- **Descendants** Any predecessor from that node till leaf node
    - Descendants of C are G,L and M
    - Descendants of C are E,I,J, K and F
- **Siblings** - All children of same parent
    - E and F are siblings
    - B, C and D are siblings
- **Degree** - No of children of node
    - Degree of A is 3
    - Degree of G is 2
    - Degree of D is 1
    - Degree of L is 0
- **Depth** - Length of longest path from root to that node (no of edges)
    - Depth of F is 2
    - Depth of L is 3
    - Depth of D is 1
    - Depth of A is 0
- **Height** - length of longest path till leaf node
    - Height of B is 2
    - Height of A is 3
    - Height of D is 1

---

## Types of tree

- [Binary tree](BinaryTree.kt)
    - A binary tree is a hierarchical data structure in which each node has at most two children, referred to as the
      left child and the right child.
    - Nodes in a binary tree are arranged in such a way that each node can have at most two children.
    - Binary trees are widely used in computer science for efficient searching and sorting algorithms.

- Complete Binary Tree
    - A complete binary tree is a binary tree in which all levels are completely filled except possibly the last level,
      which is filled from left to right.
    - In a complete binary tree, all nodes are as far left as possible, except possibly for the last level, which is
      filled from left to right.
    - Complete binary trees are useful in representing data with heap structures, and they have efficient array-based
      representations.
- Full Binary Tree (Proper Binary Tree)
    - A full binary tree is a binary tree in which every node other than the leaves has two children
    - In other words, a full binary tree is a binary tree in which every node has either 0 or 2 children, but never 1
      child.
    - Full binary trees are used in various applications, such as expression trees and decision trees.
- Perfect Binary Tree:
    - A perfect binary tree is a binary tree in which all internal nodes have exactly two children, and all leaf nodes
      are at the same level
    - In other words, a perfect binary tree is both full and complete.
    - Perfect binary trees are rare in practice but are used in some theoretical analyses and algorithms.
- Balanced Binary Tree
    - A balanced binary tree is a binary tree in which the height of the two subtrees of any node never differs by more
      than one.
    - Balancing ensures that the height of the tree remains logarithmic with respect to the number of nodes, leading to
      efficient operations such as search, insertion, and deletion.
    - Balanced binary trees are used in various data structures such as AVL trees, red-black trees, and B-trees.

- [Binary search tree - BST](BinarySearchTree.kt)
    - It is Binary tree.
    - It has special property that left and right of tree should be BST
    - All node in Left subtree should be less than **Root**
    - All node in Right subtree should be larger than **Root**
    - Equal value can be put in left or right but not in both
    - **In-Order traversal of BST will always sorted**
    - Search is faster in **_average_** case **O(log N)**. It depends on height of three (skewed tree)
- [AVL tree (Self balancing BST)](AVLTree.kt)
    - Skewed binary search tree gives birth to AVL
    - Is a Binary tree
    - |Height of left tree| - |Height of right tree| = {-1,0,1} --(Balance factor)
- B tree
- B+ tree
- Heap tree
- Red Black tree

 ---

## Tree traversal

- **Pre-Order** (Node, Left, Right)
- **In-Order** (Left, Node, Right)
- **Post-Order** (Left, Right, Node)
- **Level Order Traversal**
- [Traversal's code](Traversal.kt)

### Question on the tree

- [Tree miscellaneous function](TreeMiscellaneous.kt)
- [Check if tree is BST](CheckIsBst.kt)
- Print left view of the tree
- Print right view of the tree
- Print top view of the tree
- [Print all elements in given range](AllElementsInRange.kt)





### Binary Node

```kotlin
package tree

class BinaryNode<T>(
    var data: T,
    var left: BinaryNode<T>? = null,
    var right: BinaryNode<T>? = null,
    var height: Int = 0
){
    override fun toString(): String {
        return "BinaryNode(data=$data)"
    }
}```


### Binary Tree Implementation

```kotlin
package tree







class BinaryTree {
}```


### Binary Search Tree

```kotlin
package tree

fun main() {
    val bst = BinarySearchTree()
    val root = bst.createBst(arrayOf(2, 5, 3, 8, 15, 9, 1, 2))
    root?.inOrderTraversal()

    val valueToFind = 3
    println("\n $valueToFind found = ${bst.search(root, valueToFind)}")

    val valueToDelete = 15
    bst.deleteNode(root, valueToDelete)
    root?.inOrderTraversal()
}

class BinarySearchTree {

    private var root: BinaryNode<Int>? = null

    fun createBst(data: Array<Int>): BinaryNode<Int>? {
        data.forEach {
            root = insert(it, root)
        }
        return root
    }


    /***
     * Complexity of code
     *
     *  O(h) - height
     *
     */
    fun insert(value: Int, root: BinaryNode<Int>?): BinaryNode<Int> {
        if (root == null) {
            return BinaryNode(data = value)
        }
        if (value < root.data) {
            root.left = insert(value, root.left)
        } else {
            root.right = insert(value, root.right)
        }
        return root
    }


    /***
     * Complexity of code
     *
     *  O(h) - height
     *
     */
    fun search(root: BinaryNode<Int>?, value: Int): Boolean {
        root?.let {
            if (it.data == value) {
                return true
            }
            if (value < it.data) {
                return search(it.left, value)
            }
            return search(it.right, value)
        }
        return false
    }

    fun deleteNode(root: BinaryNode<Int>?, value: Int): BinaryNode<Int>? {
        root ?: return null
        when {
            value < root.data -> root.left = deleteNode(root.left, value)
            value > root.data -> root.right = deleteNode(root.right, value)
            else -> {
                when {
                    root.left == null && root.right == null -> {
                        // No children
                        return null
                    }

                    root.left == null -> {
                        // Single Child left}
                        return root.right
                    }

                    root.right == null -> {
                        // Single Child right
                        return root.left
                    }

                    else -> {   //2 Children
                        val minRight = findMinNode(root.right)
                        root.data = minRight!!.data
                        root.right = deleteNode(root.right, minRight.data)
                    }
                }
            }
        }

        return root
    }

    private fun findMinNode(root: BinaryNode<Int>?): BinaryNode<Int>? {
        var current = root
        while (current?.left != null) {
            current = current.left
        }
        return current
    }
}```


### AVL Tree

```kotlin
package tree


/** Binary Search Tree:
 *
 *            60
 *           /  \
 *         50    66
 *        /     /  \
 *       40    62  90
 *      / \
 *    3    55
 *        /
 *       45
 */
fun main() {

//    val values = listOf(60, 50, 40, 66, 90, 55, 62, 3,45)
    val avlTree = AVLTree()
    avlTree.root = avlTree.insert(60, avlTree.root)
    avlTree.root = avlTree.insert(50, avlTree.root)
    avlTree.root = avlTree.insert(40, avlTree.root)
    avlTree.root = avlTree.insert(66, avlTree.root)
    avlTree.root = avlTree.insert(90, avlTree.root)
    avlTree.root = avlTree.insert(55, avlTree.root)
    avlTree.root = avlTree.insert(62, avlTree.root)
    avlTree.root = avlTree.insert(3, avlTree.root)
    avlTree.root = avlTree.insert(45, avlTree.root)
    avlTree.root?.inOrderTraversal()
    println()
}

class AVLTree {
    var root: BinaryNode<Int>? = null

    private fun balanceFactor(node: BinaryNode<Int>?): Int {
        return node?.left?.heightOfNode() ?: (0 - (node?.right?.heightOfNode() ?: 0))
    }

    fun insert(value: Int, rootNode: BinaryNode<Int>?): BinaryNode<Int> {
        if (rootNode == null) {
            return BinaryNode(data = value)
        }

        if (rootNode.data > value) {
            rootNode.left = insert(value, rootNode.left)
            rootNode.left.calculateHeight()
        } else {
            rootNode.right = insert(value, rootNode.right)
            rootNode.right.calculateHeight()
        }

        rootNode.calculateHeight()
        println("value = $value height = $rootNode")
        return rootNode
    }
}

fun BinaryNode<Int>?.calculateHeight(): Int {
    this?.height = maxOf(this?.left.heightOfNode(), this?.right.heightOfNode()) + 1
    return this?.height ?: 0
}

fun BinaryNode<Int>?.heightOfNode(): Int {
    return this?.height ?: 0
}
```


### Tree Traversals

```kotlin
package tree

import java.util.*


fun main(){
    val bstRep = """        
        2
       / \
      1   5
         /  \
        3    8
              \
              15
             /
            9
"""

    val bst = BinarySearchTree()
    val root = bst.createBst(arrayOf(2, 5, 3, 8, 15, 9, 1))

    print(bstRep)
    println("Pre- Order")
    root?.preOrderTraversal()
    println("\nPre- Order Iterative")
    root?.preOrderIterativeTraversal()

    println()
    println("In- Order")
    root?.inOrderTraversal()
    println("\nIn- Order Iterative")
    root?.inOrderIterativeTraversal()

    println()
    println("Post- Order")
    root?.postOrderTraversal()
    println("\nPost- Order Iterative")
    root?.postOrderIterativeTraversal()
}


fun <T> BinaryNode<T>.postOrderTraversal() {
    this.left?.postOrderTraversal()
    this.right?.postOrderTraversal()
    print("$data ")
}


fun <T> BinaryNode<T>.postOrderIterativeTraversal() {
    if (this == null) return

    val stack1 = ArrayDeque<BinaryNode<T>>() // First stack for processing
    val stack2 = ArrayDeque<T>() // Second stack for postorder sequence

    stack1.addLast(this)

    while (stack1.isNotEmpty()) {
        val node = stack1.removeLast()
        stack2.addLast(node.data) // Store node in second stack

        // Push left and right children onto stack1
        node.left?.let { stack1.addLast(it) }
        node.right?.let { stack1.addLast(it) }
    }

    // Print in postorder (since stack2 holds Root → Right → Left)
    while (stack2.isNotEmpty()) {
        print("${stack2.removeLast()} ")
    }
}


fun <T> BinaryNode<T>.inOrderTraversal() {
    this.left?.inOrderTraversal()
    print("$data ")
    this.right?.inOrderTraversal()
}

fun <T> BinaryNode<T>.inOrderIterativeTraversal(){
    val stack = ArrayDeque<BinaryNode<T>>()
    var current: BinaryNode<T>? = this

    while (current != null || stack.isNotEmpty()) {
        // Traverse to the leftmost node
        while (current != null) {
            stack.addLast(current)
            current = current.left
        }

        // Pop and process the node
        current = stack.removeLast()
        print("${current.data} ") // Process node

        // Move to right subtree
        current = current.right
    }
}


fun <T> BinaryNode<T>.preOrderTraversal() {
    print("$data ")
    this.left?.preOrderTraversal()
    this.right?.preOrderTraversal()
}


fun <T> BinaryNode<T>.preOrderIterativeTraversal(){
    val stack = ArrayDeque<BinaryNode<T>>()
    stack.push(this)
    while (stack.isNotEmpty()) {
        val popped = stack.pop()
        print("${popped.data} ")
        popped.right?.let { stack.push(it) }
        popped.left?.let { stack.push(it) }
    }

}

fun <T> BinaryNode<T>.levelOrder() {
    val queue = LinkedList<BinaryNode<T>>()
    queue.add(this)
    while (!queue.isEmpty()) {
        val pop = queue.poll()
        print("${pop?.data} ")
        if (pop.left != null) {
            queue.add(pop.left!!)
        }
        if (pop.right != null) {
            queue.add(pop.right!!)
        }
    }
}

fun <T> BinaryNode<T>.levelOrder2() {
    val que = LinkedList<BinaryNode<T>?>()
    que.offer(this)
    while (que.isNotEmpty()) {
        val size = que.size
        for (i in 0 until size) {
            val node = que.poll()
            if (node?.left != null) {
                que.offer(node.left)
            }
            if (node?.right != null) {
                que.offer(node.right)
            }
            print("${node?.data} ")
        }
        println()
    }
}


fun <T> BinaryNode<T>.levelOrderWithNewLine() {
    val queue = LinkedList<BinaryNode<T>?>()
    queue.add(this)
    queue.add(null) // Add null as a marker for the end of the level
    while (!queue.isEmpty()) {
        val poll = queue.poll()
        if (poll == null) {
            println()
            if (queue.isNotEmpty())
                queue.add(null)

        } else {
            print("${poll?.data} - ")
            if (poll.left != null) {
                queue.add(poll.left!!)
            }
            if (poll.right != null) {
                queue.add(poll.right!!)
            }
        }
    }
}


fun <T> BinaryNode<T>.verticalOrderTraversal() {

}```


### Tree Views (Left, Right, Top, Bottom)

```kotlin
package tree

import java.util.LinkedList

fun main() {


    val treeRep = """        
        8
       / \
      3   10
     / \     \
    1   6     14
       / \    /
      4   7  13
"""

    print(treeRep)

    val bst = BinarySearchTree()
    val root = bst.createBst(arrayOf(8, 3, 10, 1, 6, 14, 4, 7, 13))
    rightViewOfTree(root)

    println("Left view of ")

    leftViewOfTree(root)
}

/**
 * Level order traversal + last element
 */
fun leftViewOfTree(root: BinaryNode<Int>?) {
    if (root == null) {
        return
    }

    val que = LinkedList<BinaryNode<Int>?>()
    que.offer(root)
    while (que.isNotEmpty()) {
        val size = que.size
        for (i in 0 until size) {
            val node = que.poll()
            if (node?.left != null) {
                que.offer(node.left)
            }
            if (node?.right != null) {
                que.offer(node.right)
            }

            if (i == 0) {
                print("${node?.data} ,")
            }
        }

    }

}/**
 * Level order traversal + last element
 */
fun rightViewOfTree(root: BinaryNode<Int>?) {
    if (root == null) {
        return
    }

    val que = LinkedList<BinaryNode<Int>?>()
    que.offer(root)
    while (que.isNotEmpty()) {
        val size = que.size
        for (i in 0 until size) {
            val node = que.poll()
            if (node?.left != null) {
                que.offer(node.left)
            }
            if (node?.right != null) {
                que.offer(node.right)
            }

            if (i == size - 1) {
                print("${node?.data} ,")
            }
        }

    }

}```


### Check if BST

```kotlin
package tree

fun main() {

    val bstRep = """        
        2
       / \
      1   5
         /  \
        3    8
              \
              15
             /
            9
"""
    val bst = BinarySearchTree()
    val root = bst.createBst(arrayOf(2, 5, 3, 8, 15, 9, 1))
    val isBst = CheckIsBst().isBst(root)
    println(bstRep)
    println("\nTree isBst = $isBst")


    val question = """            
            1
           /
          2
         / \
        4   5

"""

    val oneP = BinaryNode(1)
    oneP.left = BinaryNode(2)
    oneP.left?.left = BinaryNode(4)
    oneP.left?.right = BinaryNode(5)

    println(question)
    val isBst2 = CheckIsBst().isBst(oneP)
    println("\nTree isBst2 = $isBst2")
}

class CheckIsBst {

    fun isBst(node: BinaryNode<Int>?, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Boolean {
        if (node == null) return true
         if (node.data <= min ||  node.data >= max) {
             return false
         }
        return isBst(node.left, min, node.data) &&
                isBst(node.right, node.data, max)
    }
}```


### Check Same Tree

```kotlin
package tree

fun main() {
    /***
     *  https://leetcode.com/problems/same-tree/description/
     *
     *         1
     *        / \
     *       2   3
     *
     */
    val oneP = BinaryNode(1)
    oneP.left = BinaryNode(2)
    oneP.right = BinaryNode(3)

    val oneQ = BinaryNode(1)
    oneQ.left = BinaryNode(2)
//    oneQ.right = BinaryNode(3)

   print( isSameTree(oneP, oneQ))
}

fun isSameTree(p: BinaryNode<Int>?, q: BinaryNode<Int>?): Boolean {

    if(p == null && q == null){
        return true
    }else if(p?.data != q?.data){
        return false
    }
    return isSameTree(p?.left, q?.left) && isSameTree(p?.right,q?.right)
}

```


### Check Symmetric Tree

```kotlin
package tree

fun main() {

    val question = """   
       https://leetcode.com/problems/symmetric-tree/description/         
           1
        /    \
       2      3
      / \    / \
     4   5  5   4
"""

    val oneP = BinaryNode(1)
    oneP.left = BinaryNode(2)
    oneP.left?.left = BinaryNode(4)
    oneP.left?.right = BinaryNode(5)

    oneP.right = BinaryNode(3)
    oneP.right?.left = BinaryNode(5)
    oneP.right?.right = BinaryNode(4)

    println("Tree Symmetric ${isSymmetric(oneP)} ")
}


fun isSymmetric(root: BinaryNode<Int>?): Boolean {
    if (root == null) return true
    return isMirror(root.left, root.right)
}
fun isMirror(left: BinaryNode<Int>?, right: BinaryNode<Int>?): Boolean {
    if (left == null && right == null) return true
    if (left == null || right == null) return false
    return (left.data == right.data) && isMirror(left.left, right.right) && isMirror(left.right, right.left)
}```


### All Elements in Range (BST)

```kotlin
package tree


fun main() {
    val bst = BinarySearchTree()
    val root = bst.createBst(arrayOf(8, 3, 10, 1, 6, 14, 4, 7, 13))
    root?.inOrderTraversal()
    println()
    printInRange(root, 5, 12)
}


fun printInRange(root: BinaryNode<Int>?, n1: Int, n2: Int) {
    root?.let {
        printInRange(it.left, n1, n2)
        if (it.data in n1..n2) {
            print("${it.data} - ")
        }
        printInRange(it.right, n1, n2)
    }
}```


### Tree Miscellaneous Operations

```kotlin
package tree

import kotlin.math.max



fun main() {
    val bst = BinarySearchTree()

    /**
     *         8
     *        / \
     *       3   10
     *      / \     \
     *     1   6     14
     *        / \    /
     *       4   7  13
     *
     */
    val root = bst.createBst(arrayOf(8, 3, 10, 1, 6, 14, 4, 7, 13))

    println("Height of every node : ${getNodeHeights(root)}")
    println("Height of Tree : ${getHeightOfTree(root)}")
    println("Level order tree : ")
    root?.levelOrder()
    println()
    println()
    root?.levelOrder2()
    println()
    root?.levelOrderWithNewLine()
    println()


    /***
     *         1
     *        / \
     *       2   3
     *      / \
     *     4   5
     *
     */
    val rootManual = BinaryNode(1)
    rootManual.left = BinaryNode(2)
    rootManual.right = BinaryNode(3)
    rootManual.left?.left = BinaryNode(4)
    rootManual.left?.right = BinaryNode(5)
    println("rootManual Height of Tree : ${getHeightOfTree(rootManual)}")
    println("Height of every node : ${getNodeHeights(root)}")
}


fun getNodeHeights(root: BinaryNode<Int>?): HashMap<Int, Int> {
    val map = HashMap<Int, Int>()
    computeHeights(root, map)
    return map
}

fun computeHeights(root: BinaryNode<Int>?, heightMap: MutableMap<Int, Int>): Int {
    if (root == null) return -1 // Height of null is -1 (leaf nodes will be 0)

    val leftHeight = computeHeights(root.left, heightMap)
    val rightHeight = computeHeights(root.right, heightMap)

    val height = 1 + maxOf(leftHeight, rightHeight)
    heightMap[root.data] = height

    return height
}

fun getHeightOfTree(root: BinaryNode<Int>?): Int {
    if (root == null) {
        return -1
    }
    return 1 + max(getHeightOfTree(root.left), getHeightOfTree(root.right))
}```


## 13.9 Graph

### Graph Concepts

# Graph
Network of node(vertex) and edges(connection)


---

### Graph Terminology
- **Degree of Node**
  - Number of edges connected to node is degree of that node.
  - In an undirected graph, The degree of a node is equal to the number of edges incident to that node.
    - There are two types of degrees commonly considered:
      - **In-degree:** In a directed graph, the in-degree of a node is the number of incoming edges to that node. It represents the number of edges pointing towards the node. 
      - **Out-degree:** In a directed graph, the out-degree of a node is the number of outgoing edges from that node. It represents the number of edges originating from the node
        
- **Example**
  ```agsl
         A --- B
        /       \
       D         C
        
    ```
  1. Node A has a degree of 2 because it is connected to nodes B and D.
      
  2. Node B has a degree of 2 because it is connected to nodes A and C.
    
  3. Node C has a degree of 1 because it is connected to node B.
      
  4. Node D has a degree of 1 because it is connected to node A.
      

---
### Types of graph
- **Undirected Graph**
  - An undirected graph is a graph where the edges do not have a direction
  - The edges represent a symmetric relationship between vertices. 
  - For example, if vertex A is connected to vertex B, then vertex B is also connected to vertex A.
  ```agsl
        A --- B
  ```
- **Directed Graph (Digraph)**
  - In Directed graph edges have direction
  - The edges represent asymmetric relationship between vertices, that means A is connected B, does not mean B is connected A.
  ```agsl
        A ---> B
  ```
- **Un-Weighted Graph**
  - Graph where edge does not have weight or cost associated with it
  - The connections between vertices are considered to have equal importance or distance.
- **Weighted Graph**
  - A weighted graph is a graph where each edge has an associated weight or cost
  - These weights can represent various quantities, such as distances, costs, or capacities
  - Weights can be positive, negative or zero
  ```agsl
        (10)
    A -------> B
  ```
---

### Graph representation
- **Adjacency Matrix**:
  - An adjacency matrix is a 2D matrix where each cell represents an edge between two nodes.
  - If there is an edge between nodes A and B, the corresponding cell will contain a value (usually 1), and if there is no edge, the cell will contain a special value (usually 0 or infinity).
    ```agsl
       A  B  C  D
    A  0  1  1  0
    B  1  0  0  1
    C  1  0  0  1
    D  0  1  1  0
    ```
  **Advantage** 
  1. Simplicity: Edge representation is simple. Uses 2D matrix
  2. Efficient edge queries: Checking edge takes **O(1)** time.
  3. Suitable for dense graph
 
  **Disadvantage**
  1. Space complexity: Matrix requires **O(V^2)** space.
  2. Inefficient for the sparse graph.

---

- **Adjacency List**:
  - An adjacency list represents a graph as an array (or a hash table or list) of lists
  - Each element of the array corresponds to a node, and the list associated with each node contains its adjacent nodes.
    ```agsl
    A: [B, C, D]
    B: [A, D]
    C: [A, D]
    D: [A, B, C]
    ```
    ![Graph](https://www.plantuml.com/plantuml/dpng/SoWkIImgAStDuIejJarEB4vLS8IpdE0iPpZBXSjHGLSNA0McdC4KKD0PGZb1A8VKl1IWaG00)
  
  **Advantage**
  1. Efficient memory usage it requires only **O(V+E)** space.
  2. Efficient traversal of neighbors 
  3. Easy to insert and delete edge

  **Disadvantage**
  1. Slower edge queries. Checking the edge exists between two nodes takes **O(deg(V))**
  2. Less suitable for dense graph.


---

- **Edge List**
  - The edge list representation is a simple and straightforward way to represent a graph using a list of edge
  -  In this representation, each edge of the graph is listed individually, typically with the identifiers or labels of the vertices it connects.
      ```agsl
      [(A, B), (B, C), (C, D), (D, A)]
      ```
  **Advantage**
  1. Simplicity: The edge list representation is simple and intuitive.
  2. Efficient memory usage, Requires **O(E)** space.
  3. Flexibility: edge list allows easy to addition and removal of edges.

  **Disadvantage**
  1. Slower edge queries
  2. Inefficient for certain graph operations like finding neighbors of specific node
  3. Lack of direct node information
---

### Graph Traversal
Graph traversal is the process of visiting all the nodes in a graph.
- **Depth-First Search (DFS)**: 
  - In DFS, you start at a given node and explore as far as possible along each branch before backtracking. 
  - This approach is often implemented using recursion or a stack.
  - [DFS traversal code](DfsTraversal.kt)

- **Breadth-First Search (BFS)**: 
  - In BFS, you start at a given node and explore all its neighboring nodes before moving to the next level of nodes. 
  - This approach is often implemented using a queue.
  - Is level order traversal in terms of tree
  - It goes by step by step
  - [BFS traversal code](BfsTraversal.kt)


---

### Graph Algorithms
- **Shortest Path - Dijkstra's Algorithm**
  - It is Greedy algorithm


### Question for the Graph
- Check graph is connected or not
- Check graph contains cycle or not
- Check graph is bipartite graph

### BFS Traversal

```kotlin
package graph

import graph.BfsTraversal.Companion.GRAPH_VERTEX
import java.util.LinkedList
import java.util.Queue

/***
 * BFS (Breadth first search)
 *  - Go to immediate neighbours first
 *  - Then go to neighbours of neighbours
 *  - In-direct Level order traversal from tree traversal
 *  - Take Queue data structure
 *      - Print current node
 *      - Mark current node as visited
 *      - visit  neighbours of current node
 */
fun main() {
    val bfsTraversal = BfsTraversal()

    println("Connected Graph")

    bfsTraversal.createGraph()
    bfsTraversal.printBfs()

    val visited = Array<Boolean>(GRAPH_VERTEX) { false }
    val queue = LinkedList<Int>()
    queue.add(0)
    println()
    println("Connected Graph Recursive")
    bfsTraversal.printBfsRecursive(queue, visited)
    println()

    // Disconnected graph
    bfsTraversal.createDisconnectedGraph()
    println("Disconnected Graph")
    val queueDisconnected: Queue<Int> = LinkedList()
    val visitedDisconnected = Array<Boolean>(GRAPH_VERTEX) { false }
    bfsTraversal.disconnectedGraph.forEachIndexed { index, _ ->
        if (!visitedDisconnected[index]) {
            bfsTraversal.printDisconnectedBfs(queueDisconnected, visitedDisconnected, index)
        }
    }
}

class BfsTraversal {
    companion object {
        val GRAPH_VERTEX = 7
    }

    private val graph = Array<ArrayList<Edge>>(GRAPH_VERTEX) {
        ArrayList<Edge>()
    }

    val disconnectedGraph = Array<ArrayList<Edge>>(GRAPH_VERTEX) {
        ArrayList<Edge>()
    }


    fun printBfsRecursive(queue: Queue<Int>, visited: Array<Boolean>) {
        if (queue.isEmpty()) {
            return
        }
        val curr = queue.poll()
        if (!visited[curr]) {
            print("$curr\t")
            visited[curr] = true
            graph[curr].forEach {
                queue.offer(it.dest)
            }
        }
        printBfsRecursive(queue, visited)
    }

    fun printBfs() {
        val queue: Queue<Int> = LinkedList()
        val visited = Array<Boolean>(graph.size) { false }

        queue.add(0)

        while (queue.isNotEmpty()) {
            val curr = queue.remove()
            if (!visited[curr]) {
                print("$curr\t")
                visited[curr] = true
                graph[curr].forEach {
                    queue.add(it.dest)
                }
            }
        }
    }

    fun printDisconnectedBfs(queue: Queue<Int>, visited: Array<Boolean>, src: Int) {
        queue.add(src)
        while (queue.isNotEmpty()) {
            val curr = queue.remove()
            if (!visited[curr]) {
                print("$curr\t")
                visited[curr] = true
                disconnectedGraph[curr].forEach {
                    queue.add(it.dest)
                }
            }
        }
        println()
    }

    class Edge(
        val src: Int,
        val dest: Int
    )

    fun createGraph() {

        /****
         *
         *      1 ------- 3
         *    /           | \
         *  0             |  5 --- 6
         *    \           | /
         *      2 ------- 4
         *
         *
         */

        graph[0].add(Edge(0, 1))
        graph[0].add(Edge(0, 2))

        graph[1].add(Edge(1, 3))

        graph[2].add(Edge(2, 4))

        graph[3].add(Edge(3, 4))
        graph[3].add(Edge(3, 5))

        graph[4].add(Edge(4, 3))
        graph[4].add(Edge(4, 5))

        graph[5].add(Edge(5, 6))
    }

    fun createDisconnectedGraph() {

        /****
         *
         *      1 ------- 3
         *    /
         *  0                5 --- 6
         *    \             /
         *      2         4
         *
         *
         */

        disconnectedGraph[0].add(Edge(0, 1))
        disconnectedGraph[0].add(Edge(0, 2))

        disconnectedGraph[1].add(Edge(1, 3))

        disconnectedGraph[4].add(Edge(4, 5))

        disconnectedGraph[5].add(Edge(5, 6))
    }
}```


### DFS Traversal

```kotlin
package graph

import graph.DfsTraversal.Companion.GRAPH_VERTEX
import java.util.Stack

/***
 * DFS (Depth first search)
 *  - Keep going to first neighbour and it's neighbour recursively
 *  - Then go to the next neighbour
 *  - Close to Tree PreOrder traversal
 */
fun main() {
    val dfsTraversal = DfsTraversal()
    dfsTraversal.createGraph()

    var visited = Array(GRAPH_VERTEX) {
        false
    }
    println("Dfs Recursive ")
    dfsTraversal.printDfsRecursive(0, visited, dfsTraversal.graph)


    visited = Array(GRAPH_VERTEX) {
        false
    }
    println()
    println("Dfs Iterative")
    dfsTraversal.printDfsIterative(0, visited, dfsTraversal.graph)

}


class DfsTraversal {
    companion object {
        val GRAPH_VERTEX = 7
    }

    class Edge(
        val src: Int,
        val dest: Int
    )

    val graph = Array(GRAPH_VERTEX) {
        ArrayList<Edge>()
    }

    fun createGraph() {

        /****
         *
         *      1 ------- 3
         *    /           | \
         *  0             |  5 --- 6
         *    \           | /
         *      2 ------- 4
         *
         *
         */

        graph[0].add(Edge(0, 1))
        graph[0].add(Edge(0, 2))

        graph[1].add(Edge(1, 3))

        graph[2].add(Edge(2, 4))

        graph[3].add(Edge(3, 4))
        graph[3].add(Edge(3, 5))

        graph[4].add(Edge(4, 3))
        graph[4].add(Edge(4, 5))

        graph[5].add(Edge(5, 6))
    }

    /**
     * Dfs Recursive
     * 0	1	3	4	5	6	2
     *
     */
    fun printDfsRecursive(curr: Int, visited: Array<Boolean>, graph: Array<ArrayList<Edge>>) {
        if (visited[curr]) return
        print("$curr ")
        visited[curr] = true

        graph[curr].forEach {
            printDfsRecursive(it.dest, visited,graph)
        }
    }


    /**
     * Dfs Iterative
     * 0	2	4	5	6	3	1
     */
    fun printDfsIterative(curr: Int, visited: Array<Boolean>, graph: Array<ArrayList<Edge>>) {
        val stack = Stack<Int>()

        stack.push(curr)

        while (stack.isNotEmpty()) {
            val popItem = stack.pop()
            print("$popItem ")
            visited[popItem] = true
            graph[popItem].forEach {
                if (!visited[it.dest]) {
                    stack.push(it.dest)
                }
            }
        }
    }
}```


## 13.10 Heap & Priority Queue

### Heap Concepts

# Heap (Priority Queue)

>A heap is a specialized tree-based data structure that satisfies the heap property. The heap property can be of two types: max-heap and min-heap. In a max-heap, for any given node
i, the value of the node is greater than or equal to the values of its children. In a min-heap, for any given node
i, the value of the node is less than or equal to the values of its children.

- Heaps are specialized tree-based data structures that satisfy the heap property, which specifies the relationship between parent and child nodes. In a max-heap, for any given node i, the value of the node is greater than or equal to the values of its children. In a min-heap, the opposite holds true.

- Heaps are typically implemented using arrays, where the elements are arranged in a way that satisfies the heap property. However, the elements are not necessarily sorted in ascending or descending order.
- **Heap is complete binary tree**
- **Heap is implemented Queue and visualised as Tree.**

Below is complete Binary tree

[PlatUml Tree](https://www.plantuml.com/plantuml/uml/SoWkIImgoStCIybDBE0goIzGACbNICelASdFLKZ9B4fDBidCp-FIKZ0qCE3Iqb8m0P90oWX26OF5J24SGrCkXzIy5AXH0000)

![alt](https://www.plantuml.com/plantuml/png/SoWkIImgoStCIybDBE0goIzGACbNICelASdFLKZ9B4fDBidCp-FIKZ0qCE3Iqb8m0P90oWX26OF5J24SGrCkXzIy5AXH0000) 


Complete binary tree can be easily flatten into **Array / Queue**
Using below logic

>If the Parent is at 0 then  
> Left child = 2i+1  
> Right child = 2i+2
> Parent = (i-1)/2

>If the Parent is at 1 then  
> Left child = 2i  
> Right child = 2i+1
> Parent  = i/2


## Basic operation
1. Insert
2. Remove
3. Heapify (Sorting)



### Custom Heap Implementation

```kotlin
package heap_priority_queue

import java.util.*

class HeapCustom(private val isMinHeap: Boolean) {
    var heap = mutableListOf<Int>()

    fun push(value: Int) {
        heap.add(value)

        var currentItemIndex = heap.size - 1
        var parent = getParentIndex(currentItemIndex)

        if (isMinHeap) {
            while (currentItemIndex != 0 && heap[currentItemIndex] < heap[parent]) {
                Collections.swap(heap, currentItemIndex, parent)
                currentItemIndex = parent
                parent = getParentIndex(parent)
            }
        } else {
            while (currentItemIndex != 0 && heap[currentItemIndex] > heap[parent]) {
                Collections.swap(heap, currentItemIndex, parent)
                currentItemIndex = parent
                parent = getParentIndex(parent)
            }
        }
    }

    fun pop(): Int {
        val data = heap[0]
        Collections.swap(heap, 0, heap.size - 1)
        heap.removeLast()
        heapify(0)
        return data
    }

    fun heapify(currentIndex: Int) {
        val left = 2 * currentIndex + 1
        val right = 2 * currentIndex + 2

        var index = currentIndex

        val headpSize = heap.size
        if (isMinHeap) {
            if (left < headpSize && heap[left] < heap[index]) {
                index = left
            }
            if (right < headpSize && heap[right] < heap[index]) {
                index = right
            }
        } else {
            if (left < headpSize && heap[left] > heap[index]) {
                index = left
            }
            if (right < headpSize && heap[right] > heap[index]) {
                index = right
            }
        }
        if (index != currentIndex) {
            Collections.swap(heap, currentIndex, index)
            heapify(index)
        }
    }


    private fun getParentIndex(currentItemIndex: Int) = (currentItemIndex - 1) / 2

    fun print() {
        heap.forEachIndexed { index, values ->
            print(
                "$values${
                    if (index != heap.size - 1) {
                        ", "
                    } else {
                        ""
                    }
                }"
            )
        }
    }
}```


### Inbuilt Heap (PriorityQueue)

```kotlin
package heap_priority_queue

import java.util.*

fun main() {
//    InbuiltHeap().buildMaxHeap()

    val input = intArrayOf(1, 3, 5, 4, 6, 13, 10, 9, 8, 15, 17)
    val heapCustom = HeapCustom(true)
    input.forEach {
        heapCustom.push(it)
    }
    heapCustom.print()
    println()
    heapCustom.pop()
    println()
    heapCustom.print()

}

class InbuiltHeap {
    fun buildMaxHeap() {
        // Creating a max heap PriorityQueue
        val maxHeap = PriorityQueue<Int>(Comparator.reverseOrder()) // by default its min heap, natural ordering

        // Inserting elements into the max heap
        maxHeap.add(10)
        maxHeap.add(20)
        maxHeap.add(15)
        maxHeap.add(30)

        print(maxHeap)

        // Extract the maximum element (root of the max heap)
//        println(maxHeap.poll()) // Output: 30
//        println(maxHeap.poll()) // Output: 20
//        println(maxHeap.poll()) // Output: 15
//        println(maxHeap.poll()) // Output: 10
        while (maxHeap.isNotEmpty()) {
            println(maxHeap.poll())
        }
    }
}```


### Find Median from Numbers

```kotlin
package heap_priority_queue

import java.util.*
import kotlin.math.min


/**
 * Find the Median of a Number Stream
 *
 *
 * Mean, median, and mode
 *
 * Mean: The "average" number; found by adding all data points and dividing by the number of data points.
 * Example: The mean of 4,1 and 7 is (4+1+7)/3 =  4
 *
 *Median: The middle number; found by ordering all data points and picking out the one in the middle
 * (or if there are two middle numbers, taking the mean of those two numbers)
 *
 * Example: The median of4,1 and 7 is 4 because when the numbers are put in order
 *
 *Mode: The most frequent number—that is, the number that occurs the highest number of times
 *
 * Example: The mode of {4,2,4,3,2,2,}4 is 2 because it occurs three times, which is more than any other number.
 *
 */
class FindMedianFromNumber {
    val maxHeap = PriorityQueue<Int>(Comparator.reverseOrder()) // left half of the array
    val minHeap = PriorityQueue<Int>(Comparator.reverseOrder()) // right half of array

    fun insertNum(num: Int) {
        if (maxHeap.isEmpty() || maxHeap.first() >= num) {
            maxHeap.offer(num);
        } else {
            minHeap.offer(num);
        }

        if (maxHeap.size > minHeap.size + 1) {
            minHeap.offer(maxHeap.poll()) // remove head from max heap and add it to minHeap
        } else {
            maxHeap.offer(minHeap.poll()) // remove head from min heap and add it to maxHeap
        }
    }

    fun median(): Float {
        if (minHeap.size == maxHeap.size) {
            return (minHeap.peek() + maxHeap.peek()) / 2F
        } else {
            return maxHeap.peek().toFloat()
        }
    }
}
/**
 * Time complexity #
 * The time complexity of the insertNum() will be O(logN) due to the insertion in the heap.
 * The time complexity of the findMedian() will be O(1) as we can find the median from the top elements of the heaps.
 *
 * Space complexity #
 * The space complexity will be O(N) because, as at any time, we will be storing all the numbers.
 */```


## 13.11 Recursion & Backtracking

### Recursion Concepts

# Recursion
- Function calling itself is called recursion
- It divides big problem into sub-problems
- **Tail-recursion**
  - Tail recursion is a concept in computer programming where a recursive function makes its recursive call as the last operation before returning. In other words, the recursive call is in the "tail" position of the function.
  

---

## Points to remember
Every recursion has below two properties
1. Recursion has base case (Exit condition)
2. Logic for problem

---

[Recursion direction](RecursionDirection.kt)
- Going towards base case
    ```kotlin
        // base condition
  
        // your logic
  
        // call recursion
    ```
- Coming back from base case
    ```kotlin
        // base condition
  
        // call recursion
  
        // your logic
    ```




Problems

1. [Sum of first natural number](./SumOfNaturalNumber.kt)
2. [Nth Fibonacci number](./NFibonacciNumber.kt)
3. [Power function](./Power.kt)
4. [Check String is palindrome](./Palindrome.kt)
5. [Reverse Array using recursion](./ReverseArray.kt)
6. [Generate Power set](./PowerSet.kt)
7. [Permutation](./Permutation.kt)
7. [Permutation](./NQueen.kt)






### Sum of Natural Numbers

```kotlin
package recursion

fun main() {

    val n = 10
    println("Print sum of natural number ($n) sum =  ${sumOfNaturalNumber(n)}")
    println("Print sum of natural ny formula =  ${(n * (n + 1) / 2)}")
}

fun sumOfNaturalNumber(num: Int): Int {
    if (num == 1) return 1
    return num + sumOfNaturalNumber(num - 1)
}```


### N-th Fibonacci Number

```kotlin
package recursion


fun main() {
    val num = 0
    println("N the fibonacci number = ${nFibonacciNumber(num)}")
}

fun nFibonacciNumber(n: Int): Int {
    if (n == 1 || n == 2) return 1
    return nFibonacciNumber(n - 1) + nFibonacciNumber(n - 2)

}```


### Reverse Array

```kotlin
package recursion

fun main() {
    val array = arrayOf(1, 2, 3, 4, 5)
    println("Array ${array.joinToString(separator = ",", prefix = "[", postfix = "]")}")
    reverseArray(array, 0, array.size - 1)
    println("Reverse Array ${array.joinToString(separator = ",", prefix = "[", postfix = "]")}")


    val array2 = arrayOf(1, 2, 3, 4, 5)
    reverseUsingSinglePointer(array2, 0, array.size )
    println("Reverse Array ${array2.joinToString(separator = ",", prefix = "[", postfix = "]")}")
}


fun reverseArray(arr: Array<Int>, start: Int, end: Int) {
    if (start >= end) return
    val num = arr[start]
    arr[start] = arr[end]
    arr[end] = num
    reverseArray(arr, start + 1, end - 1)
}

fun reverseUsingSinglePointer(arr: Array<Int>, start: Int, size: Int) {
    if (start >= size / 2) return

    val num = arr[start]
    val i = size - start - 1
    arr[start] = arr[i]
    arr[i] = num
    reverseArray(arr, start + 1, start)
}```


### Palindrome Check (Recursive)

```kotlin
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
}```


### Power Calculation

```kotlin
package recursion

import kotlin.math.abs


fun main() {
    val num = 2
    val pow = 3


    val numD = 2.0
    val powD = -2

    println("Power of ($num, $pow) =  ${power(num, pow)}")
    println("Improved Power of ($numD, $powD) =  ${myPow(numD, powD)}")

}

fun power(x: Int, pow: Int): Long {
    if (pow == 0) {
        return 1
    }
    return x * power(x, pow - 1)
}

/***
 * Improved version
 */


fun myPow(x: Double, n: Int): Double {
    val base = if (n < 0) 1.0 / x else x
    return exp(base, abs(n))
}
private fun exp(x: Double, n: Int) : Double {
    if (n == 0) return 1.0
    val half = exp(x, n / 2)
    return if (n % 2 == 0) {
        half * half
    } else {
        x * half * half
    }
}
```


### Power Set

```kotlin
package recursion


fun subsets(nums: IntArray): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    helper(nums, 0, mutableListOf<Int>(), result)
    return result
}


fun helper(
    nums: IntArray,
    index: Int,
    current: MutableList<Int>,
    result: MutableList<List<Int>>
){
//    println("Index $index")
    if(index == nums.size) {
        result.add(current.toList())
//        println("Adding current to result ${current.joinToString()}")
        return
    }

    // exclude the current element
    helper(nums, index + 1, current, result)
//    println("Current ${current.joinToString()} result = ${result.joinToString()}")
//
    current.add(nums[index])
//    println("Adding ${nums[index]} Index = $index  Current ${current.joinToString()}")

    helper(nums, index + 1, current, result)
//    println("Removing last from current ${nums[index]} result = ${result.joinToString()}\"")
    current.removeLast()

}

fun main() {
    val inputSet = intArrayOf(1, 2, 3)
    val powerSet = subsets(inputSet)

    println("Power set:")
    for (subset in powerSet) {
        println(subset)
    }
}
```


### Subsequences

```kotlin
package recursion


fun main() {
//    val array = intArrayOf(3, 1, 2)
//    subSequence(0, array, array.size, mutableListOf())


    val str = "Nil" // keep in mind that it has O(2^N) complexity larger string slows the program and above 100 length will be almost impossible
    printAllSubSequence(0, str, str.length, StringBuilder())
}

fun subSequence(index: Int, list: IntArray, size: Int, result: MutableList<Int>) {

    if (index >= size) {
        println(result)
        return
    }
    result.add(list[index]) // take condition
    subSequence(index + 1, list, size, result)
    result.removeLast() // not take
    subSequence(index + 1, list, size, result)
}


fun printAllSubSequence(index: Int, str: String, size: Int, result: StringBuilder) {
    if (index >= size) {
        println(result)
        return
    }

    // pick condition
    val c = str[index]
    result.append(c)
    printAllSubSequence(index + 1, str, size, result)
    result.deleteAt(result.length - 1)
    printAllSubSequence(index + 1, str, size, result)
}```


### Permutations

```kotlin
package recursion


fun main() {
    val nums = intArrayOf(1, 2, 3)
    println(permutation(nums))

}

fun permutation(nums: IntArray): MutableList<MutableList<Int>> {
    val ans = mutableListOf<MutableList<Int>>()
    val freq = BooleanArray(nums.size) { false }
    permHelper(nums, ans, mutableListOf(), freq)
    return ans
}

fun permHelper(nums: IntArray, ans: MutableList<MutableList<Int>>, currDs: MutableList<Int>, freq: BooleanArray) {
    if (currDs.size == nums.size) {
        ans.add(ArrayList(currDs))
        return
    }

    for (i in nums.indices) {
        if (!freq[i]) {
            freq[i] = true
            currDs.add(nums[i])
            permHelper(nums, ans, currDs, freq)
            currDs.removeLast()
            freq[i] = false
        }
    }

}```


### Combination Sum

```kotlin
package recursion

/**
 * https://leetcode.com/problems/combination-sum/description/
 *
 * Given an array of distinct integers candidates and a target integer target, return a list of all unique combinations of candidates where the chosen numbers sum to target. You may return the combinations in any order.
 *
 * The same number may be chosen from candidates an unlimited number of times. Two combinations are unique if the frequency of at least one of the chosen numbers is different.
 *
 * The test cases are generated such that the number of unique combinations that sum up to target is less than 150 combinations for the given input.
 *
 *
 *
 * Example 1:
 *
 * Input: candidates = [2,3,6,7], target = 7
 * Output: [[2,2,3],[7]]
 * Explanation:
 * 2 and 3 are candidates, and 2 + 2 + 3 = 7. Note that 2 can be used multiple times.
 * 7 is a candidate, and 7 = 7.
 * These are the only two combinations.
 * Example 2:
 *
 * Input: candidates = [2,3,5], target = 8
 * Output: [[2,2,2,2],[2,3,3],[3,5]]
 * Example 3:
 *
 * Input: candidates = [2], target = 1
 * Output: []
 *
 */
fun main() {
    val candidates = intArrayOf(2, 3, 6, 7)
    println("Combination ${combinationSum(candidates, 7)}")
}

fun combinationSum(candidates: IntArray, target: Int): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    helper(0, candidates, target, result, mutableListOf(), 0)
    return result
}


fun helper(
    index: Int,
    array: IntArray,
    target: Int,
    result: MutableList<List<Int>>,
    currentStack: MutableList<Int>,
    sum: Int
) {
    if (sum == target) {
        result.add(ArrayList(currentStack)) // Make a copy before adding
        return
    }
    if (index == array.size || sum > target) return
    currentStack.add(array[index])
    helper(index , array, target, result, currentStack, sum + array[index]) // reuse allowed
    currentStack.removeLast()

    // Exclude current element and move to next
    helper(index + 1, array, target, result, currentStack, sum )

}
```


### N-Queens Problem

```kotlin
package recursion

import java.util.*


fun solveNQueens(n: Int): List<List<String>> {
    val board = Array(n) { CharArray(n){'.'} }
//    for (i in 0 until n) for (j in 0 until n) board[i][j] = '.'
    val res: MutableList<List<String>> = ArrayList()
    dfs(0, board, res)
    return res
}

fun validate(board: Array<CharArray>, rowArg: Int, colArg: Int): Boolean {
    var row = rowArg
    var col = colArg
    while (row >= 0 && col >= 0) {
        if (board[row][col] == 'Q') return false
        row--
        col--
    }
    row = rowArg
    col = colArg
    while (col >= 0) {
        if (board[row][col] == 'Q') return false
        col--
    }
    row = rowArg
    col = colArg
    while (col >= 0 && row < board.size) {
        if (board[row][col] == 'Q') return false
        col--
        row++
    }
    return true
}

fun dfs(col: Int, board: Array<CharArray>, res: MutableList<List<String>>) {
    if (col == board.size) {
        res.add(construct(board))
        return
    }
    for (row in board.indices) {
        if (validate(board, row, col)) {
            board[row][col] = 'Q'
            dfs(col + 1, board, res)
            board[row][col] = '.'
        }
    }
}

fun construct(board: Array<CharArray>): List<String> {
    val res: MutableList<String> = LinkedList()
    for (i in board.indices) {
        val s = String(board[i])
        res.add(s)
    }
    return res
}


fun main() {
    val N = 4
    val queen = solveNQueens(N)
    var i = 1
    for (it in queen) {
        println("Arrangement $i")
        for (s in it) {
            println(s)
        }
        println()
        i += 1
    }
}
```


### Recursion Direction (Print)

```kotlin
package recursion

fun main() {
    printIncreasing(5)
    println()
    printDecreasing(5)
}


fun printDecreasing(num: Int) {
    // base condition
    if (num == 0) {
        return
    }
    // Going toward  base condition
    print("$num ")
    printDecreasing(num - 1)
}


fun printIncreasing(num: Int) {
    // base condition
    if (num == 0) {
        return
    }
    printIncreasing(num - 1)
    // coming from base condition
    print("$num ")
}
```


## 13.12 Dynamic Programming

### DP Concepts

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

### Fibonacci DP

```kotlin
package dp


fun main() {
    val num = 6
    println("N the fibonacci number using Memoization = ${fibMemoization(num)}")

    println("N the fibonacci number using Tabulation = ${fibTabulation(num)}")
    println("N the fibonacci number using Tabulation fibTabulationOptimal = ${fibTabulationOptimal(num)}")

}

val memoization = mutableMapOf<Int, Long>()


/**
 * Time complexity -  O(N)
 * Space complexity = O(N) [memo map] + O(N) [call stack] = O(N)
 */
fun fibMemoization(n: Int): Long {
    if (n <= 1) return n.toLong()
    if (memoization[n] != null) return memoization[n]!!
    val value = fibMemoization(n - 1) + fibMemoization(n - 2)
    memoization[n] = value
    return memoization[n]!!
}


/**
 * Time complexity -  O(N)
 * Space complexity = O(N)  --> No call stack space
 */
fun fibTabulation(n: Int): Long {
    if (n <= 1) return n.toLong()
    val dp = LongArray(n + 1)
    dp[0] = 0
    dp[1] = 1

    for (i in 2..n) {
        dp[i] = dp[i - 1] + dp[i - 2]
    }
    return dp[n]
}

/**
 * Time complexity -  O(N)
 * Space complexity = O(1)
 */
fun fibTabulationOptimal(n: Int): Long {
    if (n <= 1) return n.toLong()
    var prev2 = 0L
    var prev = 1L

    for (i in 2..n) {
        val cur = prev + prev2
        prev2 = prev
        prev = cur
    }
    return prev
}


```


### Stairs Climbing

```kotlin
package dp

/****
 * https://leetcode.com/problems/climbing-stairs/description/
 */

fun main() {
    print(climStairs(43))
}

fun climStairs(n: Int): Int {
    if (n <= 2) {
        return n
    }
    return climStairs(n - 1) + climStairs(n - 2)
}```


### Frog Jump

```kotlin
package dp

import java.lang.Integer.min
import kotlin.math.abs


fun main() {
//    val height = intArrayOf(30, 10, 60, 10, 60, 50)
//    val height = intArrayOf(10, 20, 30, 10)
    val height = intArrayOf(10, 3, 40, 5, 25)
    println("Min const for Jump = ${minCost(height)}")
}

fun minCost(height: IntArray): Int {
    return helperMinCost(height, (height.size - 1), mutableMapOf())
}

fun helperMinCost(height: IntArray?, pos: Int, mutableMapOf: MutableMap<Int, Int>): Int {
    if (pos == 0) return 0

    if (mutableMapOf[pos] != null) return mutableMapOf[pos]!!

    var right = Int.MAX_VALUE
    val left = helperMinCost(height, pos - 1, mutableMapOf) + abs(height?.get(pos)!! - height[pos - 1])
    if (pos > 1) {
        right = helperMinCost(height, pos - 2, mutableMapOf) + abs(height[pos] - height[pos - 2])
    }
    return min(left, right)
}

```


### Maximum Sum Non-Adjacent

```kotlin
package dp

import kotlin.math.max

/***
 * https://takeuforward.org/data-structure/maximum-sum-of-non-adjacent-elements-dp-5/
 */
fun main() {
//    val array = intArrayOf(2, 1, 4, 9)
//    val array = intArrayOf(2, 7, 9, 3,1)
    val array = intArrayOf(2, 3, 2)
    val maxSum = maxSum(array.size - 1, array, IntArray(array.size))


    val maxSumTabulation = maxSumTabulation(array)
    println("Max Non adjacent sum $maxSum")
    println("MaxSumTabulation Non adjacent sum $maxSumTabulation")

    val spaceOptimisedMaxSumTabulation = spaceOptimisedMaxSumTabulation(array)
    println("spaceOptimisedMaxSumTabulation Non adjacent sum $spaceOptimisedMaxSumTabulation")
}

fun maxSum(pos: Int, array: IntArray, dp: IntArray): Int {

    if (pos == 0) return array[pos]
    if (pos < 0) return 0

    if (dp[pos] != 0) return dp[pos]

    val picked = array[pos] + maxSum(pos - 2, array, dp)
    val notPicked = maxSum(pos - 1, array, dp)
    dp[pos] = max(picked, notPicked)
    return dp[pos]
}

fun maxSumTabulation(array: IntArray): Int {
    val dp = IntArray(array.size)
    dp[0] = array[0]
    for (i in 1 until array.size) {
        var take = array[i]
        if (i > 1) take += dp[i - 2]
        val nonTake = dp[i - 1]
        dp[i] = max(take, nonTake)
    }
    return dp[array.size - 1]
}

fun spaceOptimisedMaxSumTabulation(array: IntArray): Int {
    var prev = array[0]
    var prev2 = 0
    for (i in 1 until array.size) {
        val curr = array[i] + prev2
        prev2 = prev
        prev = max(curr, prev)
    }
    return prev
}


```


## 13.13 Binary Search

### Binary Search Concepts

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

### Binary Search Implementation

```kotlin
package search.binary


fun main() {
    val intArray = intArrayOf(1, 2, 4, 6, 7, 8, 9)

    println("Found 2 at ${findUsingBinarySearch(leftPar = 0, rightPar = intArray.size - 1, 2, arr = intArray)}")
    println("Found 5 at ${findUsingBinarySearch(leftPar = 0, rightPar = intArray.size - 1, 5, arr = intArray)}")
    println("Found 10 at ${findUsingBinarySearch(leftPar = 0, rightPar = intArray.size - 1, 10, arr = intArray)}")
    println("Found 9 at ${findUsingBinarySearch(leftPar = 0, rightPar = intArray.size - 1, 9, arr = intArray)}")
    println()
    println("Found 2 at ${binarySearchRec(left = 0, right = intArray.size - 1, arr = intArray, 2)}")
    println("Found 5 at ${binarySearchRec(left = 0, right = intArray.size - 1, arr = intArray, 5)}")
    println("Found 10 at ${binarySearchRec(left = 0, right = intArray.size - 1, arr = intArray, 10)}")
    println("Found 9 at ${binarySearchRec(left = 0, right = intArray.size - 1, arr = intArray, 9)}")
}


/***
 *  - Also think of applying binary search on Reverse sorted array
 *  - Also think of give array is sorted but don't know in which order
 */
fun findUsingBinarySearch(leftPar: Int, rightPar: Int, target: Int, arr: IntArray): Int {
    var left = leftPar
    var right = rightPar

    while (left <= right) {
        val mid = (left + right).ushr(1) //left + (right - left) / 2
        when {
            target == arr[mid] -> return mid
            target < arr[mid] -> right = mid - 1
            else -> left = mid + 1
        }
    }

    return -1
}

fun binarySearchRec(left: Int, right: Int, arr: IntArray, target: Int): Int {
    if (right >= arr.size || left < 0 || left > right) return -1
    val mid = left + (right - left) / 2
    if (arr[mid] == target) return mid

    return if (arr[mid] > target) {
        binarySearchRec(
            left, mid - 1, arr, target
        )
    } else {
        binarySearchRec(
            mid + 1,
            right,
            arr,
            target
        )
    }
}```


### First Occurrence in Sorted Array

```kotlin
package search.binary

/**
 * Think of it as how to find last occurrence
 */
fun main() {
    val intArray = intArrayOf(1, 2, 4, 4, 4, 4, 4, 6, 6, 6, 7, 8, 9)

    println("First occurrence of 4 at ${findFirstOccurrence(target = 4, arr = intArray)}")
    println("Last occurrence of 4 at ${findLastOccurrence(target = 4, arr = intArray)}")
    println("First occurrence of 6 at ${findFirstOccurrence(target = 6, arr = intArray)}")
    println("Last occurrence of 16 at ${findLastOccurrence(target = 16, arr = intArray)}")
}

fun findFirstOccurrence(target: Int, arr: IntArray): Int {
    var left = 0
    var right = arr.size - 1
    var result = -1
    while (left <= right) {
        val mid = left + (right - left) / 2
        if (target <= arr[mid]) {
            // this is potential answer
            if(target == arr[mid]) {
                result = mid
            }
            right = mid - 1
        } else {
            left = mid + 1
        }
    }
    return result
}

fun findLastOccurrence(target: Int, arr: IntArray): Int {
    var left = 0
    var right = arr.size - 1
    var result = -1
    while (left <= right) {
        val mid = left + (right - left) / 2
        if (target >= arr[mid]) {
            // this is potential answer
            if(target == arr[mid]) {
                result = mid
            }
            left = mid + 1
        } else {
            right = mid - 1
        }
    }
    return result
}```


### Count of Element in Sorted Array

```kotlin
package search.binary

fun main() {
    val intArray = intArrayOf(1, 2, 4, 4, 4, 4, 4, 6, 6, 6, 7, 8, 9)
    println("Count  of 4 = ${countElements(target = 4, arr = intArray)}")
    println("Count  of 6 = ${countElements(target =6, arr = intArray)}")
    println("Count  of 16 = ${countElements(target = 16, arr = intArray)}")
    println("Count  of 9 = ${countElements(target = 9, arr = intArray)}")
}

fun countElements(target: Int, arr: IntArray): Int {
    val firstIndex = findFirstOccurrence(target, arr)
    if (firstIndex == -1) return firstIndex
    val lastIndex = findLastOccurrence(target, arr)
    return lastIndex - firstIndex + 1
}```


### Find Floor in Array

```kotlin
package search.binary


fun main() {
    val intArray = intArrayOf(1, 2, 4, 6, 7, 8, 9)

    println("Floor of 5 = ${floorInArray(5, intArray)}")
}

fun floorInArray(num: Int, arr: IntArray): Int {
    var result = 0
    var left = 0
    var right = arr.size - 1

    while (left <= right) {
        val mid = (left + right).ushr(1)
        when {
            arr[mid] == num -> return mid
            arr[mid] < num -> { // move right
                result = mid
                left = mid + 1
            }
            else -> right = mid - 1 // move to left
        }
    }

    return arr[result]
}```


### Find Element in Rotated Array

```kotlin
package search.binary


fun main() {
//    val intArray = intArrayOf(11, 12, 15, 18, 2, 5, 6, 8)
    val intArray = intArrayOf(4, 5, 6, 7, 0, 1, 2)
    val target = 1

    println("Find rotation in sorted array ${finElementInRotatedArray(intArray, 1)}")
}

fun finElementInRotatedArray(arr: IntArray, target: Int): Int {
    val index = findRotationInSortedArray(arr) // we got the min element where array is pivoted

    if (index == -1) return findUsingBinarySearch(0, arr.size - 1, target, arr)
    if (arr[index] == target) return index
    val indexLeft = findUsingBinarySearch(0, index - 1, target, arr)
    val indexRight = findUsingBinarySearch(index + 1, arr.size - 1, target, arr)
    return if (indexLeft != -1) indexLeft else indexRight
}
```


### Find Rotation in Sorted Array

```kotlin
package search.binary

fun main() {
//    val intArray = intArrayOf(12, 15, 18, 2, 5, 6, 8, 11)
    val intArray = intArrayOf(4, 5, 6, 7, 0, 1, 2)

    println("Find rotation in sorted array ${findRotationInSortedArray(intArray)}")
}

/**
 * One property for sorted array is that first element is always smaller
 * than last element in case onf ascending sorted element
 *
 *  -- how to find the element in rotated sorted array
 */
fun findRotationInSortedArray(arr: IntArray): Int {

    var left = 0
    var right = arr.size - 1

    while (left <= right) {
        if (arr[left] <= arr[right]) return left
        val mid = left + (right - left) / 2

        if (arr[mid] <= arr[mid - 1] && arr[mid] <= arr[mid + 1]) {
            return mid
        }
        if (arr[mid] < arr[left]) { // if left is sorted then first element should be small than mid
            right = mid - 1
        } else {
            left = mid + 1
        }
    }
    return 0
}```


### Peak in Bitonic Array

```kotlin
package search.binary

fun main() {
    val intArray = intArrayOf(1, 2, 4, 6, 7, 8, 9, 5, 3)

    println("Search in Bitonic array ${intArray[peakInBitonicArray(arr = intArray)]}")
}

fun peakInBitonicArray(arr: IntArray): Int {
    var left = 0
    var right = arr.size - 1

    while (left < right) {
        val mid = (left + right).ushr(1)
        if (arr[mid] < arr[mid + 1]) {
            // Peak is in the right half
            left = mid + 1
        } else {
            // Peak is in the left half (including mid)
            right = mid
        }
    }

    return left
}```


### Search in Bitonic Array

```kotlin
package search.binary

fun main() {
    val intArray = intArrayOf(1, 2, 4, 6, 7, 8, 9, 5, 3)

    println("Search in Bitonic array ${searchBitonicArray(6, arr = intArray)}")
}

fun searchBitonicArray(target: Int, arr: IntArray): Int {

    val peakElement = peakInBitonicArray(arr)

    val left = findUsingBinarySearch(0, peakElement - 1, target, arr)
    val right = findUsingBinarySearch(peakElement, arr.size - 1, target, arr)

    return  if(left != -1) left else right
}```


## 13.14 Sliding Window

### Sliding Window Concepts

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




### Minimum Size Subarray Sum

```kotlin
package sliding_window

fun main() {
    /**
     * https://leetcode.com/problems/minimum-size-subarray-sum/description/?envType=problem-list-v2&envId=sliding-window
     */
    val num1 = intArrayOf(2, 3, 1, 2, 4, 3)
    val target1 = 7
    println("containsNearbyDuplicate target  $num1  $target1 o/p = 2 =  ${minSubArrayLen(7, num1)}")
}

fun minSubArrayLen(target: Int, nums: IntArray): Int {
    var left = 0
    var right = 0
    var sum = 0
    var minLength = Int.MAX_VALUE

    while (right < nums.size) {
        sum += nums[right] // Expand window

        // Shrink window when sum ≥ target
        while (sum >= target) {
            minLength = minOf(minLength, right - left + 1)
            sum -= nums[left] // Shrink from left
            left++
        }
        right++
    }

    return minLength
}```


### Max Consecutive Ones III

```kotlin
package sliding_window

fun main() {

    /**
     * https://leetcode.com/problems/max-consecutive-ones-iii/description/
     */
    println("Max Ones ${longestOnes(intArrayOf(1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0), 2)}")
}



fun longestOnes(nums: IntArray, k: Int): Int {
    var zeroCount = 0
    var maxLen = 0
    var left = 0
    var right = 0
    while (right < nums.size) {
        if (nums[right] == 0) {
            zeroCount++
        }

        if (zeroCount <= k) {
            maxLen = maxOf(maxLen, right - left + 1)
        } else {
            if(nums[left] == 0){
                zeroCount--
            }
            left++
        }
        right++
    }
    return maxLen
}

fun longestOnesWithLoop(nums: IntArray, k: Int): Int {
    var zeroCount = 0
    var maxLen = 0
    var left = 0
    var right = 0
    while (right < nums.size) {
        if (nums[right] == 0) {
            zeroCount++
        }

        while (zeroCount > k) {
            if (nums[left] == 0) zeroCount--
            left++
        }

        maxLen = maxOf(maxLen, right - left + 1)
        right++
    }
    return maxLen
}```


### Contains Duplicate II

```kotlin
package sliding_window

import java.util.*

fun main() {
    /**
     * https://leetcode.com/problems/contains-duplicate-ii/description
     */
    println("containsNearbyDuplicate (1,2,3,1) 3  true =  ${containsNearbyDuplicate(intArrayOf(1, 2, 3, 1), 3)}")
    println("containsNearbyDuplicate (1,0,1,1) 1 true =  ${containsNearbyDuplicate(intArrayOf(1, 0, 1, 1), 1)}")
    println(
        "containsNearbyDuplicate (1,2,3,1,2,3) 2 false =  ${
            containsNearbyDuplicate(
                intArrayOf(1, 2, 3, 1, 2, 3),
                2
            )
        }"
    )
    println("containsNearbyDuplicate (1,2,1) 0 false=  ${containsNearbyDuplicate(intArrayOf(1, 2, 1), 0)}")
    println(
        "containsNearbyDuplicate (0,1,2,3,2,5) 3 true=  ${
            containsNearbyDuplicate(
                intArrayOf(0, 1, 2, 3, 2, 5),
                3
            )
        }"
    )
}


fun containsNearbyDuplicate(nums: IntArray, k: Int): Boolean {
    // Map that tracks num -> last seen index
    val map = HashMap<Int, Int>()
    for (i in nums.indices) {
        if (map.containsKey(nums[i])) {
            val lastSeenIndex = map[nums[i]]!!
            if (Math.abs(i - lastSeenIndex) <= k) return true
        }
        map[nums[i]] = i
    }
    return false
}

```


### Max Substring Without Repeating Characters

```kotlin
package sliding_window

fun main() {
    /**
     * https://leetcode.com/problems/longest-substring-without-repeating-characters/
     */
//    val str = "aaabcdd"
//    val str = "abcabcbb"
    val str = "bbbbb"
    println("Long = ${lengthOfLongestSubstring(str)}")
}

fun lengthOfLongestSubstring(s: String): Int {
    val charSet = mutableSetOf<Char>()
    var result = 0
    var left = 0
    var right = 0

    while (s.length > right) {
        if (charSet.contains(s[right])) {
            charSet.remove(s[left])
            left++
        } else {
            result = maxOf(result,right - left + 1)
            charSet.add(s[right])
            right++
        }
    }
    return result
}```


### Negative Number in Every Subarray

```kotlin
package sliding_window

import java.util.LinkedList

fun main() {
    val nums = intArrayOf(12, -1, -7, 8, -15, 30, 16, 28)
    val k = 3
    println(firstNegativeInSubArray(nums, k)) // Output: [-1, -1, -7, -15, -15, 0]

}


fun firstNegativeInSubArray(nums: IntArray, k: Int): List<Int> {

    val result = mutableListOf<Int>()
    val queue = LinkedList<Int>()

    var start = 0
    var end = 0

    while (end < nums.size) {
        if (nums[end] < 0) queue.add(nums[end])
        if (end < k - 1) {
            end++
            continue
        }
        result.add(if (queue.isNotEmpty()) queue.first else 0)
        end++
        if (queue.isNotEmpty() && nums[start] < 0) queue.pollFirst()
        start++
    }
    return result
}```


## 13.15 Sorting

### Bubble Sort

# Bubble sort

Iterate through the array repeatedly, comparing adjacent pairs of elements and swapping them if they are in the wrong
order. Repeat until the array is fully sorted.
At every pass it pushed the largest element to last correct position

### Code

```kotlin
fun bubbleSort(arr: IntArray) {
    val n = arr.size

    for (i in 0 until n - 1) {
        for (j in 0 until n - i - 1) {
            if (arr[j] > arr[j + 1]) {
                // Swap the elements
                val temp = arr[j]
                arr[j] = arr[j + 1]
                arr[j + 1] = temp
            }
        }
    }
}
```

### Bubble Sort Time Complexity

| **Case**     | **Time Complexity**    |
|--------------|------------------------|
| Worst-case   | O(n^2)                 |
| Average-case | O(n^2)                 |
| Best-case    | O(n) - array is sorted |


### Selection Sort

# Selection sort

Selection sort is a simple sorting algorithm that works by repeatedly finding the minimum element from the unsorted
portion of the array and swapping it with the first unsorted element.

### Code

```kotlin
fun selectionSort(arr: IntArray) {
    val n = arr.size

    for (i in 0 until n - 1) {
        var minIndex = i
        // Find the index of the minimum element in the unsorted portion
        for (j in i + 1 until n) {
            if (arr[j] < arr[minIndex]) {
                minIndex = j
            }
        }

        // Swap the minimum element with the first unsorted element
        if (minIndex != i) {
            val temp = arr[i]
            arr[i] = arr[minIndex]
            arr[minIndex] = temp
        }
    }
}
```

### Selection Sort Time Complexity

| **Case**     | **Time Complexity** |
|--------------|---------------------|
| Worst-case   | O(n^2)              |
| Average-case | O(n^2)              |
| Best-case    | O(n^2)              |



### Insertion Sort

# Insertion sort


Build up a sorted sub-array from left to right by inserting each new element into its correct position in the subarray. Repeat until the array is fully sorted.


### Code
```kotlin
fun insertionSort(arr: IntArray) {
    val n = arr.size
    for (i in 1 until n) {
        val key = arr[i]
        var j = i - 1
        while (j >= 0 && arr[j] > key) {
            arr[j + 1] = arr[j]
            j--
        }
        arr[j + 1] = key
    }
}
```


### Insertion  Sort Time Complexity

| **Case**     | **Time Complexity**    |
|--------------|------------------------|
| Worst-case   | O(n^2)                 |
| Average-case | O(n^2)                 |
| Best-case    | O(n) - array is sorted |


## 13.16 Bit Manipulation

### Bitwise Operators

# Bitwise Operator

### Binary number representation]

**Example 1**
> 8 in binary **1 0 0 0**    
> = 1 x (2^3) + 0 x (2^2) + 0 x (2^1) + 0 x (2^0)   
> = 8 + 0 + 0 + 0


**Example 2**
> 6 in binary **1 1 0**     
> = 1 x (2^2) + 1 x (2^1) + 0 x (2^0)   
> = 4 + 2 + 0

**Example 3**
> 9 in binary  **1 0 0 1**     
> = 1 x (2^3) + 0 x (2^2) + 0 x (2^1) + 1 x (2^0)   
> = 8 + 0 + 0 + 1


---

### AND (&)

Returns a bit-wise AND of two integers. Each bit of the result is 1 if both corresponding bits of the operands are 1;
  otherwise, it's 0.


| Operand 1 | Operand 2 | Result |
|-----------|-----------|--------|
| 0         | 0         | 0      |
| 0         | 1         | 0      |
| 1         | 0         | 0      |
| 1         | 1         | 1      |


**Note: '&' is useful in finding mod value** 

    9 % 10 == 9 & 9
    9 & 3 == 9 % 4


---

### OR (|)

Returns a bit-wise OR of two integers. Each bit of the result is 0 if both corresponding bits of the operands are 0;
  otherwise, it's 1.

| Operand 1 | Operand 2 | Result |
|-----------|-----------|--------|
| 0         | 0         | 0      |
| 0         | 1         | 1      |
| 1         | 0         | 1      |
| 1         | 1         | 1      |


---

### XOR (^)

- Returns a bit-wise XOR (exclusive OR) of two integers. Each bit of the result is 1 if the corresponding bits of the
  operands are different; otherwise, it's 0.

| Operand 1 | Operand 2 | Result |
|-----------|-----------|--------|
| 0         | 0         | 0      |
| 0         | 1         | 1      |
| 1         | 0         | 1      |
| 1         | 1         | 0      |


**Note**

1. **Number XOR with same number will be Zero (5^5 = 0)**
2. **Any number XOR with zero will be same number (5^0 = 5)**
---

### Negation (~)

- Inverts all the bits of its operand. If the operand is `x`, then `~x` is equal to `-x - 1`.

| Operand | Result | 
|---------|--------|
| 0       | -1     |
| 1       | -2     |
| 2       | -3     |
| ...     | ...    |



---

### Left Shift (<<)
**8 in binary is 1000** 

**Example 1**
> 1000 << 1 = 10000 (16)

**Example 2**
> 1000 << 2 = 100000 (32)

 **Note: Left shift by 1 mean multiply by 2**

---

### Right Shift (>>)
**8 in binary is 1000**

**Example 1**
>1000 >> 1 = 0100 (4)

**Example 2**
>1000 >> 2 = 0010 (2)

### Note: Right shift by 1 means dividing by 2

---

### Addition in Binary numbers

Each bit is added according to standard binary addition rules, where:

- 0 + 0 = 0
- 0 + 1 = 1
- 1 + 0 = 1
- 1 + 1 = 10 (carry 1 to the next higher bit)

|  Binary   | Decimal |
|:---------:|:-------:|
|   0011    |    3    |
|  + 0101   |    5    |
| --------- | ------- |
|   1000    |    8    |

### Subtraction in Binary numbers

To do subtraction **9 and 3** we need to take **2's** compliment of the second number

- Step 1: Negate all bit (0011) = 1100 **(4 digit if required prefix 1 to form 8 digit)**
- Step 2: Add **1** to it become 1101
- Step 3: Add binary both numbers

|  Binary   | Decimal |
|:---------:|:-------:|
|   1001    |    9    |
|  - 1101   |    3    |
| --------- | ------- |
|   0110    |    6    |




## 13.17 DS Quick Reference & Cheat Sheet

### Quick Methods for Data Structures




## Stack

| Stack Operation | Kotlin Code using `ArrayDeque`     |
|-----------------|------------------------------------|
| Push            | `addLast(element)`  `add(element)` |
| Pop             | `removeLast()`                     |
| Peek            | `lastOrNull()`                     |
| Is Empty        | `isEmpty()`                        |
| Size            | `size`                             |
| Contains        | `contains(element)`                |
| Clear           | `clear()`                          |


## Queue

| Operation | Purpose                          | Method Used on `ArrayDeque`       |
|-----------|----------------------------------|-----------------------------------|
| Enqueue   | Add element to the end (tail)    | `addLast(element)` `add(element)` |
| Dequeue   | Remove element from front (head) | `removeFirst()`                   |
| Peek      | View front element               | `firstOrNull()`                   |
| Is Empty  | Check if queue is empty          | `isEmpty()`                       |
| Size      | Number of items in queue         | `size`                            |
| Contains  | Check if an item exists          | `contains(element)`               |
| Clear     | Remove all items                 | `clear()`                         |


## Char array

1-D

```kotlin
val size = 5
val arr = CharArray(size)  // Default values are '\u0000' (null char)
arr[0] = 'X'

```

2-D
```kotlin
val rows = 3
val cols = 4

val char2D = Array(rows) { CharArray(cols) }

```

### Declarations & Misc Utilities

```kotlin
package cheestsheet

import java.util.Stack

fun main() {


    val stack = ArrayDeque<Int>()
    stack.add(10)
    stack.add(20)
    stack.add(30)


    println("Stack= $stack")

    println("Stack last = ${stack.lastOrNull()}")
    println("Stack top = ${stack.removeLast()}")

    println("Stack= $stack")

    println("Stack is empty ${stack.isEmpty()}")


    /***
     *
     */

}```


---

# 14. System Design (Low-Level Design)

## 14.1 Parking Lot System

### Requirements

# Parking lot system


- The parking lot should have the capacity to park 40,000 vehicles. 
- The four different types of parking spots are handicapped, compact, large, and motorcycle. 
- The parking lot should have multiple entrance and exit points. 
- Four types of vehicles should be allowed to park in the parking lot, which are as follows:
  - Car 
  - Truck 
  - Van 
  - Motorcycle 
- The parking lot should have a display board that shows free parking spots for each parking spot type.
- The system should not allow more vehicles in the parking lot if the maximum capacity (40,000) is reached. 
- If the parking lot is completely occupied, the system should show a message on the entrance and on the parking lot display board. 
- Customers should be able to collect a parking ticket from the entrance and pay at the exit. 
- The customer can pay for the ticket either with an automated exit panel or pay the parking agent at the exit. 
- The payment should be calculated at an hourly rate. 
- Payment can be made using either a credit/debit card or cash.

### Use Case & Class Diagram

# Car Rental  System

## Usecase Diagram
### Actors
1. Customer
2. Parking Agent
3. System
4. Admin


[PlantUML for Usecase](https://www.plantuml.com/plantuml/uml/ZPBFRjim3CRlUWgYEJkjhQ1TXo1eaEiEnR8z02lHZX1PCYGvHVRdtISxbMS17Z0PCFWHFtsaGBxuW2xqhMQQwW31WbFDAO1KZaHGrZ0KmJguErvzeFOEq8DFkRribHbJ-94wz3xObjoG4mbxHNTMfe5zGoQCU8Si7c133JIDfh-GjX61rU5X-7v627fF0dr4ZYVx3liBAerlcaQZxmTUmkgn5kAvdgDxxrLZuDZPaBXgYVjAhRqGfE9-ndDOVUAIruyiQ-8RdWc2EWjAP1axg6lnzY3ct2jU8SVu4jR5od2XanAQ4bWbC7-zG1CTnNboh6BX9tn-4ZZd3kXF2QWM0KTI1H3ePEBunB6Rxtgz_hMxDVqPOBjLHkXUaj_jsCst6DQbA3Ae8KCEjVg1m-9yeFp_K9PDJygbaPJJH79sjbYnu3-W-zPALL_1OI2VPhp4Vh6DClVnR_lakPauzfinDYxTjkYx0lmWghdWauX7G27jkod6fvUPh79PLhdaZ7MOtyvqDUdfVgvvhZT3swbhZ74rY_aitamnOozaPD_gFm00)

![Alt text](https://www.plantuml.com/plantuml/png/ZPBFRjim3CRlUWgYEJkjhQ1TXo1eaEiEnR8z02lHZX1PCYGvHVRdtISxbMS17Z0PCFWHFtsaGBxuW2xqhMQQwW31WbFDAO1KZaHGrZ0KmJguErvzeFOEq8DFkRribHbJ-94wz3xObjoG4mbxHNTMfe5zGoQCU8Si7c133JIDfh-GjX61rU5X-7v627fF0dr4ZYVx3liBAerlcaQZxmTUmkgn5kAvdgDxxrLZuDZPaBXgYVjAhRqGfE9-ndDOVUAIruyiQ-8RdWc2EWjAP1axg6lnzY3ct2jU8SVu4jR5od2XanAQ4bWbC7-zG1CTnNboh6BX9tn-4ZZd3kXF2QWM0KTI1H3ePEBunB6Rxtgz_hMxDVqPOBjLHkXUaj_jsCst6DQbA3Ae8KCEjVg1m-9yeFp_K9PDJygbaPJJH79sjbYnu3-W-zPALL_1OI2VPhp4Vh6DClVnR_lakPauzfinDYxTjkYx0lmWghdWauX7G27jkod6fvUPh79PLhdaZ7MOtyvqDUdfVgvvhZT3swbhZ74rY_aitamnOozaPD_gFm00)

---


## Class Diagram

[PlantUML for Class](https://www.plantuml.com/plantuml/uml/TLF1Jjn03BtdAtmCXVn0j5gnfQrQ2H0IYBitSSE8P2OQEz0LzD-xJYP39bjoyZmxzhEVVSc2NeQkpN3Fuh4IUAGdKxK4cyqCRRTl6HJWgRbr9ToBDxR9u1oGsJJsmLJF9FbPzZVBuh_lHG5Nw2557lnGFQ-mHxGfSEF4-UfmV6V7BzAwG_zyh7lVEpcbPkeIVbbHyxSdAc7lN4je5NW9oSkvCwLk-8SdcaXdIMgbS8s-eV5vwlk9jZOLzZtLdqHSkQx7IXH8dMbtYgMrYs8REMPPrI8pVBVYqLQKjdaE3SbYy37wZv5rv0ksfaQXHNY8_cQuR_7mrQ6lrsFiFtWnb723_MOI_WAk3SjcmNkxrJByv5vrfehaPn4PsZQ4tX0pDZJviAuN6NBkdLo0cW-7dfJYGlbTLRd1_aVrWSdVOaUwayRRPkBD_EfylTnJJqno5npv6OpfOzRtOkWBsDMTiR160yTTGrR2rAR8jz34SeHvFJrA7UmCVjOjbB20vgXPxPo2KS8CpI4gSKwraHBKNCYv93lowxshY7aM7k9Pw3_fJrFCE5KPpLiNLq5CHopOzSV6nZtgI64wLayHxDnW5NF3lgL9ZKD72jrDXXQEkWPiizdjSOI2hATfM506ZbdknsT23IAJwRojaH0SRrnVyuKYFwqGhoEzGb-l9tFjvek2E16hInSNh51PFB7UoMwomeM650i14aI9VLI1BqKHrLxZAlKQItEjdN6vrgxaxBCioQB-gDNolwBOJek_1jCBkIHR3rtx3m00)

![Alt text](https://www.plantuml.com/plantuml/png/TLF1Jjn03BtdAtmCXVn0j5gnfQrQ2H0IYBitSSE8P2OQEz0LzD-xJYP39bjoyZmxzhEVVSc2NeQkpN3Fuh4IUAGdKxK4cyqCRRTl6HJWgRbr9ToBDxR9u1oGsJJsmLJF9FbPzZVBuh_lHG5Nw2557lnGFQ-mHxGfSEF4-UfmV6V7BzAwG_zyh7lVEpcbPkeIVbbHyxSdAc7lN4je5NW9oSkvCwLk-8SdcaXdIMgbS8s-eV5vwlk9jZOLzZtLdqHSkQx7IXH8dMbtYgMrYs8REMPPrI8pVBVYqLQKjdaE3SbYy37wZv5rv0ksfaQXHNY8_cQuR_7mrQ6lrsFiFtWnb723_MOI_WAk3SjcmNkxrJByv5vrfehaPn4PsZQ4tX0pDZJviAuN6NBkdLo0cW-7dfJYGlbTLRd1_aVrWSdVOaUwayRRPkBD_EfylTnJJqno5npv6OpfOzRtOkWBsDMTiR160yTTGrR2rAR8jz34SeHvFJrA7UmCVjOjbB20vgXPxPo2KS8CpI4gSKwraHBKNCYv93lowxshY7aM7k9Pw3_fJrFCE5KPpLiNLq5CHopOzSV6nZtgI64wLayHxDnW5NF3lgL9ZKD72jrDXXQEkWPiizdjSOI2hATfM506ZbdknsT23IAJwRojaH0SRrnVyuKYFwqGhoEzGb-l9tFjvek2E16hInSNh51PFB7UoMwomeM650i14aI9VLI1BqKHrLxZAlKQItEjdN6vrgxaxBCioQB-gDNolwBOJek_0jCBkIHR3rtx3m00)


### Implementation Code

# Car Rental  System - Code


```kotlin
import java.util.*
import kotlin.collections.HashMap

class ParkingLot(
    val regNo: String,
    val name: String,
    val address: Address,
    val parkingTickets: HashMap<Int, ParkingTicket>,
    private val parkingSpots: List<ParkingSpot>,
    val entrance: HashMap<Int, Entrance>,
    val exit: HashMap<Int, Exit>,
    val displayBoards: HashMap<Int, DisplayBoard>,
) {
    private var parkingTicketCounter = 0
    fun getParkingTickets(vehicle: Vehicle): ParkingTicket {
        val isFull = when (vehicle) {
            is Car -> isFull(CompactSpot())
            is Van -> isFull(HandicappedSpot())
            is Truck -> isFull(LargeSpot())
            is MotorCycle -> isFull(MotorcycleSpot())
            else -> {
                false
            }
        }

        val parkingTicket = ParkingTicket(
            ticketNo = parkingTicketCounter++,
            timestamp = Date(),
            amount = 0.0,
            payment = null
        )
        vehicle.assignTicket(parkingTicket)
        parkingSpots.find { it.isFree() }?.markAllocated()
        return parkingTicket

    }

    fun addEntrance(entrance: Entrance) {

    }

    fun addExit(exit: Exit) {

    }

    fun isFull(parkingSpot: ParkingSpot): Boolean {
        return parkingSpots.find { it.isFree() } != null
    }
}

class Entrance
class Exit
class DisplayBoard
abstract class ParkingSpot {
    protected var id: Int = 0
    protected var free: Boolean = false
    protected var vehicle: Vehicle? = null
    abstract fun isFree(): Boolean
    abstract fun markAllocated()
}

class LargeSpot : ParkingSpot() {
    override fun isFree(): Boolean {
        TODO("Not yet implemented")
    }

    override fun markAllocated() {
        TODO("Not yet implemented")
    }

}

class HandicappedSpot : ParkingSpot() {
    override fun isFree(): Boolean {
        TODO("Not yet implemented")
    }

    override fun markAllocated() {
        TODO("Not yet implemented")
    }

}

class CompactSpot : ParkingSpot() {
    override fun isFree(): Boolean {
        TODO("Not yet implemented")
    }

    override fun markAllocated() {
        TODO("Not yet implemented")
    }
}

class MotorcycleSpot : ParkingSpot() {
    override fun isFree(): Boolean {
        TODO("Not yet implemented")
    }

    override fun markAllocated() {
        TODO("Not yet implemented")
    }
}

abstract class Vehicle {
    val regNo: String = ""
    abstract fun assignTicket(parkingTicket: ParkingTicket)
}

class Car : Vehicle() {
    override fun assignTicket(parkingTicket: ParkingTicket) {
        TODO("Not yet implemented")
    }
}

class Van : Vehicle() {
    override fun assignTicket(parkingTicket: ParkingTicket) {
        TODO("Not yet implemented")
    }
}

class Truck : Vehicle() {
    override fun assignTicket(parkingTicket: ParkingTicket) {
        TODO("Not yet implemented")
    }
}

class MotorCycle : Vehicle() {
    override fun assignTicket(parkingTicket: ParkingTicket) {
        TODO("Not yet implemented")
    }
}

class ParkingTicket(
    val ticketNo: Int,
    val timestamp: Date,
    var amount: Double,
    var payment: Payment?
) {
    fun calculateAmount(): Double {
        TODO("Not yet implemented")
    }
}

abstract class Payment {

}

class Address(
    val address: String,
    val pinCode: Int,
    val city: String,
    val state: String,
    val country: String
)

abstract class Account {
    protected var userName: String = ""
    protected var password: String = ""
    protected abstract fun resetPassword()
}

class Admin : Account() {
    override fun resetPassword() {

    }
}

class ParkingAgent : Account() {
    override fun resetPassword() {

    }
}
```

## 14.2 Library Management System

### Requirements

# Library Management System

1. Any library member be it a customer or a librarian should be able to search books by their title, author, subject category as well by the publication date. 
2. Each book will have a unique identification number and other details including a rack number which will help to physically locate the book. 
3. There could be more than one copy of a book, and library members should be able to check-out and reserve any copy. We will call each copy of a book, a book item. 
4. The system should be able to retrieve information like who took a particular book or what are the books checked-out by a specific library member. 
5. There should be a maximum limit (5) on how many books a member can check-out. 
6. There should be a maximum limit (10) on how many days a member can keep a book. 
7. The system should be able to collect fines for books returned after the due date.
8. Members should be able to reserve books that are not currently available. 
9. The system should be able to send notifications whenever the reserved books become available, as well as when the book is not returned within the due date. 
10. Each book and member card will have a unique barcode. The system will be able to read barcodes from books and members' library cards.

### Use Case & Class Diagram

# Library management

## Usecase Diagram

[PlantUML for Usecase](https://www.plantuml.com/plantuml/uml/RPBFZjCm48VlVehHzh8zB0fDjtyue6KhRY0a40ymiIUjjOwDx74X0jvzdMvYELBSowUPRtvJ9taAZ9vZPuIXbe4TU7s-C2ZjIR9sLg1av-4hTGrvm01nhDnzqOr7hz7U3MNJJFKVVM3gRiKWn123zcm8tlmUO7MgXjzV0H03IGoKXWYzlC2pSo-toP-dzx0wrAfkTqcxV-w1DHjQmyF3luzmdzfh61iO-UByKaSYqzdvlkoLIqyNaYykShbsKwpDudUozBkqjiiMHszBhIxly0jxQBMbDRox_PTw3H-0_Z1P5SGy9v2_qknXL3cfD3yfLLgFi6fRsLHodjQvUTXkIVkiD9VIFakbDGY0LzG66qDW7UjMInm-b77k0Aiz7lSIox5lL_8gBlh7x8l_8frqr_Ism3G3LHdhJ8-PTfdscJQPjYC94GTAQHcgYJOJr1Dian_Uy331KQJpH5R5phm2#google_vignette)

![Alt text](https://www.plantuml.com/plantuml/png/RPBFZjCm48VlVehHzh8zB0fDjtyue6KhRY0a40ymiIUjjOwDx74X0jvzdMvYELAIowUPRtuJ9taAZ9vZPuIXbe4TU7s-C2ZjIR9sLg1av-4hTGrvm01nhDnzqOr7hz7U3MNJJFKVVM3gRiKWn123zcm8tlmUO7MgXlkl08Y190PAGuHUNk3PkPVRvC_JUrWTQbMtkwJTF_V0cWsjuU7XtqUuJ-qrZ0sCV75-gIEHwUnyN_RAfQSBoHSNkLoxATPcyJjP-brQssMBexUbLfTd-8KzjDhI6jxTdgLUmmUWFqnM1J7F2UIlD7iPLKugpKzAbTOZhDfMDfMSftLk7hPRqZuhpQMqpr9f3GBW5RN1nX1OnxhL4eSVPPmxm6gFnxt4SkpRbRoAY_un--A_I5SwQ_fQO9g1genrfiTCkqpxJ9jCsv64Y0Ebj0pLH9i9wWcsoGzlU9ZWA9x8gjYPLm00)

---


## Class Diagram

[PlantUML for Class](https://www.plantuml.com/plantuml/uml/hLLFZvi-4BtpAQpyBiclvT0hgg8bVrP5OZSLP5VggN9W5gm0ZcpJBKhtkzVZO4oIRdhf1UpncpVFRsouaueArTQLbrLKIhBWV7_qY4zO7f2eKJXKJ5KGa5G9rXGOeAqgkP01YPbKdqCpc-D2HXKKN7I1uTbs1_2yDy-3fgs7Y6PFbz7t--YZHvBDG_YuZPPw-6MJ99j43rP9-13gzxrU2TDlcE-aHGfg8sz7HSRpCrM2Plk09FePSos4ySO0TLQgg6ebYh9ZiqCfMrXHfKdmgGlPIZ7R2IewKwYXDU2xwZa3SibkVR3wvfvFMWd6dqZ7qqxgw1D6j4ckYkKnPMYU2v1ol9-CgMuybyfeTO43QvReWUcJutMxrCpXSnZ5uI9UQqVZJRf5qzVfEdbUh_GmIjEdzSfx8tsgCS_9qctRWJZnEmEi0bsnDuhu5Rmetv-JU02LL18w02QV9iHl3xx_FvdKjEacGpTx925JBO1dpAJFhXEvJNHssjGfBGe1HP-9nCGNh2XHqN3ih29RTmHnXbs2tCR6pboZKE2l4n492FnKqEJIh3k6qpMnkRtXk0rjz7_aXGcf7cdj6go35RsErIpFAnX7tGrnrk2PWfgoogNwXv8ty7XggOkoV0pHBAzSvDVK3u1vI4iKLxGwyU0EvRA4RCzRDNml9D1CP6KAuWVBK2PGDxMt9iBRDq0M5n5RsDv5HflZlw8c7_2sONZAp06qVPoPOGMqGKmE5IZuAmnofju5cQ7Ro-a87ArE-RNTLKoMa8--87AEUC-rSZXTE767opJ4BJgyWmpMtKGPKUzLSHn-pbyRFCTZo-sjwjNSndX_O7FTkK5u0sQTFIjunBuKODgowBRuzvgU_SDcPm1j01TJYi-0cCdvEduzfae_t5zjvdfwgUoeb-wqTsrT_GO0)

![Alt text](http://www.plantuml.com/plantuml/png/hLLFZvi-4BtpAQpyBiclvT0hgg8bVrP5OZSLP5VggN9W5gm0ZcpJBKhtkzVZO4oIRdhf1UpncpVFRsouaueArTQLbrLKIhBWV7_qY4zO7f2eKJXKJ5KGa5G9rXGOeAqgkP01YPbKdqCpc-D2HXKKN7I1uTbs1_2yDy-3fgs7Y6PFbz7t--YZHvBDG_YuZPPw-6MJ99j43rP9-13gzxrU2TDlcE-aHGfg8sz7HSRpCrM2Plk09FePSos4ySO0TLQgg6ebYh9ZiqCfMrXHfKdmgGlPIZ7R2IewKwYXDU2xwZa3SibkVR3wvfvFMWd6dqZ7qqxgw1D6j4ckYkKnPMYU2v1ol9-CgMuybyfeTO43QvReWUcJutMxrCpXSnZ5uI9UQqVZJRf5qzVfEdbUh_GmIjEdzSfx8tsgCS_9qctRWJZnEmEi0bsnDuhu5Rmetv-JU02LL18w02QV9iHl3xx_FvdKjEacGpTx925JBO1dpAJFhXEvJNHssjGfBGe1HP-9nCGNh2XHqN3ih29RTmHnXbs2tCR6pboZKE2l4n492FnKqEJIh3k6qpMnkRtXk0rjz7_aXGcf7cdj6go35RsErIpFAnX7tGrnrk2PWfgoogNwXv8ty7XggOkoV0pHBAzSvDVK3u1vI4iKLxGwyU0EvRA4RCzRDNml9D1CP6KAuWVBK2PGDxMt9iBRDq0M5n5RsDv5HflZlw8c7_2sONZAp06qVPoPOGMqGKmE5IZuAmnofju5cQ7Ro-a87ArE-RNTLKoMa8--87AEUC-rSZXTE767opJ4BJgyWmpMtKGPKUzLSHn-pbyRFCTZo-sjwjNSndX_O7FTkK5u0sQTFIjunBuKODgowBRuzvgU_SDcPm1j01TJYi-0cCdvEduzfae_t5zjvdfwgUoeb-wqTsrT_GO0)


### Implementation Code

# Library Management System - Code


```agsl
package system_design.library_management

import java.util.Date


data class Library(private val name: String, private val location: Location, private val books: List<Book>)

class Location(
    private val address: String,
    private val city: String,
    private val state: String,
    private val pinCode: Int,
)

class RackLocation(private val number: Int, location: String)
class BookItem(
    private val id: Int,
    private val title: String,
    private val authors: List<Author>,
    private val type: BookType,
    private val barcode: String,
    private val rack: RackLocation,
    private val bookStatus: BookStatus,
    private val issueDate: Date
) : Book(id, title, authors, type)

enum class BookType {
    SCI_FI, ROMANTIC, HORROR, DRAMA, FANTASY
}

enum class BookStatus {
    AVAILABLE, LOST, RESERVED, ISSUED
}


open class Book(
    private val id: Int,
    private val title: String,
    private val authors: List<Author>,
    private val type: BookType
)

open class User {
    private val firstName: String = ""
    private val middleName: String = ""
    private val lastName: String = ""
}

class Author(private val publishedBooks: List<Book>) : User()

open class SystemUser : User() {
    private val email: String = ""
    private val phoneNumber: String = ""
    private val id: String = ""
    private val password: String = ""
}


class Member() : SystemUser() {
    private var totalNumberOfBooksCheckout: Int = 0
    private val searchService: SearchService? = null
    private val bookIssueService: BookIssueService? = null
}

class Librarian : SystemUser() {
    private val searchService: SearchService? = null
    private val bookIssueService: BookIssueService? = null
    fun addBook(item: BookItem): Boolean {

    }

    fun deleteBook(item: BookItem): Boolean {

    }

    fun editBook(item: BookItem): BookItem {

    }
}

class SearchService {
    fun getBookByTitle(title: String) {

    }

    fun getBookByAuthor(author: Author) {

    }

    fun getBookByType(type: BookType) {

    }
}

class BookIssueService {
    fun reserveBook(book: BookItem, user: SystemUser) {

    }

    fun getReservationDetails(book: BookItem): BookReservationDetails {

    }

    fun issueBook(book: BookItem, user: SystemUser): BookIssueDetails {

    }

    fun renewBook(book: BookItem, user: SystemUser): BookIssueDetails {

    }

    fun returnBook(book: BookItem, user: SystemUser){
        
    }
}

```

## 14.3 Car Rental System

### Requirements

# Car Rental  System

- Can be city based stores 
- Search a car based on location
- A system to store information about vehicles, customers, rentals, and payments 
- A reservation system to allow customers to reserve vehicles for specific dates and times 
- A billing system to calculate rental fees and process payments 
- An inventory management system to track the availability of vehicles and their current locations 
- A reporting system to provide insight into key metrics, such as utilization rates, revenue, and customer satisfaction

### Use Case & Class Diagram

# Car Rental  System

## Usecase Diagram

[PlantUML for Usecase](https://www.plantuml.com/plantuml/uml/RP7FQW8n48VlynI3UrwmhdyKIh4ilRPGyG76P5P3iuaaiuAKldkDh6659PTlXY-_8Pbj50hIjHOiDuBYCPZZIL2Rm4gCTq1AVC1zv80KiUlhoTUP0zreahhsy6Zlhb6ulJSZGCeWTxICave21dP25cFlO55Fq_a5n2woedZpTan1dV2ctqFszGiMwqfNpN9eVNFaSE61DXlJwXEh7wGB6Ki7OurSqJU3IzLujdJ18Gsy-PYdFPxfshAJtbfaQwYzQpr8McBHDEemLSyJM__qcDMOzQ6D90Sr2naRUtUTtUTlT1e_lPZ6A4fxxEqD5YlQh1J17q0gNyiI8oHU9awhJFCphXy8q65PlgMDP9fbgZBDCorWoqvthVq7)

![Alt text](http://www.plantuml.com/plantuml/png/RP7FQW8n48VlynI3UrwmhdyKIh4ilRPGyG76P5P3iuaaiuAKldkDh6659PTlXY-_8Pbj50hIjHOiDuBYCPZZIL2Rm4gCTq1AVC1zv80KiUlhoTUP0zreahhsy6Zlhb6ulJSZGCeWTxICave21dP25cFlO55Fq_a5n2woedZpTan1dV2ctqFszGiMwqfNpN9eVNFaSE61DXlJwXEh7wGB6Ki7OurSqJU3IzLujdJ18Gsy-PYdFPxfshAJtbfaQwYzQpr8McBHDEemLSyJM__qcDMOzQ6D90Sr2naRUtUTtUTlT1e_lPZ6A4fxxEqD5YlQh1J17q0gNyiI8oHU9awhJFCphXy8q65PlgMDP9fbgZBDCorWoqvthVq7)

---


## Class Diagram

[PlantUML for Class](https://www.plantuml.com/plantuml/uml/dLRDZk8u4BxdAIpj9Rr3U00qGXC2gq73mWXehYrtKXsiJUp8SUYDc7xtBPiYRMyZPcSl47_-wgkViYlvqXgkJLTNKLxnjcK7E8gyWdF44lQikImcR6UqaEN88hKge0eG3ULQNH4Bv2YtxnkOi8nh-s11-BiHkfzpWx3zZTwYA7IuD50xfvNAkH5AJjYAdaXrPxZfMYjBZov4ti8VN5jPrJrNwEmJU-aaAy5SIF631rqL4SG0FCyiwPea-J5bX5ttM5B1YqcItoyRl0AR4PkmNDMDQeM1KCLZNjpVeweXmmdZPOcLv0RQA0BPrO4_hCjkCK_N8_n_FE3lrtIVVNsQfTatN6JRpMxtj3aijj4R5NKGTXLj3Ue6BHAczzUDa9agaBAKnguhj3LTuHKLCINB47mB8gLyqM5wI9UhTBPQO6phpVvfytsnJVVBpJfT8R9Tx1RRmsAEZxFDvjjYRdKo5_HZ2ze5_29qQzQy3eAk-4UiLi-Y2X2ekQYkW7VATdrhe2OFDcK4NvKkSF695MgbIY5d_PzEBmvb7qBE8u2sSSGTYla1ZGRHTykX8LabG9fBEu3h_BW3VHAvDVQNxaWhzHSQ3mIgxoo0BrTFce6rVLzuNhdUl4gggf0dv2ZT3pPBXt2jUiyoUYyLI_1Qs5jX5z4bXU6clDEYgvjRUr4q6BhTQPWmteyZRzd0ATIhJ93rCUjRx4IBycYGJka792yignXa6n2fMXy9PpiwU57GlOrFb_-9FxFSUD5GgnFyb0Q5C9GNpC5mKRLnC3z7xBRjqD0XJGeEbVAkuWRiHbpWmrxKzehIquZPKuhoFVM3F73FXISVjCOweCCwwsVsLH07BuG7Vz-EgTZOiHzChe2JdZ1ZZbTBc9ucyCqcZ-tC7O-dVWqm9MX05YrJClhOoV7OdOGxDhTTaHdQVUTzZQA13LKbDFMhem5BUh_nr07lIhYQb62spjYDQJeCloAWmNHQ_W_FXS9KeJOoRgufK-uZnclL8K0JthKe4jA0Vk4vN6cVFm_GT7gEtBiLfL9d6z-Go3GK8kEw8076nbqp7l_mYYxa4RGmRVJOY7lR6M-Fz_QfOw57chivhl_rgN8wqJkFlb1uJvydxBUVfaMkCE0plKD_mShcSVul_2yWYwwk_W40)

![Alt text](http://www.plantuml.com/plantuml/png/dLRDZk8u4BxdAIpj9Rr3U00qGXC2gq73mWXehYrtKXsiJUp8SUYDc7xtBPiYRMyZPcSl47_-wgkViYlvqXgkJLTNKLxnjcK7E8gyWdF44lQikImcR6UqaEN88hKge0eG3ULQNH4Bv2YtxnkOi8nh-s11-BiHkfzpWx3zZTwYA7IuD50xfvNAkH5AJjYAdaXrPxZfMYjBZov4ti8VN5jPrJrNwEmJU-aaAy5SIF631rqL4SG0FCyiwPea-J5bX5ttM5B1YqcItoyRl0AR4PkmNDMDQeM1KCLZNjpVeweXmmdZPOcLv0RQA0BPrO4_hCjkCK_N8_n_FE3lrtIVVNsQfTatN6JRpMxtj3aijj4R5NKGTXLj3Ue6BHAczzUDa9agaBAKnguhj3LTuHKLCINB47mB8gLyqM5wI9UhTBPQO6phpVvfytsnJVVBpJfT8R9Tx1RRmsAEZxFDvjjYRdKo5_HZ2ze5_29qQzQy3eAk-4UiLi-Y2X2ekQYkW7VATdrhe2OFDcK4NvKkSF695MgbIY5d_PzEBmvb7qBE8u2sSSGTYla1ZGRHTykX8LabG9fBEu3h_BW3VHAvDVQNxaWhzHSQ3mIgxoo0BrTFce6rVLzuNhdUl4gggf0dv2ZT3pPBXt2jUiyoUYyLI_1Qs5jX5z4bXU6clDEYgvjRUr4q6BhTQPWmteyZRzd0ATIhJ93rCUjRx4IBycYGJka792yignXa6n2fMXy9PpiwU57GlOrFb_-9FxFSUD5GgnFyb0Q5C9GNpC5mKRLnC3z7xBRjqD0XJGeEbVAkuWRiHbpWmrxKzehIquZPKuhoFVM3F73FXISVjCOweCCwwsVsLH07BuG7Vz-EgTZOiHzChe2JdZ1ZZbTBc9ucyCqcZ-tC7O-dVWqm9MX05YrJClhOoV7OdOGxDhTTaHdQVUTzZQA13LKbDFMhem5BUh_nr07lIhYQb62spjYDQJeCloAWmNHQ_W_FXS9KeJOoRgufK-uZnclL8K0JthKe4jA0Vk4vN6cVFm_GT7gEtBiLfL9d6z-Go3GK8kEw8076nbqp7l_mYYxa4RGmRVJOY7lR6M-Fz_QfOw57chivhl_rgN8wqJkFlb1uJvydxBUVfaMkCE0plKD_mShcSVul_2yWYwwk_W40)


### Implementation Code

# Car Rental  System - Code


---

# 15. Git


## 15.1 Git Commands & Concepts

# GIT

### Clone

``git clone <repo-url>`` - Clone repo with entire history

``git clone --depth=1 <repo-url>`` - clone repo with last one commit history

``git checkout -b <branch-name> --single-branch <repo-url>`` - clone repo with specified branch only

## Status

``git status`` - prints current status of repo/ branch

## Checkout

``git checkout -b <branch-name>`` - checkout branch

``git switch <branch-name>`` - checkout branch

### Changes and Commit

``git add <file-name>`` - add files to staged area

``git commit -m "Message goes here"`` - add commit with message specified

``git commit --amend -m "Amend message"`` - Combines current and previous commit (it changes hashcode)

### Push

`` git push --set-upstream <remote> <branch name>`` - push and add tracking of local branch to remote

``git push origin`` -- push the changes to remote named origin you can specify branch name to push

``git push -f`` - force push the branch

### Log

``git log --oneline`` - Show one-line git logs

``git log --oneline --after = "last week"`` - shows logs that are committed after last week

``git log --oneline --grep="animation""`` - search for commit message that contains **animation**

``git log -n 2`` - shows last 2 commits only

### History re-write

``git reset --hard origin/main`` - uncommit + unstage + delete changes, nothing left.

``git reset --soft origin/main`` - uncommit changes, changes are left staged

``git rebase --interactice HEAD~N`` - squash last N commits

### Clean up files

`` git clean -nd`` - shows what files/directory going to be deleted in clean command

- -n list only files
- -nd list the directory

``git clean -xdf``

- The -x flag removes ignored files.
- The -d flag removes untracked folders.
- The -f flag removes untracked files.

### Stash

``git stash`` - Stash current changes (tracked files)
``git stash -u`` - Stash everything including untracked files
``git stash list`` - View all stash list
``git stash show`` - Show the changes in last stash
``git stash show -p`` - See full diff of latest stash
``git stash apply`` - Apply the latest stash, but keep it saved
``git stash apply stash@{1}`` - Apply a specific stash entry
``git stash pop`` - Apply and delete latest stash
``git stash drop stash@{0}`` - Delete a specific stash
``git stash clear`` - Delete all stashes






---

# 16. Behavioral Interview


## 16.1 Behavioral Round Preparation

# Behavioural round / Intro


### Tell me about yourself

**Example**

"I’m a seasoned Android developer with over 12 years of hands-on experience building scalable and user-centric mobile applications.
I my career i have worked with various domains like edtech, fintech, healthtech, and enterprise mobility, that leds to the strong foundation for quickly adapting different user needs and business models


My expertise in Android stack spans from core java, kotlin to modern frame work like Jetpack Compose,, coroutine and coin, I have been actively involved in architecture design, following clean code architecture and MVVM pattern to ensue the maintainable and testable code.

Apart from the coding, I have mentored junior developers, let the team during major app re-writes, involved in system design discussion. What excites me most is solving the real world problem through the technology and constant learning new tools and technology to say ahead in mobile space.

Currently I am looking for the more technical challenged , work with cutting edge technology and  contribute to product tha reached global scale , I find very aligned with your team at [Company name]

----------

### Why are you looking for the job change

**Example 1**

I have had fulfilling journey of past 5 year in ed tech domain, Where I contributed to  impactful product and grown technically and professionally. But i think growth comes with steeping out of comfort zone and taking new challenges.

[Company name] caught my attention because the scale at which the technology is applied to real world problem, especially in domain like Automotive. I am excited to work with cutting edge technology, collaborate with diverse team.  

So for me it's not leaving anything but learning new curve and contributing in high-impact environment. 

**Example 2**

I've been working in the EdTech domain for the past few years, and while it's been a valuable experience, the industry has been facing some challenges lately in terms of growth and stability. Over the last 5 years, I've gained solid experience, and I now feel ready to take on more challenging work that allows me to grow technically and expand my skill set. Unfortunately, my current organization doesn't support internal movement between teams, which limits my ability to explore new technologies or contribute in different capacities. I'm now looking for a role where I can work on impactful projects, keep learning, and stay aligned with the latest industry trends.

---------------

## Conflict between manager

**Example 1**

In my current organisation, I experienced the situation where me and my manager had different working style. 
I prefer more of independent approach, where I can plan and execute the task keeping stakeholder informed at key checkpoints.
My manager had more of hands-on style which sometimes felt like micromanagement.

At first it created some tension, as I felt it was impacting my productivity and confidence. 
Instead of letting the conflict grow. I decided to keep candid but respectful discussion with my manager. 
I shared how i work best when trusted with accountability and ownership, 
while also keeping in mind about their need to visibility and control.

To align better we decided to on regular daily standups ,  weekly connects and posting day work status before leaving the day.
This gave me the autonomy which i was looking for.
That small shift improved our collaboration significantly and strengthened our profession relationship.


What I learned from that experience is the value of proactive communication and adaptability—recognizing that conflict doesn’t have to be negative, but an opportunity for growth and better mutual understanding.

**Example 2**

In a previous role, I experienced some tension with my manager due to micromanagement. I prefer working with autonomy, while my manager had a more hands-on style. Instead of letting it affect work, I initiated an open conversation to align our expectations. We agreed on regular check-ins, which gave them visibility and gave me the space to work independently. It improved our collaboration, and I learned how effective communication can resolve most conflicts

----------

## How do you handle a conflict between two colleagues?

When I notice a conflict between colleagues, I first try to understand the root cause by listening to both sides without judgment. I believe most conflicts arise from miscommunication, so I focus on creating a space where each person can express their perspective clearly.

If needed, I help mediate by finding common ground and encouraging a solution-focused discussion. I avoid taking sides and instead guide the conversation toward what’s best for the team and the project.

In my experience, being calm, respectful, and neutral helps de-escalate tension and restore collaboration quickly."


---

> This document was auto-generated from the android-knowledge-mesh repository for interview preparation and RAG training.
