# Command Pattern

- **Command is a Behavioral design pattern that turns a request into an independent object containing all the information about the request.**
- It allows you to queue, log, or undo commands.

### Use Case
- Undo/redo functionality
- Macro commands (sequence of commands)
- Job queues
- Android: `Runnable` in thread pools

### Steps to create Command

1. **Command** : Declares the interface for executing a command.
2. **Concrete Commands** : Implement Command and define the actual operation.
3. **Receiver** : The object that performs the actual work.
4. **Invoker** : Holds a command and triggers its execution.
5. **Client** : Creates concrete commands and wires them to receivers and invokers.

### Pros
1. Decouples the invoker from the receiver.
2. Enables undo/redo, command queues, and logging.
3. Follows Open/Closed Principle — new commands can be added without modifying existing code.

### Cons
1. Many small classes can make the design verbose.

---

### Kotlin code

Command interface

```kotlin
interface Command {
    fun execute()
    fun undo()
}
```

Receiver — Text Editor

```kotlin
class TextEditor {
    private var text = ""

    fun insert(word: String) {
        text += word
        println("Inserted: $word")
    }

    fun delete(word: String) {
        text = text.removeSuffix(word)
        println("Deleted: $word")
    }

    fun getText() = text
}
```

Concrete Commands

```k
class InsertCommand(
    private val editor: TextEditor,
    private val word: String
) : Command {
    override fun execute() = editor.insert(word)
    override fun undo() = editor.delete(word)
}

class DeleteCommand(
    private val editor: TextEditor,
    private val word: String
) : Command {
    override fun execute() = editor.delete(word)
    override fun undo() = editor.insert(word)
}
```

Invoker — Command History

```kotlin
class CommandHistory {
    private val history = mutableListOf<Command>()

    fun push(command: Command) {
        history.add(command)
    }

    fun pop(): Command {
        val last = history.removeAt(history.size - 1)
        return last
    }

    fun undo() {
        if (history.isNotEmpty()) {
            pop().undo()
        }
    }
}
```

Client code

```kotlin
fun main() {
    val editor = TextEditor()
    val history = CommandHistory()

    val insert = InsertCommand(editor, "Hello")
    insert.execute()
    history.push(insert)

    val insert2 = InsertCommand(editor, " World")
    insert2.execute()
    history.push(insert2)

    println("Current text: ${editor.getText()}")

    history.undo()  // Undoes " World"
    history.undo()  // Undoes "Hello"
    println("After undo: ${editor.getText()}")
}
```

---

### Android Use Case
- **AsyncTask / Coroutine**: The task is a command object that gets queued and executed.
- **Handler/Looper**: Messages are commands placed in a queue and processed later.
- **Firebase Analytics**: Analytics events are queued as commands and sent in batches.