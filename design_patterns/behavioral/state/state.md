# State Pattern

- **State is a Behavioral design pattern that allows an object to alter its behavior when its internal state changes.**
- The object appears to change its class.

### Use Case
- Finite state machines
- Workflow engines
- Network connection states (idle, connecting, connected, disconnected)
- Android: lifecycle states

### Steps to create State

1. **Context** : Holds a reference to the current state.
2. **State** : Declares the interface for state-specific behavior.
3. **Concrete States** : Implement state-specific behavior.

### Pros
1. Removes large conditional statements.
2. Makes state transitions explicit.
3. Follows Open/Closed Principle — new states can be added without modifying existing code.

### Cons
1. Can lead to many small classes.
2. State transitions are often spread across state classes.

---

### Kotlin code

State interface

```kotlin
interface ConnectionState {
    fun connect()
    fun disconnect()
    fun sendData(data: String)
}
```

Context — Network Manager

```kotlin
class NetworkManager {
    private var state: ConnectionState = Disconnected(this)

    fun setState(state: ConnectionState) {
        this.state = state
    }

    fun connect() = state.connect()
    fun disconnect() = state.disconnect()
    fun sendData(data: String) = state.sendData(data)
}
```

Concrete States

```kotlin
class Disconnected(private val manager: NetworkManager) : ConnectionState {
    override fun connect() {
        println("Connecting...")
        manager.setState(Connecting(manager))
    }

    override fun disconnect() = println("Already disconnected")

    override fun sendData(data: String) = println("Cannot send data while disconnected")
}

class Connecting(private val manager: NetworkManager) : ConnectionState {
    override fun connect() = println("Already connecting")

    override fun disconnect() {
        println("Connection cancelled")
        manager.setState(Disconnected(manager))
    }

    override fun sendData(data: String) = println("Cannot send data while connecting")
}

class Connected(private val manager: NetworkManager) : ConnectionState {
    override fun connect() = println("Already connected")

    override fun disconnect() {
        println("Disconnecting...")
        manager.setState(Disconnected(manager))
    }

    override fun sendData(data: String) = println("Sending data: $data")
}
```

Client code

```kotlin
fun main() {
    val manager = NetworkManager()

    manager.connect()
    manager.connect()
    manager.sendData("Hello")
    manager.disconnect()
    manager.sendData("Bye")
}
```

---

### Android Use Case
- **Activity lifecycle**: `onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy` are state transitions.
- **Media player**: Idle → Prepared → Playing → Paused → Stopped.
- **Bluetooth**: Off → Scanning → Connected → Disconnected.