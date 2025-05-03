# Single Responsibility Principal 

A class should have only one reason to change. In other words, each class should do one thing and do it well.


### Bad Code example

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

In above example, the UserManager class is responsible for  creating , updating user in the database and sending a welcome email to the user. This violates the SRP because the class has more than one reason to change.


### Good Code Example

```kotlin
class UserDatabaseService {
    fun createUser(user: User?) {
        // create user in database
    }
    
    fun updateUser(user: User) {
        // update user
    }
}


class EmailService {
    fun sendWelcomeEmail(user: User?) {
        // send welcome email to user
    }
}


class UserManager {
    private val userDatabase: UserDatabaseService? = null
    private val emailService: EmailService? = null
    fun createUser(user: User?) {
        userDatabase!!.createUser(user)
        emailService!!.sendWelcomeEmail(user)
    }
}
```

In the improved code, the responsibilities of creating, updating a user and sending a welcome email have been separated into two separate classes (UserDatabase and EmailService). The UserManager class now delegates those responsibilities to the appropriate classes


>Single Responsibility principal does not mean Class should have single method. It can have multiple method as long as it adheres to single common responsibility in Above case UserDataBase 

