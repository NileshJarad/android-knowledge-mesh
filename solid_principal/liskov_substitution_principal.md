# Liskov Substitution Principal

Subtypes must be substitutable for their base types. In simpler terms, any instance of a parent class should be able to be replaced by an instance of one of its child classes without affecting the correctness of the program.


### Bad Code Example

```kotlin
open class Bird {
    open fun fly() {
        println("Bird is flying")
    }
}

class Ostrich : Bird() {
    override fun fly() {
        // ❌ Ostriches can't fly — this breaks expected behavior
        throw UnsupportedOperationException("Ostrich can't fly")
    }
}

fun makeBirdFly(bird: Bird) {
    bird.fly()
}

```

- Ostrich is a Bird, but it can’t fly.
- Calling makeBirdFly(Ostrich()) will crash — it violates the expectations of the base type.

### Good Code Example

```kotlin
interface Bird {
    fun eat()
}

interface FlyingBird : Bird {
    fun fly()
}

class Sparrow : FlyingBird {
    override fun eat() {
        println("Sparrow eating")
    }

    override fun fly() {
        println("Sparrow flying")
    }
}

class Ostrich : Bird {
    override fun eat() {
        println("Ostrich eating")
    }
}

fun makeBirdFly(bird: FlyingBird) {
    bird.fly()
}

```


- Sparrow can fly.
- Ostrich doesn't implement fly() — so can't be passed to makeBirdFly(). 
- LSP is respected.