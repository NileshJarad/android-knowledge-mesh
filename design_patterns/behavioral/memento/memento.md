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
---

### Java code

Memento class

```java
public class PlayerState {
    private final int health;
    private final int position;
    private final int score;

    public PlayerState(int health, int position, int score) {
        this.health = health;
        this.position = position;
        this.score = score;
    }

    public int getHealth() { return health; }
    public int getPosition() { return position; }
    public int getScore() { return score; }
}
```

Originator

```java
public class Player {
    private int health = 100;
    private int position = 0;
    private int score = 0;

    public void takeDamage(int damage) {
        health -= damage;
        System.out.println("Health reduced to " + health);
    }

    public void move(int steps) {
        position += steps;
        System.out.println("Moved to position " + position);
    }

    public void addScore(int points) {
        score += points;
        System.out.println("Score increased to " + score);
    }

    public PlayerState createMemento() {
        return new PlayerState(health, position, score);
    }

    public void restoreMemento(PlayerState memento) {
        this.health = memento.getHealth();
        this.position = memento.getPosition();
        this.score = memento.getScore();
        System.out.println("State restored to health=" + health + ", position=" + position + ", score=" + score);
    }

    public String getCurrentState() {
        return "health=" + health + ", position=" + position + ", score=" + score;
    }
}
```

Caretaker

```java
import java.util.ArrayList;
import java.util.List;

public class GameHistory {
    private final List<PlayerState> history = new ArrayList<>();

    public void save(PlayerState state) {
        history.add(state);
    }

    public PlayerState undo() {
        if (history.isEmpty()) {
            throw new IllegalStateException("No state to undo");
        }
        return history.remove(history.size() - 1);
    }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        Player player = new Player();
        GameHistory history = new GameHistory();

        history.save(player.createMemento());

        player.move(10);
        player.takeDamage(30);
        player.addScore(50);
        System.out.println("Current state: " + player.getCurrentState());

        PlayerState previousState = history.undo();
        player.restoreMemento(previousState);
        System.out.println("Restored state: " + player.getCurrentState());
    }
}
```
