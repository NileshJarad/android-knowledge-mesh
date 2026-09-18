# Single Responsibility Principle (SRP)

- **Definition**: A class should have only one reason to change. In other words, each class should do one thing and do it well.
- **Author**: Robert C. Martin (Uncle Bob)
- **Category**: SOLID Principles

### 🧠 Key Concept

A class should have only one responsibility, meaning it should have only one job or reason to change. When a class does too much, it violates SRP and becomes difficult to maintain, test, and understand.

---

### ❌ Bad Code Example

```kotlin
class UserManager {
    fun createUser(user: User) {
        // create user in database
    }
    
    fun updateUser(user: User) {
        // update user
    }

    fun sendEmail(user: User) {
        // send email to user
    }
}
```

**Problems:**
- `UserManager` handles both user persistence AND email communication
- Multiple reasons to change: database schema or email templates
- Violates SRP — the class has more than one responsibility

---

### ✅ Good Code Example

```kotlin
class UserDatabaseService {
    fun createUser(user: User) { /* persist user */ }
    fun updateUser(user: User) { /* update user */ }
}

class EmailService {
    fun sendWelcomeEmail(user: User) { /* send email */ }
}

class UserManager(
    private val userDatabase: UserDatabaseService,
    private val emailService: EmailService
) {
    fun createUser(user: User) {
        userDatabase.createUser(user)
        emailService.sendWelcomeEmail(user)
    }
}
```

**Improvements:**
- Separate classes for separate responsibilities
- Clean separation of concerns
- Easier to test each component independently
- Follows constructor injection (no `!!` operator)

---

### UML Diagram

![UML](single_responsibility_principle.puml)

---

### Java Code

```java
// User entity
public class User {
    private String name;
    private String email;
    
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }
    // getters...
}

// Persistence concern
public class UserDatabaseService {
    public void createUser(User user) { /* database logic */ }
    public void updateUser(User user) { /* database logic */ }
}

// Communication concern
public class EmailService {
    public void sendWelcomeEmail(User user) { /* email logic */ }
}

// Orchestrator
public class UserManager {
    private final UserDatabaseService userDatabase;
    private final EmailService emailService;

    public UserManager(UserDatabaseService userDatabase, EmailService emailService) {
        this.userDatabase = userDatabase;
        this.emailService = emailService;
    }

    public void createUser(User user) {
        userDatabase.createUser(user);
        emailService.sendWelcomeEmail(user);
    }
}
```

---

### 📱 Android Use Case

```kotlin
// In Android, SRP is naturally achieved with architecture components:
class UserRepository @Inject constructor(
    private val userDao: UserDao,           // Persistence
    private val apiService: ApiService      // Network
) {
    suspend fun getUser(): User { /* repository pattern naturally follows SRP */ }
}

// UI uses ViewModel - yet another single responsibility
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user
}
```

---

### ✅ Best Practices

1. **Identify cohesive responsibilities** — group related functionality
2. **Use dependency injection** — pass dependencies via constructor
3. **Keep classes small** — if it grows, it may have multiple responsibilities
4. **Follow "single verb"** — think of one primary action the class performs