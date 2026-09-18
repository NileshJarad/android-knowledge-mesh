# Dependency Inversion Principle (DIP)

- **Definition**: High-level modules should not depend on low-level modules. Both should depend on abstractions. Abstractions should not depend on details. Details should depend on abstractions.
- **Author**: Robert C. Martin (Uncle Bob)
- **Category**: SOLID Principles

### 🧠 Key Concept

The Dependency Inversion Principle states that we should depend on abstractions (interfaces or abstract classes) rather than concrete implementations. This decouples high-level modules from low-level modules, making the system more flexible, testable, and maintainable.

---

### ❌ Bad Code Example

```kotlin
class DatabaseService {
    fun connect() { /* ... */ }
    fun disconnect() { /* ... */ }
    fun executeQuery(query: String): List<Any> { /* ... */ }
}

class UserService {
    private val db = DatabaseService()

    fun getUsers(): List<User> {
        val query = "SELECT * FROM users"
        val results = db.executeQuery(query)
        return results.map { row -> User(row) }
    }
}
```

**Problems:**
- `UserService` directly depends on `DatabaseService` concrete class
- Switching to a different database requires modifying `UserService`
- Tight coupling makes testing difficult (can't mock without concrete class)
- Violates DIP — high-level module depends on low-level module

---

### ✅ Good Code Example

```kotlin
interface Database {
    fun connect()
    fun disconnect()
    fun executeQuery(query: String): List<Any>
}

class DatabaseService : Database {
    override fun connect() { /* ... */ }
    override fun disconnect() { /* ... */ }
    override fun executeQuery(query: String): List<Any> { /* ... */ }
}

class UserService(private val db: Database) {
    fun getUsers(): List<User> {
        val query = "SELECT * FROM users"
        val results = db.executeQuery(query)
        return results.map { row -> User(row) }
    }
}
```

**Improvements:**
- `UserService` depends on `Database` abstraction, not `DatabaseService` concrete class
- Easy to swap implementations (e.g., `CachedDatabase`, `RemoteDatabase`)
- Simple to test with mock implementations of `Database`
- Follows DIP — both layers depend on abstractions

---

### UML Diagram

![UML](dependency_inversion_principle.puml)

---

### Java Code

```java
// Abstraction (interface)
public interface Database {
    void connect();
    void disconnect();
    List<Object> executeQuery(String query);
}

// Low-level module implements abstraction
public class DatabaseService implements Database {
    @Override
    public void connect() { /* database connection */ }
    @Override
    public void disconnect() { /* close connection */ }
    @Override
    public List<Object> executeQuery(String query) { /* execute query */ }
}

// High-level module depends on abstraction
public class UserService {
    private final Database db;

    public UserService(Database db) {
        this.db = db;
    }

    public List<User> getUsers() {
        String query = "SELECT * FROM users";
        List<Object> results = db.executeQuery(query);
        return results.stream().map(User::new).collect(Collectors.toList());
    }
}

// Client code — easily swappable
public class Main {
    public static void main(String[] args) {
        Database db = new DatabaseService();
        UserService userService = new UserService(db);
        List<User> users = userService.getUsers();
    }
}
```

---

### 📱 Android Use Case

```kotlin
// In Android, DIP is everywhere with architecture components:

// Repository depends on abstraction (DAO interface), not concrete implementation
class UserRepository @Inject constructor(
    private val userDao: UserDao,  // Interface, not concrete class
    private val apiService: ApiService  // Interface, not concrete class
) {
    suspend fun getUser(id: String): User {
        // Can use local DB or network — swap implementations freely
        return userDao.getUserById(id) ?: apiService.getUser(id)
    }
}

// Room DAO is an interface — DIP in action
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?
}

// Retrofit API interface — DIP in action
interface ApiService {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): User
}
```

---

### ✅ Best Practices

1. **Depend on abstractions** — use interfaces, not concrete classes
2. **Use dependency injection** — pass dependencies via constructor
3. **Invert the call direction** — high-level modules define interfaces that low-level modules implement
4. **Make swapping easy** — different implementations should be interchangeable
5. **Follow the pattern**: `interface` → `implementor` → `consumer (depends on interface)`
