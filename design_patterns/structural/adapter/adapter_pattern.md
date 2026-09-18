# Adapter Pattern

- **Adapter is a Structural design pattern that lets you attach new behaviors to objects by placing them inside wrapper objects that contain those behaviors.**
- It is a flexible alternative to subclassing for extending functionality.

### Use Case
- Connecting incompatible interfaces (e.g., US to UK plug)
- Legacy code integration
- Android: `ArrayAdapter`, `RecyclerView.Adapter`

### Steps to create Adapter

1. **Target** : The interface the client expects.
2. **Adaptee** : The existing interface that needs adapting.
3. **Adapter** : Implements Target and wraps Adaptee, delegating calls.

### Pros
1. Decouples client from the concrete Adaptee.
2. Enables reusable classes that work with unrelated APIs.
3. Follows Single Responsibility Principle — adapters focus solely on interface conversion.

### Cons
1. Can add unnecessary complexity for simple cases.
2. Requires many small classes.

---

### UML

```
         +----------------+
         |   <<Target>>   |
         |   Target       |
         |----------------|
         | +request():    |
         +----------------+
                ^
                |
      +---------+---+  +----------------+
      | <<Adaptee>>  |  | <<Adapter>>    |
      | Adaptee      |  | Adapter        |
      |----------------|  |----------------|
      | +specificRequest(): void | +request(): void |
      +----------------+  +----------------+
                              |
                   +---------+-----------+
                   | <<ConcreteAdapter>> |
                   | ConcreteAdapter    |
                   +----------------------+
```

---

### Kotlin code

Target interface

```kotlin
interface MediaPlayer {
    fun play(audioType: String, fileName: String)
}
```

Adaptee — Media player with different API

```kotlin
class AdvancedMediaPlayer {
    fun playMp3(fileName: String) {
        println("Playing MP3: $fileName")
    }

    fun playVlc(fileName: String) {
        println("Playing VLC: $fileName")
    }
}
```

Adapter — implements Target, wraps Adaptee

```kotlin
class MediaAdapter(private val advancedPlayer: AdvancedMediaPlayer) : MediaPlayer {
    override fun play(audioType: String, fileName: String) {
        when (audioType.lowercase()) {
            "mp3" -> advancedPlayer.playMp3(fileName)
            "vlc" -> advancedPlayer.playVlc(fileName)
            else -> println("Unsupported audio format: $audioType")
        }
    }
}
```

Client code

```kotlin
fun main() {
    val adapter = MediaAdapter(AdvancedMediaPlayer())

    adapter.play("mp3", "song.mp3")
    adapter.play("vlc", "movie.vlc")
    adapter.play("invalid", "file.xyz")
}
```

---

### Android Use Case
- **ArrayAdapter**: Converts data objects to View rows.
- **RecyclerView.Adapter**: Binds data to RecyclerView items.
- **CursorAdapter**: Maps database cursor results to Views.
---

### Java code

Target interface

```java
public interface MediaPlayer {
    void play(String audioType, String fileName);
}
```

Adaptee

```java
public class AdvancedMediaPlayer {
    public void playMp3(String fileName) {
        System.out.println("Playing MP3: " + fileName);
    }

    public void playVlc(String fileName) {
        System.out.println("Playing VLC: " + fileName);
    }
}
```

Adapter

```java
public class MediaAdapter implements MediaPlayer {
    private final AdvancedMediaPlayer advancedPlayer;

    public MediaAdapter(AdvancedMediaPlayer advancedPlayer) {
        this.advancedPlayer = advancedPlayer;
    }

    @Override
    public void play(String audioType, String fileName) {
        if (audioType.equalsIgnoreCase("mp3")) {
            advancedPlayer.playMp3(fileName);
        } else if (audioType.equalsIgnoreCase("vlc")) {
            advancedPlayer.playVlc(fileName);
        } else {
            System.out.println("Unsupported audio format: " + audioType);
        }
    }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        MediaAdapter adapter = new MediaAdapter(new AdvancedMediaPlayer());

        adapter.play("mp3", "song.mp3");
        adapter.play("vlc", "movie.vlc");
        adapter.play("invalid", "file.xyz");
    }
}
```
