# Memento Pattern

- **Memento is a Behavioral design pattern that captures and externalizes an object's internal state so that it can be restored later without violating encapsulation.**
- It is commonly used for undo/redo functionality, snapshots, and state restoration.

### Use Case
- Text editors (undo/redo)
- Game state snapshots
- Database transactions
- Android: `Bundle` for Activity state restoration

### Steps to create Memento

1. **Memento** : Stores the state of the originator.
2. **Originator** : Creates and restores mementos.
3. **Caretaker** : Holds mementos for later use (does not access state directly).

### Pros
1. Preserves encapsulation — state is hidden in the memento.
2. Enables undo/redo and state restoration.
3. Simple to understand and implement.

### Cons
1. Memory overhead — snapshots can be large.
2. Caretaker must manage memento lifecycle.

---

### Kotlin code

Memento — stores state

```kotlin
class PlayerState(
    val health: Int,
    val position: Int,
    val score: Int
)
```

Originator — the game

```kotlin
class Player {
    private var health: Int = 100
    private var position: Int = 0
    private var score: Int = 0

    fun takeDamage(damage: Int) {
        health -= damage
        println("Health reduced to $health")
    }

    fun move(steps: Int) {
        position += steps
        println("Moved to position $position")
    }

    fun addScore(points: Int) {
        score += points
        println("Score increased to $score")
    }

    fun createMemento(): PlayerState {
        return PlayerState(health, position, score)
    }

    fun restoreMemento(memento: PlayerState) {
        health = memento.health
        position = memento.position
        score = memento.score
        println("State restored to health=$health, position=$position, score=$score")
    }

    fun getCurrentState() = "health=$health, position=$position, score=$score"
}
```

Caretaker — stores mementos

```kotlin
class GameHistory {
    private val history = mutableListOf<PlayerState>()

    fun save(state: PlayerState) {
        history.add(state)
    }

    fun undo(): PlayerState {
        if (history.isEmpty()) {
            throw IllegalStateException("No state to undo")
        }
        return history.removeAt(history.size - 1)
    }
}
```

Client code

```kotlin
fun main() {
    val player = Player()
    val history = GameHistory()

    history.save(player.createMemento())

    player.move(10)
    player.takeDamage(30)
    player.addScore(50)
    println("Current state: ${player.getCurrentState()}")

    val previousState = history.undo()
    player.restoreMemento(previousState)
    println("Restored state: ${player.getCurrentState()}")
}
```

---

### Android Use Case
- **Activity onSaveInstanceState()**: Restores state after configuration changes.
- **ViewModel**: Restores state from `SavedStateHandle`.
- **Game state**: Snapshots before and after actions.