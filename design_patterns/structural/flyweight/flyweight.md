# Flyweight Pattern

- **Flyweight is a Structural design pattern that minimizes memory usage by sharing as much data as possible with similar objects.**
- It separates intrinsic (shared) state from extrinsic (unique) state.

### Use Case
- Text rendering (shared character metrics, unique position/color)
- Game entities (shared sprite data, unique position)
- Database connection pools

### Steps to create Flyweight

1. **Flyweight** : Declares the interface for sharing state (intrinsic).
2. **ConcreteFlyweight** : Implements Flyweight, stores intrinsic state.
3. **FlyweightFactory** : Manages flyweight objects, returns shared instances.
4. **Client** : Stores extrinsic state and passes it when calling flyweight methods.

### Pros
1. Reduces memory usage significantly when many similar objects exist.
2. Shares immutable state across objects.

### Cons
1. Increases complexity — state must be split into intrinsic/extrinsic.
2. Thread safety issues if flyweights are mutable.

---

### Kotlin code

Flyweight interface

```kotlin
interface CharacterStyle {
    fun render(text: String, x: Int, y: Int, color: String)
}
```

Concrete Flyweight (intrinsic: font family, size — shared)

```kotlin
class TextStyle(private val fontFamily: String, private val fontSize: Int) : CharacterStyle {
    override fun render(text: String, x: Int, y: Int, color: String) {
        println("Rendering '$text' at ($x,$y) with $fontFamily $fontSize in $color")
    }
}
```

Factory

```kotlin
object StyleFactory {
    private val styles = mutableMapOf<String, CharacterStyle>()

    fun getStyle(fontFamily: String, fontSize: Int): CharacterStyle {
        val key = "$fontFamily-$fontSize"
        return styles.getOrPut(key) {
            TextStyle(fontFamily, fontSize)
        }
    }
}
```

Client code

```kotlin
fun main() {
    // Reuse same style objects for different positions/colors
    val style1 = StyleFactory.getStyle("Arial", 12)
    val style2 = StyleFactory.getStyle("Arial", 12)  // Same instance reused
    val style3 = StyleFactory.getStyle("Courier", 14)

    style1.render("H", x = 10, y = 20, color = "black")
    style1.render("e", x = 20, y = 20, color = "black")
    style2.render("i", x = 30, y = 20, color = "red")  // Reused Arial 12
    style3.render("W", x = 40, y = 30, color = "blue")  // Different style
}
```

---

### Android Use Case
- **TextView**: `Typeface` objects are shared across multiple TextViews — Android caches them via `Typeface.create()`.
- **RecyclerView.Adapter**: Item views are recycled — the view holder is a flyweight that gets rebound with extrinsic data.
---

### Java code

Flyweight interface

```java
public interface CharacterStyle {
    void render(String text, int x, int y, String color);
}
```

Concrete Flyweight

```java
public class TextStyle implements CharacterStyle {
    private final String fontFamily;
    private final int fontSize;

    public TextStyle(String fontFamily, int fontSize) {
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
    }

    @Override
    public void render(String text, int x, int y, String color) {
        System.out.println("Rendering '" + text + "' at (" + x + "," + y + ") with " + fontFamily + " " + fontSize + " in " + color);
    }

    public String getFontFamily() { return fontFamily; }
    public int getFontSize() { return fontSize; }
}
```

Factory

```java
import java.util.HashMap;
import java.util.Map;

public class StyleFactory {
    private final Map<String, CharacterStyle> styles = new HashMap<>();

    public CharacterStyle getStyle(String fontFamily, int fontSize) {
        String key = fontFamily + "-" + fontSize;
        return styles.computeIfAbsent(key, k -> new TextStyle(fontFamily, fontSize));
    }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        StyleFactory factory = new StyleFactory();

        CharacterStyle style1 = factory.getStyle("Arial", 12);
        CharacterStyle style2 = factory.getStyle("Arial", 12);
        CharacterStyle style3 = factory.getStyle("Courier", 14);

        style1.render("H", 10, 20, "black");
        style1.render("e", 20, 20, "black");
        style2.render("i", 30, 20, "red");
        style3.render("W", 40, 30, "blue");
    }
}
```
