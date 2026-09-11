# Composite Pattern

- **Composite is a Structural design pattern that lets you compose objects into tree structures and then work with them as if they were individual objects.**
- It treats individual objects and compositions uniformly.

### Use Case
- File system (files are leaves, folders are composites)
- UI components (buttons are leaves, panels are composites)
- Organization hierarchies

### Steps to create Composite

1. **Component** : Declare common interface for leaf and composite objects.
2. **Leaf** : Represents end objects with no children. Implements Component.
3. **Composite** : Stores child components and implements Component operations.
4. **Client** : Works with all components through the Component interface.

### Pros
1. Makes it easy to add new types of components.
2. Simplifies client code — it can treat individual objects and groups uniformly.
3. Supports recursive operations across the tree.

### Cons
1. Can be hard to limit components that a composite can contain.
2. Overuse can lead to overly generic designs.

---

### UML

```
         +--------------------+
         |   <<Component>>    |
         |   Component        |
         |--------------------|
         | +operation(): void |
         +--------------------+
                ^          ^
                |          |
    +-----------+----+  +--+------------+
    |   <<Leaf>>     |  | <<Composite>> |
    |   Leaf         |  | Composite     |
    |----------------|  |----------------|
    | +operation()   |  | +add()         |
    +----------------+  | +remove()      |
                        | +getChild()    |
                        | +operation()   |
                        +----------------+
```

---

### Kotlin code

Component interface

```kotlin
interface FileSystemComponent {
    fun printStructure(indent: String = "")
}
```

Leaf — File

```kotlin
class File(private val name: String) : FileSystemComponent {
    override fun printStructure(indent: String) {
        println("$indent- $name (file)")
    }
}
```

Composite — Directory

```kotlin
class Directory(private val name: String) : FileSystemComponent {
    private val children = mutableListOf<FileSystemComponent>()

    fun add(component: FileSystemComponent) {
        children.add(component)
    }

    fun remove(component: FileSystemComponent) {
        children.remove(component)
    }

    override fun printStructure(indent: String) {
        println("$indent+ $name (directory)")
        children.forEach { it.printStructure("$indent  ") }
    }
}
```

Client code

```kotlin
fun main() {
    val root = Directory("Root")

    val etc = Directory("etc")
    val home = Directory("home")
    val user = Directory("user")

    val hosts = File("hosts")
    val profile = File("profile")
    val notes = File("notes.txt")

    etc.add(hosts)
    user.add(profile)
    user.add(notes)
    home.add(user)

    root.add(etc)
    root.add(home)

    root.printStructure()
}
```

**Output:**
```
+ Root (directory)
  + etc (directory)
    - hosts (file)
  + home (directory)
    + user (directory)
      - profile (file)
      - notes.txt (file)
```

---

### Android Use Case
- **ViewGroup/View**: Android's `ViewGroup` (composite) holds child `View` objects (leaves). Both extend `View`. Methods like `draw()`, `layout()` work recursively.
- **Menu**: `Menu` contains `MenuItem` and submenus — classic Composite.