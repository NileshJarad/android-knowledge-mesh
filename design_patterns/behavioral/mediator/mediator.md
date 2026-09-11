# Mediator Pattern

- **Mediator is a Behavioral design pattern that lets you reduce chaotic dependencies between objects.**
- The pattern restricts direct communications between objects and forces them to collaborate only via a mediator object.

### Use Case
- Chat rooms (users communicate via the room, not directly)
- Flight control systems
- UI components (buttons, text fields, dropdowns coordinating)
- Android: `FragmentManager`

### Steps to create Mediator

1. **Mediator** : Declares the interface for communication.
2. **Concrete Mediator** : Implements the mediator interface and coordinates colleagues.
3. **Colleague** : Objects that communicate through the mediator.

### Pros
1. Reduces coupling between components.
2. Makes it easy to add new components without modifying existing ones.
3. Centralizes control logic.

### Cons
1. The mediator can become a "god object" with too much responsibility.

---

### Kotlin code

Mediator interface

```kotlin
interface ChatMediator {
    fun sendMessage(message: String, colleague: Colleague)
    fun register(colleague: Colleague)
}
```

Concrete Mediator — Chat Room

```kotlin
class ChatRoom : ChatMediator {
    private val colleagues = mutableListOf<Colleague>()

    override fun register(colleague: Colleague) {
        colleagues.add(colleague)
    }

    override fun sendMessage(message: String, colleague: Colleague) {
        colleagues
            .filter { it != colleague }
            .forEach { it.receive(message) }
    }
}
```

Colleague — User

```kotlin
class User(
    private val name: String,
    private val mediator: ChatMediator
) : Colleague {
    init {
        mediator.register(this)
    }

    fun send(message: String) {
        println("$name sends: $message")
        mediator.sendMessage(message, this)
    }

    override fun receive(message: String) {
        println("$name receives: $message")
    }
}
```

Client code

```kotlin
fun main() {
    val chatRoom = ChatRoom()

    val alice = User("Alice", chatRoom)
    val bob = User("Bob", chatRoom)
    val charlie = User("Charlie", chatRoom)

    alice.send("Hello everyone!")
    bob.send("Hi Alice!")
}
```

---

### Android Use Case
- **FragmentManager**: Fragments communicate through the fragment manager, not directly.
- **ViewModel**: Shared ViewModel mediates between fragments in a navigation graph.
- **RecyclerView.Adapter**: Coordinates between ViewHolder and data source.