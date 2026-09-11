# Decorator Pattern

- **Decorator is a Structural design pattern that lets you attach new behaviors to objects by placing them inside wrapper objects that contain those behaviors.**
- It is a flexible alternative to subclassing for extending functionality.

### Use Case
- Adding responsibilities to individual objects dynamically (e.g., `BufferedReader`, `BufferedWriter`)
- Android: adding decoration to views (e.g., `FrameLayout`, `ScrollView`)
- Logging, caching, encryption wrappers

### Steps to create Decorator

1. **Component** : Declare common interface for components and decorators.
2. **Concrete Component** : The base object that can be wrapped.
3. **Decorator** : Abstract class that holds a reference to a Component and delegates operations.
4. **Concrete Decorators** : Add specific responsibilities by overriding behavior.

### Pros
1. Add/remove responsibilities at runtime.
2. Avoids subclass explosion.
3. Follows Open/Closed Principle.

### Cons
1. Many small classes can make the design hard to understand.
2. Decorator chains can be confusing.

---

### UML

```
         +----------------+
         | <<Component>>  |
         | Component      |
         |----------------|
         | +operation()   |
         +----------------+
                ^       ^
                |       |
      +---------+---+  +----------------+
      | <<Concrete>> |  | <<Decorator>> |
      | ConcreteComp |  | Decorator    |
      |--------------|  |----------------|
      | +operation() |  | +operation() |
      +--------------+  +----------------+
                               ^
                               |
                    +----------+-----------+
                    | <<ConcreteDecorator>> |
                    | ConcreteDecorator    |
                    +----------------------+
```

---

### Kotlin code

Component interface

```kotlin
interface Coffee {
    fun cost(): Int
    fun description(): String
}
```

Concrete Component

```kotlin
class SimpleCoffee : Coffee {
    override fun cost() = 5
    override fun description() = "Simple coffee"
}
```

Abstract Decorator

```kotlin
abstract class CoffeeDecorator(private val coffee: Coffee) : Coffee {
    override fun cost() = coffee.cost()
    override fun description() = coffee.description()
}
```

Concrete Decorators

```kotlin
class Milk(coffee: Coffee) : CoffeeDecorator(coffee) {
    override fun cost() = super.cost() + 2
    override fun description() = super.description() + ", milk"
}

class Sugar(coffee: Coffee) : CoffeeDecorator(coffee) {
    override fun cost() = super.cost() + 1
    override fun description() = super.description() + ", sugar"
}

class WhippedCream(coffee: Coffee) : CoffeeDecorator(coffee) {
    override fun cost() = super.cost() + 3
    override fun description() = super.description() + ", whipped cream"
}
```

Client code

```kotlin
fun main() {
    var coffee: Coffee = SimpleCoffee()
    coffee = Milk(coffee)
    coffee = Sugar(coffee)
    coffee = WhippedCream(coffee)

    println(coffee.description())
    println("Total cost: ${coffee.cost()}")
}
```

**Output:**
```
Simple coffee, milk, sugar, whipped cream
Total cost: 11
```

---

### Android Use Case
- **ContextWrapper**: `ContextWrapper` wraps a `Context` to delegate calls.
- **ViewGroup**: `FrameLayout` wraps child views to add layout behavior.
- **RecyclerView**: ItemDecoration adds visual decoration without changing the RecyclerView itself.