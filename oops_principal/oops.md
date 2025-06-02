# Object-oriented principal

## Clas and object
- Class is blue-print or template that defines properties and behaviour
- Object is instance of class , created using class definition

## Encapsulation
- Encapsulates data and functions together as single unit.
- Protects the data from unauthorized access and modification.
- Provides a public interface for interaction.
- hiding the internal state and requiring all interactions to be performed through an object's methods.

```kotlin
class Car(private var speed: Int) {

    fun accelerate(amount: Int) {
        speed += amount
    }

    fun brake(amount: Int) {
        speed -= amount
    }

    fun getSpeed(): Int {
        return speed
    }
}
```
## Abstraction
- Exposes only relevant data and methods.
- Focuses on what an object does rather than how it does it.
-  simplifying complex reality by modeling classes based on the essential properties and behaviors of the objects
```kotlin
abstract class Car(val name: String, val year: Int) {
    abstract fun start()
    abstract fun stop()
}

class Sedan(name: String, year: Int) : Car(name, year) {
    override fun start() {
        println("$name - $year Sedan starts.")
    }

    override fun stop() {
        println("$name - $year Sedan stops.")
    }
}

class SUV(name: String, year: Int) : Car(name, year) {
    override fun start() {
        println("$name - $year SUV starts.")
    }

    override fun stop() {
        println("$name - $year SUV stops.")
    }
}
```

## Inheritance
- Mechanism that allows the class to inherit  properties and methods from another class
- Provide way to create class from another class
- It promotes code reuse
```kotlin
open class Vehicle(val brand: String, val model: String, val year: Int) {
    fun accelerate() {
        println("$brand $model is accelerating.")
    }

    fun brake() {
        println("$brand $model is braking.")
    }
}

class Car(brand: String, model: String, year: Int) : Vehicle(brand, model, year) {
    fun park() {
        println("$brand $model is parked.")
    }
}

class Truck(brand: String, model: String, year: Int, val payloadCapacity: Int) : Vehicle(brand, model, year) {
    fun loadCargo() {
        println("$brand $model is loading cargo with capacity $payloadCapacity kg.")
    }
}

```

## Polymorphism
- Ability to take multiple forms
- Types of polymorphism
  1. Dynamic polymorphism
     - Method overriding
  2. Static polymorphism
     - Method overloading
     - Operating overloading
```kotlin
open class Shape {
    open fun draw() {
        println("Drawing a shape")
    }
}

class Circle : Shape() {
    override fun draw() { // Method overriding 
        println("Drawing a circle")
    }
    
    fun draw(radius: Double) { // Method over loading same method name in same class
        println("Drawing a circle with radius $radius")
    }
}
```