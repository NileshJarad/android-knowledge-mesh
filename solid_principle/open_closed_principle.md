# Open Closed Principle (OCP)

- **Definition**: Software entities (classes, modules, functions, etc.) should be open for extension but closed for modification. This means you should be able to add new functionality without changing existing code.
- **Author**: Robert C. Martin (Uncle Bob)
- **Category**: SOLID Principles

### 🧠 Key Concept

The Open Closed Principle encourages designing software so that new functionality can be added by extending existing code rather than modifying it. This makes the system more maintainable and less error-prone.

---

### ❌ Bad Code Example

```kotlin
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

**Problems:**
- Adding a new shape requires modifying the `Shape` class
- Violates OCP — the class is open for modification
- Growing if/else chain makes the code harder to maintain

---

### ✅ Good Code Example

```kotlin
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

class Triangle(private val base: Double, private val height: Double) : Shape() {
    override fun area(): Double {
        return (base * height) / 2
    }
}
```

**Improvements:**
- New shapes can be added without modifying existing code
- Each shape class handles its own area calculation
- Follows Open Closed Principle

---

### UML Diagram

![UML](open_closed_principle.puml)

---

### Java Code

```java
// Abstract base class
public abstract class Shape {
    public abstract double area();
}

// Concrete implementations
public class Rectangle extends Shape {
    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double area() {
        return width * height;
    }
}

public class Circle extends Shape {
    private final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }
}

public class Triangle extends Shape {
    private final double base;
    private final double height;

    public Triangle(double base, double height) {
        this.base = base;
        this.height = height;
    }

    @Override
    public double area() {
        return (base * height) / 2;
    }
}

// Client code
public class Main {
    public static void main(String[] args) {
        List<Shape> shapes = Arrays.asList(
            new Rectangle(10, 20),
            new Circle(5),
            new Triangle(10, 15)
        );

        for (Shape shape : shapes) {
            System.out.println("Area: " + shape.area());
        }
    }
}
```

---

### 📱 Android Use Case

```kotlin
// RecyclerView.Adapter with different ViewHolder types
abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: T)
}

class TextViewHolder(itemView: View) : BaseViewHolder<String>(itemView) {
    override fun bind(item: String) { /* render text */ }
}

class ImageViewHolder(itemView: View) : BaseViewHolder<Image>(itemView) {
    override fun bind(item: Image) { /* render image */ }
}

// New ViewHolder types can be added without modifying BaseViewHolder
```

---

### ✅ Best Practices

1. **Use abstraction** — depend on interfaces or abstract classes
2. **Prefer composition over inheritance** when appropriate
3. **Use polymorphism** — let subclasses handle their own behavior
4. **Avoid type checking** — replace if/else chains with polymorphic behavior