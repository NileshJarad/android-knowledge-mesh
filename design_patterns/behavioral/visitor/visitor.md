# Visitor Pattern

- **Visitor is a Behavioral design pattern that lets you add new operations to existing classes without modifying them.**
- It separates algorithms from the objects on which they operate.

### Use Case
- Rendering engines (HTML, JSON, XML export from same data model)
- Compiler design (AST node visitors)
- Android: `View` traversal with `ViewVisitor`

### Steps to create Visitor

1. **Visitor** : Declares visit methods for each element type.
2. **Concrete Visitor** : Implements the operations.
3. **Element** : Declares an accept method that takes a visitor.
4. **Concrete Element** : Implements accept by calling visitor.visit(this).

### Pros
1. Adds new operations without changing classes.
2. Keeps related operations together.
3. Easy to add new visitors.

### Cons
1. Hard to add new element types — all visitors must be updated.
2. Breaks encapsulation — visitor needs internal details.

---

### Kotlin code

Element interface

```kotlin
interface Animal {
    fun accept(visitor: Visitor)
}
```

Concrete Elements

```kotlin
class Dog(val name: String) : Animal {
    override fun accept(visitor: Visitor) {
        visitor.visitDog(this)
    }
}

class Cat(val name: String) : Animal {
    override fun accept(visitor: Visitor) {
        visitor.visitCat(this)
    }
}
```

Visitor interface

```kotlin
interface Visitor {
    fun visitDog(dog: Dog)
    fun visitCat(cat: Cat)
}
```

Concrete Visitor — Sound

```kotlin
class SoundVisitor : Visitor {
    override fun visitDog(dog: Dog) {
        println("${dog.name} says Woof!")
    }

    override fun visitCat(cat: Cat) {
        println("${cat.name} says Meow!")
    }
}
```

Concrete Visitor — Action

```kotlin
class ActionVisitor : Visitor {
    override fun visitDog(dog: Dog) {
        println("${dog.name} is fetching the ball")
    }

    override fun visitCat(cat: Cat) {
        println("${cat.name} is catching a mouse")
    }
}
```

Client code

```kotlin
fun main() {
    val animals: List<Animal> = listOf(Dog("Buddy"), Cat("Whiskers"), Dog("Max"))

    val soundVisitor = SoundVisitor()
    val actionVisitor = ActionVisitor()

    animals.forEach { it.accept(soundVisitor) }
    println("---")
    animals.forEach { it.accept(actionVisitor) }
}
```

---

### Android Use Case
- **ViewGroup traversal**: `ViewGroup` visits each child.
- **AccessibilityNodeProvider**: Visits UI nodes to extract text/content descriptions.
- **Data binding**: Binding expressions visit model properties.