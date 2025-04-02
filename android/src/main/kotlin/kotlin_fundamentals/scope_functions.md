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