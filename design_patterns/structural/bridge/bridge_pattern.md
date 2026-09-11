# Bridge Pattern

- **Bridge is a Structural design pattern that separates an abstraction from its implementation so that the two can vary independently.**
- It uses composition instead of inheritance.

### Use Case
- UI toolkit (Window/Button with different OS implementations)
- Shape with different rendering engines
- Android: View with different implementations across API levels

### Steps to create Bridge

1. **Abstraction** : Declares the interface for the abstraction.
2. **Refined Abstraction** : Extends the Abstraction.
3. **Implementor** : Declares the interface for implementation.
4. **Concrete Implementor** : Implements the Implementor interface.

### Pros
1. Abstraction and implementation can be extended independently.
2. Reduces subclass explosion.
3. Follows Open/Closed Principle.

### Cons
1. Adds indirection.
2. More complex to understand.

---

### Kotlin code

Implementor interface

```kotlin
interface Renderer {
    fun renderCircle(radius: Int)
    fun renderRectangle(width: Int, height: Int)
}
```

Concrete Implementors

```kotlin
class VectorRenderer : Renderer {
    override fun renderCircle(radius: Int) {
        println("Vector: Drawing circle with radius $radius")
    }

    override fun renderRectangle(width: Int, height: Int) {
        println("Vector: Drawing rectangle $width x $height")
    }
}

class RasterRenderer : Renderer {
    override fun renderCircle(radius: Int) {
        println("Raster: Drawing circle with radius $radius")
    }

    override fun renderRectangle(width: Int, height: Int) {
        println("Raster: Drawing rectangle $width x $height")
    }
}
```

Abstraction

```kotlin
abstract class Shape(protected val renderer: Renderer) {
    abstract fun draw()
    abstract fun resize(factor: Float)
}
```

Refined Abstractions

```kotlin
class Circle(renderer: Renderer, private var radius: Int) : Shape(renderer) {
    override fun draw() {
        renderer.renderCircle(radius)
    }

    override fun resize(factor: Float) {
        radius = (radius * factor).toInt()
    }
}

class Rectangle(renderer: Renderer, private var width: Int, private var height: Int) : Shape(renderer) {
    override fun draw() {
        renderer.renderRectangle(width, height)
    }

    override fun resize(factor: Float) {
        width = (width * factor).toInt()
        height = (height * factor).toInt()
    }
}
```

Client code

```kotlin
fun main() {
    val vector = VectorRenderer()
    val raster = RasterRenderer()

    val circle = Circle(vector, 5)
    circle.draw()

    val rect = Rectangle(raster, 10, 20)
    rect.draw()

    rect.resize(2.0f)
    rect.draw()
}
```

---

### Android Use Case
- **View system**: Android's `View` class uses bridge-like patterns to separate view logic from rendering.
- **WindowManager**: Different window implementations across API levels.