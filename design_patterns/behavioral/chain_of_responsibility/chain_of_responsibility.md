# Chain of Responsibility Pattern

- **Chain of Responsibility is a Behavioral design pattern that lets you pass requests along a chain of handlers.**
- Each handler decides either to process the request or pass it to the next handler.

### Use Case
- Logging (loggers at different levels: DEBUG, INFO, ERROR)
- Android: `View` touch event dispatching
- Request routing in HTTP servers
- Filters in web applications

### Steps to create Chain of Responsibility

1. **Handler** : Declares the interface for handling requests.
2. **Concrete Handlers** : Handle requests or pass them to the next handler.
3. **Client** : Assembles the chain and sends requests.

### Pros
1. Decouples sender and receiver.
2. Makes it easy to add new handlers.
3. Order of handling can be changed dynamically.

### Cons
1. A request may go unhandled if no handler processes it.
2. Debugging can be hard with long chains.

---

### Kotlin code

Handler abstract class

```kotlin
abstract class Handler {
    private var nextHandler: Handler? = null

    fun setNext(handler: Handler): Handler {
        this.nextHandler = handler
        return handler
    }

    fun handle(request: Request): Boolean {
        if (canHandle(request)) {
            process(request)
            return true
        }
        return nextHandler?.handle(request) ?: false
    }

    abstract fun canHandle(request: Request): Boolean
    abstract fun process(request: Request)
}
```

Concrete Handlers

```kotlin
class DebugHandler : Handler() {
    override fun canHandle(request: Request): Boolean = request.level == "DEBUG"
    override fun process(request: Request) = println("DEBUG: ${request.message}")
}

class InfoHandler : Handler() {
    override fun canHandle(request: Request): Boolean = request.level == "INFO"
    override fun process(request: Request) = println("INFO: ${request.message}")
}

class ErrorHandler : Handler() {
    override fun canHandle(request: Request): Boolean = request.level == "ERROR"
    override fun process(request: Request) = println("ERROR: ${request.message}")
}
```

Request data class

```kotlin
data class Request(val level: String, val message: String)
```

Client code

```kotlin
fun main() {
    val debugHandler = DebugHandler()
    val infoHandler = InfoHandler()
    val errorHandler = ErrorHandler()

    // Build the chain
    debugHandler.setNext(infoHandler).setNext(errorHandler)

    debugHandler.handle(Request("DEBUG", "Debug message"))
    debugHandler.handle(Request("INFO", "Info message"))
    debugHandler.handle(Request("ERROR", "Error message"))
}
```

### Java code

Handler abstract class

```java
abstract class Handler {
    private Handler nextHandler;

    public Handler setNext(Handler handler) {
        this.nextHandler = handler;
        return handler;
    }

    public boolean handle(Request request) {
        if (canHandle(request)) {
            process(request);
            return true;
        }
        return nextHandler != null && nextHandler.handle(request);
    }

    abstract boolean canHandle(Request request);
    abstract void process(Request request);
}
```

Concrete Handlers

```java
class DebugHandler extends Handler {
    @Override
    boolean canHandle(Request request) { return request.level.equals("DEBUG"); }
    @Override
    void process(Request request) { System.out.println("DEBUG: " + request.message); }
}

class InfoHandler extends Handler {
    @Override
    boolean canHandle(Request request) { return request.level.equals("INFO"); }
    @Override
    void process(Request request) { System.out.println("INFO: " + request.message); }
}

class ErrorHandler extends Handler {
    @Override
    boolean canHandle(Request request) { return request.level.equals("ERROR"); }
    @Override
    void process(Request request) { System.out.println("ERROR: " + request.message); }
}
```

Request class

```java
class Request {
    final String level;
    final String message;

    Request(String level, String message) {
        this.level = level;
        this.message = message;
    }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        Handler debugHandler = new DebugHandler();
        Handler infoHandler = new InfoHandler();
        Handler errorHandler = new ErrorHandler();

        // Build the chain
        debugHandler.setNext(infoHandler).setNext(errorHandler);

        debugHandler.handle(new Request("DEBUG", "Debug message"));
        debugHandler.handle(new Request("INFO", "Info message"));
        debugHandler.handle(new Request("ERROR", "Error message"));
    }
}
```

---

### Android Use Case
- **View.dispatchTouchEvent**: The touch event passes through the view hierarchy — each view handles or passes it.
- **Activity.onRequestPermissionsResult**: Permissions pass through the chain of fragments/activities.
- **Logger chains**: `Log.d`, `Log.i`, `Log.e` — handlers at different levels.