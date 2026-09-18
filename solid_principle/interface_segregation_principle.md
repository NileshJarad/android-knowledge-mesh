# Interface Segregation Principle (ISP)

- **Definition**: Clients should not be forced to depend on interfaces they do not use. This means that you should break up interfaces into smaller, more focused interfaces so that clients only need to implement the methods they care about.
- **Author**: Robert C. Martin (Uncle Bob)
- **Category**: SOLID Principles

### 🧠 Key Concept

Instead of having one large "fat" interface that forces implementers to provide implementations for methods they don't need, we should split interfaces into smaller, specific ones. This leads to more decoupled and maintainable code.

---

### ❌ Bad Code Example

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
        // ❌ Robot doesn't eat — forced to implement irrelevant method
        throw UnsupportedOperationException("Robot doesn't eat")
    }
}
```

**Problems:**
- `RobotWorker` is forced to implement `eat()` even though robots don't eat
- Violates ISP — clients are forced to depend on unused methods

---

### ✅ Good Code Example

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

**Improvements:**
- `RobotWorker` only implements what it needs
- Code is clean, extendable, and respects Interface Segregation
- No more throwing exceptions for unused methods

---

### UML Diagram

![UML](interface_segregation_principle.puml)

---

### Java Code

```java
// Fat interface (violates ISP)
interface Worker {
    void work();
    void eat();
}

// Properly segregated interfaces
interface Workable {
    void work();
}

interface Eatable {
    void eat();
}

// Human implements both
class HumanWorker implements Workable, Eatable {
    @Override
    public void work() { System.out.println("Human working"); }
    @Override
    public void eat() { System.out.println("Human eating"); }
}

// Robot only needs Workable
class RobotWorker implements Workable {
    @Override
    public void work() { System.out.println("Robot working"); }
}
```

---

### 📱 Android Use Case

```kotlin
// In Android, ISP prevents bloated interfaces in:
// 1. RecyclerView adapters - separate ViewHolder interfaces
// 2. Lifecycle events - separate LifecycleObserver methods
// 3. Click listeners - specific vs general

// Violates ISP (too broad):
interface ClickListener {
    fun onClick()
    fun onLongClick()
    fun onDoubleClick()
    fun onSwipeLeft()
    fun onSwipeRight()
}

// Follows ISP (specific interfaces):
interface OnClickListener { fun onClick() }
interface OnLongClickListener { fun onLongClick() }
interface SwipeListener { fun onSwipe(direction: SwipeDirection) }

// Components only implement what they need
class SubmitButton : View(), OnClickListener { /* ... */ }
class SwipeToDelete : View(), SwipeListener { /* ... */ }
```

### 📱 Improved Android Use Case — Click Listeners

```kotlin
// Android's View.OnClickListener is a great example of ISP:
// It has only ONE method: `onClick(View?)`

// ✅ Good - single responsibility
button.setOnClickListener { /* simple click */ }

// ❌ What if you need multiple behaviors?
// Don't create one giant interface — combine small ones
interface OnItemClick {
    fun onItemClick(position: Int)
}

interface OnItemLongClick {
    fun onItemLongClick(position: Int): Boolean
}

// Adapter implements BOTH interfaces (it needs both)
class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>(), OnItemClick, OnItemLongClick {
    override fun onItemClick(position: Int) {
        // handle click
    }

    override fun onItemLongClick(position: Int): Boolean {
        // handle long click
        return true
    }
}
```

**Key takeaways for Android:**
- Prefer small, single-method functional interfaces (click listeners)
- Combine multiple small interfaces instead of one fat one
- Use `var clickListener: ((Int) -> Unit)?` for simple callbacks

---

### ✅ Best Practices

1. **Keep interfaces small and focused** — one responsibility per interface
2. **Use multiple interfaces** — a class can implement several specific interfaces
3. **Avoid fat interfaces** — if an interface has >3-4 methods, consider splitting
4. **Name interfaces by capability** — `Clickable`, `Scrollable`, `Editable`, etc.