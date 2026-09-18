# Liskov Substitution Principle (LSP)

- **Definition**: Subtypes must be substitutable for their base types. In simpler terms, any instance of a parent class should be able to be replaced by an instance of one of its child classes without affecting the correctness of the program.
- **Author**: Barbara Liskov
- **Category**: SOLID Principles

### 🧠 Key Concept

If `S` is a subtype of `T`, then objects of type `T` may be replaced with objects of type `S` without altering any of the desirable properties of the program. Violations typically happen when a subclass weakens preconditions or strengthens postconditions.

---

### ❌ Bad Code Example

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

**Problems:**
- `Ostrich` is a `Bird`, but it can't fly
- Calling `makeBirdFly(Ostrich())` will crash
- Violates LSP — subtypes break expected behavior

---

### ✅ Good Code Example

```kotlin
interface Bird {
    fun eat()
}

interface FlyingBird : Bird {
    fun fly()
}

class Sparrow : FlyingBird {
    override fun eat() { println("Sparrow eating") }
    override fun fly() { println("Sparrow flying") }
}

class Ostrich : Bird {
    override fun eat() { println("Ostrich eating") }
}

fun makeBirdFly(bird: FlyingBird) {
    bird.fly()
}
```

**Improvements:**
- `FlyingBird` interface separates flying capability
- `Ostrich` doesn't implement `fly()` — so it can't be passed to `makeBirdFly()`
- LSP is respected — substitution works correctly

---

### UML Diagram

![UML](liskov_substitution_principle.puml)

---

### Java Code

```java
// Base interface - common behavior
public interface Bird {
    void eat();
}

// Specialized interface for flying birds
public interface FlyingBird extends Bird {
    void fly();
}

// Can fly - implements both
public class Sparrow implements FlyingBird {
    @Override
    public void eat() { System.out.println("Sparrow eating"); }
    @Override
    public void fly() { System.out.println("Sparrow flying"); }
}

// Cannot fly - only implements base interface
public class Ostrich implements Bird {
    @Override
    public void eat() { System.out.println("Ostrich eating"); }
}

// Client code
public class Main {
    public static void makeBirdFly(FlyingBird bird) {
        bird.fly();
    }

    public static void main(String[] args) {
        makeBirdFly(new Sparrow());  // Works
        // makeBirdFly(new Ostrich()); // Compile error - Ostrich doesn't implement FlyingBird
    }
}
```

---

### 📱 Android Use Case

```kotlin
// In Android, LSP is crucial for:
// 1. View binding - all Views behave as View
// 2. Parcelable - subtypes can replace base types
// 3. FragmentManager - fragments are interchangeable

// LSP violation example:
abstract class BaseAdapter {
    abstract fun onBind(item: Any)
}

// If a subclass throws UnsupportedOperationException for certain types, it violates LSP

// Correct approach - sealed classes or specific interfaces
sealed class ItemType {
    data class Header(val title: String) : ItemType()
    data class Content(val text: String) : ItemType()
}

// Each type handled appropriately without LSP violations
```

---

### ✅ Best Practices

1. **Don't override with stricter behavior** — subclass shouldn't throw exceptions parent doesn't
2. **Use interfaces to separate capabilities** — flying, swimming, walking are separate
3. **Prefer composition over inheritance** when behavior differs significantly
4. **Test substitutability** — can you replace parent with child in all contexts?