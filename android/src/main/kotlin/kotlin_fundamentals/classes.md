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
