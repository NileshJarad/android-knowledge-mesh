# Template Method Pattern

- **Template Method is a Behavioral design pattern that defines the skeleton of an algorithm in an operation, deferring some steps to subclasses.**
- It lets subclasses redefine certain steps of an algorithm without changing the algorithm's structure.

### Use Case
- Activity lifecycle in Android (`onCreate` → `onStart` → `onResume`)
- Algorithm skeletons: data processing pipelines, sorting with custom comparison
- Code generation tools

### Steps to create Template Method

1. **Abstract Class** : Defines the template method with the algorithm skeleton.
2. **Concrete Class** : Overrides specific steps of the algorithm.
3. **Client** : Calls the template method on the abstract class.

### Pros
1. Avoids code duplication — common logic lives in the base class.
2. Follows Open/Closed Principle — new subclasses extend without modifying the base.
3. Easy to add new variants.

### Cons
1. Increases the number of classes.
2. Can be hard to override the right method.

---

### Kotlin code

Abstract Class with Template Method

```kotlin
abstract class AbstractDataProcessor {
    open fun process() {
        val data = readData()
        val transformed = transform(data)
        save(transformed)
        log(transformed)
    }

    open fun readData(): String = "Raw data from source"
    open fun transform(data: String): String = data.uppercase()
    open fun save(data: String) = println("Saving: $data")
    open fun log(data: String) = println("Logging: $data")
}
```

Concrete Classes

```kotlin
class CsvProcessor : AbstractDataProcessor() {
    override fun readData(): String = "CSV rows from file"
    override fun transform(data: String): String = "[CSV] $data"
    override fun log(data: String) = println("CSV Log: $data")
}

class JsonProcessor : AbstractDataProcessor() {
    override fun readData(): String = "JSON from API"
    override fun transform(data: String): String = "{ \"data\": \"$data\" }"
    override fun save(data: String) = println("Saving JSON to DB: $data")
}
```

Client code

```kotlin
fun main() {
    val csvProcessor = CsvProcessor()
    csvProcessor.process()
    println("---")
    val jsonProcessor = JsonProcessor()
    jsonProcessor.process()
}
```

---

### Android Use Case
- **Activity**: `onCreate` → `setContentView` → `onStart` → `onResume` is a template method.
- **AsyncTask**: `onPreExecute` → `doBackgroundWork` → `onProgressUpdate` → `onPostExecute`.
---

### Java code

Abstract Class

```java
public abstract class AbstractDataProcessor {
    public void process() {
        String data = readData();
        String transformed = transform(data);
        save(transformed);
        log(transformed);
    }

    protected String readData() { return "Raw data from source"; }
    protected String transform(String data) { return data.toUpperCase(); }
    protected void save(String data) { System.out.println("Saving: " + data); }
    protected void log(String data) { System.out.println("Logging: " + data); }
}
```

Concrete Classes

```java
public class CsvProcessor extends AbstractDataProcessor {
    @Override
    protected String readData() { return "CSV rows from file"; }

    @Override
    protected String transform(String data) { return "[CSV] " + data; }

    @Override
    protected void save(String data) { System.out.println("Saving CSV to DB: " + data); }

    @Override
    protected void log(String data) { System.out.println("CSV Log: " + data); }
}

public class JsonProcessor extends AbstractDataProcessor {
    @Override
    protected String readData() { return "JSON from API"; }

    @Override
    protected String transform(String data) { return "{ \"data\": \"" + data + "\" }"; }

    @Override
    protected void save(String data) { System.out.println("Saving JSON to DB: " + data); }

    @Override
    protected void log(String data) { System.out.println("JSON Log: " + data); }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        AbstractDataProcessor csvProcessor = new CsvProcessor();
        csvProcessor.process();
        System.out.println("---");
        AbstractDataProcessor jsonProcessor = new JsonProcessor();
        jsonProcessor.process();
    }
}
```
