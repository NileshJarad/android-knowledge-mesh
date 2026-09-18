# Proxy Pattern

- **Proxy is a Structural design pattern that provides a substitute object that acts as a placeholder for another object to control access to it.**
- It can add behaviors like lazy loading, access control, logging, or caching.

### Types of Proxy
1. **Virtual Proxy** — Creates objects on demand (lazy loading).
2. **Remote Proxy** — Acts as a local representative for an object in a different address space.
3. **Protection Proxy** — Controls access based on permissions.
4. **Caching Proxy** — Caches results of expensive operations.

### Steps to create Proxy

1. **Subject** : Declares the interface for the real subject.
2. **RealSubject** : The actual object that does the work.
3. **Proxy** : Holds a reference to RealSubject and implements the same interface.
4. **Client** : Uses the proxy instead of the real subject.

### Pros
1. Adds behaviors without modifying the real subject.
2. Lazy loading for expensive objects.
3. Access control and security.

### Cons
1. Adds an extra layer of indirection.
2. More classes to maintain.

---

### Kotlin code

Subject interface

```kotlin
interface Image {
    fun display()
}
```

Real Subject (expensive to load)

```kotlin
class RealImage(private val filename: String) : Image {
    init {
        println("Loading image from disk: $filename")
    }

    override fun display() {
        println("Displaying image: $filename")
    }
}
```

Proxy (lazy loading)

```kotlin
class ProxyImage(private val filename: String) : Image {
    private var realImage: RealImage? = null

    override fun display() {
        if (realImage == null) {
            realImage = RealImage(filename)
        }
        realImage!!.display()
    }
}
```

Protection Proxy

```kotlin
class PermissionProxyImage(
    private val filename: String,
    private val userRole: String
) : Image {
    private var realImage: RealImage? = null

    override fun display() {
        if (userRole != "admin") {
            println("Access denied: $userRole cannot view $filename")
            return
        }
        if (realImage == null) {
            realImage = RealImage(filename)
        }
        realImage!!.display()
    }
}
```

Client code

```kotlin
fun main() {
    val image = ProxyImage("photo.jpg")

    println("Image not yet loaded")
    image.display()  // Loads and displays
    image.display()  // Already loaded, no disk access
}
```

---

### Android Use Case
- **ContentResolver**: Acts as a proxy for accessing content providers.
- **Room DAO**: DAO methods act as proxies that delegate to SQLite.
- **Glide**: Loads images lazily with caching proxy.
- **Binder**: Android's IPC mechanism uses a proxy on the client side.
---

### Java code

Subject interface

```java
public interface Image {
    void display();
}
```

Real Subject

```java
public class RealImage implements Image {
    private final String filename;

    public RealImage(String filename) {
        this.filename = filename;
        System.out.println("Loading image from disk: " + filename);
    }

    @Override
    public void display() {
        System.out.println("Displaying image: " + filename);
    }
}
```

Proxy (Lazy Loading)

```java
public class ProxyImage implements Image {
    private final String filename;
    private RealImage realImage;

    public ProxyImage(String filename) {
        this.filename = filename;
    }

    @Override
    public void display() {
        if (realImage == null) {
            realImage = new RealImage(filename);
        }
        realImage.display();
    }
}
```

Protection Proxy

```java
public class PermissionProxyImage implements Image {
    private final String filename;
    private final String userRole;
    private RealImage realImage;

    public PermissionProxyImage(String filename, String userRole) {
        this.filename = filename;
        this.userRole = userRole;
    }

    @Override
    public void display() {
        if (!userRole.equals("admin")) {
            System.out.println("Access denied: " + userRole + " cannot view " + filename);
            return;
        }
        if (realImage == null) {
            realImage = new RealImage(filename);
        }
        realImage.display();
    }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        Image image = new ProxyImage("photo.jpg");

        System.out.println("Image not yet loaded");
        image.display();  // Loads and displays
        image.display();  // Already loaded
    }
}
```
