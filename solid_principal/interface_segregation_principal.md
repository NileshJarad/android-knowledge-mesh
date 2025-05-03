# Interface Segregation Principal


Clients should not be forced to depend on interfaces they do not use. This means that you should break up interfaces into smaller, more focused interfaces so that clients only need to implement the methods they care about.


### Bad Code Example

```kotlin
interface Worker {
    fun work()
    fun eat()
}

class HumanWorker : Worker {
    override fun work() {
        println("Human working")
    }

    override fun eat() {
        println("Human eating")
    }
}

class RobotWorker : Worker {
    override fun work() {
        println("Robot working")
    }

    override fun eat() {
        // ❌ Robot doesn’t eat — forced to implement irrelevant method
        throw UnsupportedOperationException("Robot doesn't eat")
    }
}

```
RobotWorker doesn’t need eat() — but it's forced to implement it, which violates ISP.



### Good Code Example

```kotlin
interface Workable {
    fun work()
}

interface Eatable {
    fun eat()
}

class HumanWorker : Workable, Eatable {
    override fun work() {
        println("Human working")
    }

    override fun eat() {
        println("Human eating")
    }
}

class RobotWorker : Workable {
    override fun work() {
        println("Robot working")
    }
}

```

- RobotWorker only implements what it needs.
- Code is clean, extendable, and respects Interface Segregation.