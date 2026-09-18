# Facade Pattern

- **Facade is a Structural design pattern that provides a simplified interface to a complex subsystem.**
- It hides the complexity of a system and provides a higher-level interface.

### Use Case
- Library management systems
- Compilation process
- Android: `FragmentManager` facade

### Steps to create Facade

1. **Subsystems** : Complex classes that provide specific functionality.
2. **Facade** : A class that wraps the subsystems and provides a simple interface.
3. **Client** : Uses the Facade instead of the subsystems directly.

### Pros
1. Simplifies the interface to a complex subsystem.
2. Decouples client from subsystems.
3. Easy to add new subsystems without affecting clients.

### Cons
1. Can become a god object if not designed well.

---

### UML

```
     +----------------+
     |   <<Facade>>   |
     |   Library      |
     |----------------|
     | +borrow()      |
     | +return()      |
     +--------+-------+
              |
    +---------+---------+
    |                   |
+--------+        +--------+
| Catalog|        | Lending|
+--------+        +--------+
```

---

### Kotlin code

Subsystems

```kotlin
class Catalog {
    fun searchBook(title: String): Boolean {
        println("Searching catalog for: $title")
        return true
    }
}

class Lending {
    fun checkOut(bookId: String): Boolean {
        println("Checking out: $bookId")
        return true
    }

    fun checkIn(bookId: String) {
        println("Checking in: $bookId")
    }
}
```

Facade

```kotlin
class LibraryFacade(
    private val catalog: Catalog,
    private val lending: Lending
) {
    fun borrowBook(title: String) {
        if (catalog.searchBook(title)) {
            lending.checkOut(title)
        }
    }

    fun returnBook(bookId: String) {
        lending.checkIn(bookId)
    }
}
```

Client code

```kotlin
fun main() {
    val library = LibraryFacade(Catalog(), Lending())

    library.borrowBook("Android Design Patterns")
    library.returnBook("Android Design Patterns")
}
```

---

### Android Use Case
- **FragmentManager**: Simplifies fragment transactions.
- **Room**: Simplifies database operations with a single `Room.databaseBuilder()` call.
---

### Java code

Subsystems

```java
public class Catalog {
    public boolean searchBook(String title) {
        System.out.println("Searching catalog for: " + title);
        return true;
    }
}

public class Lending {
    public boolean checkOut(String bookId) {
        System.out.println("Checking out: " + bookId);
        return true;
    }

    public void checkIn(String bookId) {
        System.out.println("Checking in: " + bookId);
    }
}
```

Facade

```java
public class LibraryFacade {
    private final Catalog catalog;
    private final Lending lending;

    public LibraryFacade(Catalog catalog, Lending lending) {
        this.catalog = catalog;
        this.lending = lending;
    }

    public void borrowBook(String title) {
        if (catalog.searchBook(title)) {
            lending.checkOut(title);
        }
    }

    public void returnBook(String bookId) {
        lending.checkIn(bookId);
    }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        LibraryFacade library = new LibraryFacade(new Catalog(), new Lending());

        library.borrowBook("Android Design Patterns");
        library.returnBook("Android Design Patterns");
    }
}
```
