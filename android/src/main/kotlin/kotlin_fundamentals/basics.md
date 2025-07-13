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
