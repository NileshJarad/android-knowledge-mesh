# Simple Factory

- **Simple Factory (also called Static Factory) is a creational design pattern that provides an interface for creating objects without exposing the instantiation logic to the client.**
- It is not technically a GoF pattern but a common simplification that precedes Factory Method.

### Difference from Factory Method
- **Simple Factory**: One class (the factory) decides which concrete class to instantiate. No subclassing needed.
- **Factory Method**: Subclasses decide which class to instantiate via inheritance.

### Steps to create Simple Factory

1. **Product** : Define an interface or abstract class for objects the factory creates.
2. **Concrete Product** : Implement the Product interface.
3. **Factory** : A class with a static (or instance) method that takes a parameter and returns the appropriate Product.
4. **Client** : Calls the factory method instead of constructing objects directly.

### Pros
1. Centralizes object creation in one place.
2. Client code is decoupled from concrete classes.
3. Simpler than Factory Method — no inheritance hierarchy needed.

### Cons
1. The factory class can become bloated as new products are added (violates Open/Closed Principle).
2. Hard to extend without modifying the factory.

---

### UML

```
     +----------------+        +----------------+
     |   <<Factory>>  |        |   <<Product>>  |
     |  ProductFactory|        |   Product      |
     |----------------|        |----------------|
     | +create():     |<>------| +operation():   |
     |  Product       |        |  void          |
     +----------------+        +----------------+
                |
        +-------+--------+
        |                |
+---------------+  +---------------+
| ConcreteProd1 |  | ConcreteProd2 |
+---------------+  +---------------+
```

---

### Kotlin code

Product interface

```kotlin
interface Shape {
    fun draw()
}
```

Concrete Products

```kotlin
class Circle : Shape {
    override fun draw() = println("Drawing Circle")
}

class Rectangle : Shape {
    override fun draw() = println("Drawing Rectangle")
}

class Square : Shape {
    override fun draw() = println("Drawing Square")
}
```

Factory

```kotlin
object ShapeFactory {
    fun createShape(type: String): Shape {
        return when (type.lowercase()) {
            "circle" -> Circle()
            "rectangle" -> Rectangle()
            "square" -> Square()
            else -> throw IllegalArgumentException("Unknown shape: $type")
        }
    }
}
```

Client code

```kotlin
fun main() {
    val circle = ShapeFactory.createShape("circle")
    circle.draw()

    val rectangle = ShapeFactory.createShape("rectangle")
    rectangle.draw()

    val square = ShapeFactory.createShape("square")
    square.draw()
}
```

---

### Java code

Product

```java
public interface Shape {
    void draw();
}
```

Concrete Products

```java
public class Circle implements Shape {
    @Override
    public void draw() { System.out.println("Drawing Circle"); }
}

public class Rectangle implements Shape {
    @Override
    public void draw() { System.out.println("Drawing Rectangle"); }
}

public class Square implements Shape {
    @Override
    public void draw() { System.out.println("Drawing Square"); }
}
```

Factory

```java
public class ShapeFactory {
    public static Shape createShape(String type) {
        switch (type.toLowerCase()) {
            case "circle": return new Circle();
            case "rectangle": return new Rectangle();
            case "square": return new Square();
            default: throw new IllegalArgumentException("Unknown shape: " + type);
        }
    }
}
```

Client

```java
public class Main {
    public static void main(String[] args) {
        Shape circle = ShapeFactory.createShape("circle");
        circle.draw();

        Shape rectangle = ShapeFactory.createShape("rectangle");
        rectangle.draw();
    }
}
```

---

### Android Use Case

- **RecyclerView Adapter**: `ViewHolder` creation in `onCreateViewHolder` is essentially a simple factory — the adapter decides which ViewHolder to create based on view type.
- **Dependency Injection**: Providing different implementations based on configuration flags.