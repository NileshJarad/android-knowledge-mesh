# Android-knowledge-mesh

## GIT

1. [Clone](git/git.md#clone)
2. [Status](git/git.md#status)
3. [Checkout](git/git.md#checkout)
4. [Changes and Commit](git/git.md#changes-and-commit)
5. [Push](git/git.md#push)
6. [Log](git/git.md#log)
7. [History re-write](git/git.md#history-re-write)
8. [Clean up files](git/git.md#clean-up-files)


## Algorithm 
1. Time complexity 
2. [How to Avoid the TLE (Time Limit Exceeded)](./data_structure/src/main/java/algorithms/avoid_tle.md)


## Sorting
1. **[Selection sort](data_structure/src/main/java/sorting/selection_sort.md)** - Find the minimum element in the unsorted portion of the array and swap it with the first unsorted element. 
2. **[Bubble sort](data_structure/src/main/java/sorting/bubble_sort.md)** - comparing adjacent pairs of elements and swapping them if they are in the wrong order. After each pass, the largest unsorted element is placed in its correct position
2. **[Insertion sort](data_structure/src/main/java/sorting/insertion_sort.md)** - Build up a sorted sub-array from left to right by inserting each new element into its correct position in the sub-array. Repeat until the array is fully sorted.

## Data Structure

1. [Array](data_structure/src/main/java/array/array.md)
2. [Bitwise Operator](data_structure/src/main/java/bit_manipulation/bitwise_operator.md)
3. [LinkList](data_structure/src/main/java/linklist/linklist.md)
4. [Recursion](data_structure/src/main/java/recursion/recursion.md)
5. [HashTable - (Hashing)](data_structure/src/main/java/hashing/hasshing.md)
6. [Stack](data_structure/src/main/java/stack/stack.md)
7. [Queue](data_structure/src/main/java/queue/queue.md)
8. [Tree](data_structure/src/main/java/tree/tree.md)
9. [Heap](data_structure/src/main/java/heap_priority_queue/heap.md)
10. [Graph](data_structure/src/main/java/graph/graph.md)
11. **Java Collections**
    1. [HashMap](data_structure/src/main/java/collections/hashmap.md)

---

## Object-Oriented Principals (OOPS)
1. [Class and object](oops_principal/oops.md#clas-and-object)
2. [Encapsulation](oops_principal/oops.md#encapsulation)
3. [Abstraction](oops_principal/oops.md#abstraction)
4. [Inheritance](oops_principal/oops.md#inheritance)
5. [Polymorphism](oops_principal/oops.md#polymorphism)


---

## SOLID Principal

1. [Single Responsibility Principal](solid_principal/single_responsibility_principal.md)
2. [Open Closed Principal](solid_principal/open_closed_principal.md)
3. [Liskov Substitution Principal](solid_principal/liskov_substitution_principal.md)
4. [Interface Segregation Principal](solid_principal/interface_segregation_principal.md)
5. [Dependency Inversion Principal](solid_principal/dependency_inversion_principal.md)

---

## Object oriented design

1. [Class Diagram to UML representation](ood/class_uml_relationship.md)
2. [Usecase diagram](ood/usecase.md)

## Design patterns

---

### Creational design pattern

These patterns are used to provide a mechanism for creating objects in a specific situation without revealing the
creation method.

1. **[Singleton](design_patterns/creational/singletone/singletone.md)** Lets you ensure that a class has only one
   instance,
   while providing a global access point to this instance.
2. **[Factory-method](design_patterns/creational/factory/factory_method.md)**  Provides an interface for creating
   objects in
   a superclass, but allows subclasses to alter the type of objects that will be created.
3. **[Abstract-factory](design_patterns/creational/abstract_factory/abstract_factory.md)** Lets you produce families of
   related objects without specifying their concrete classes.
4. **[Builder](design_patterns/creational/builder/builder.md)** Lets you construct complex objects step by step. The
   pattern
   allows you to produce different types and representations of an object using the same construction code.
5. **[Prototype(Clone)](design_patterns/creational/prototype/prototype.md)** Lets you copy existing objects without
   making
   your code dependent on their classes.

### Structural design patterns

These patterns are concerned with class/object composition and relationships between objects. They let us add new
functionalities to objects so that restructuring some parts of the system does not affect the rest. Hence, when some
parts of the structure change, the entire system does not need to change

### Behavioral design pattern

These patterns are concerned with communication between objects in a system. They streamline communication
and ensure that the information is synchronized objects

1. **[Strategy](design_patterns/behavioral/strategy/strategy.md)** Lets you define a family of algorithms, put each of
   them into a separate class, and make their objects interchangeable.
2. **[Observer](design_patterns/behavioral/observer/observer.md)** Lets you define a subscription mechanism to notify
   multiple objects about any events that happen to the object they're observing.
3. **[Iterator](design_patterns/behavioral/observer/observer.md)** Lets you traverse elements of a collection without
   exposing its underlying representation (list, stack, tree, etc.).

---

## System Design

### LLD(Low level design)

1. Library Management
    1. [Requirement](system_design/lld/library_management/requirment.md)
    2. [Use case and class diagram](system_design/lld/library_management/usercase_class_diagram.md)
    3. [Code](system_design/lld/library_management/code.md)
2. Car Rental Management
    1. [Requirement](system_design/lld/car_rental_system/requirement.md)
    2. [Use case and class diagram](system_design/lld/car_rental_system/usecase_class_diagram.md)
    3. [Code](system_design/lld/car_rental_system/code.md)
3. Parking Lot
   1. [Requirement](system_design/lld/parking_lot/requirement.md)
   2. [Usecase and Class diagram](system_design/lld/parking_lot/usecase_class_diagram.md)
   3. [Code](system_design/lld/parking_lot/code.md)

---

## Android

1. Android Component
    - Activity
    - Service
    - Broadcast Receiver
    - Content Provider
2. Intent
    - Types of intent
    - Intent Filter
3. Launch mode
    - Standard
    - Single Top
    - Single Task
    - Single Instance
4. Fragments
    - Fragment Lifecycle
5. Storage
    - Shared Preference
    - Database
    - File
6. Threading
    - Thread
    - Handler
    - Looper
    - Message Queue
    - Executor
    - RxJava
    - Co-routine
7. [Networking](android/src/main/kotlin/networking/networking.md)
    - REST API
    - [HTTP status code](android/src/main/kotlin/networking/networking.md#http-status-code)
    - Multi-part requests
    - OkHttp
    - Interceptors
    - Caching
    - Socket connection
    - Server Side Event (SSE)
    - Auth refresh tokens
8. Security
    - SSL pining
9. Android Jetpack
10. Compose
11. Kotlin
    - [Basics](android/src/main/kotlin/kotlin_fundamentals/basics.md)
    - Functions
        - [Higher-order functions](android/src/main/kotlin/kotlin_fundamentals/functions.md#higher-order-function)
        - [Scope functions](android/src/main/kotlin/kotlin_fundamentals/scope_functions.md)
            - [let](android/src/main/kotlin/kotlin_fundamentals/scope_functions.md#let)
            - [run](android/src/main/kotlin/kotlin_fundamentals/scope_functions.md#run)
            - [with](android/src/main/kotlin/kotlin_fundamentals/scope_functions.md#with)
            - [apply](android/src/main/kotlin/kotlin_fundamentals/scope_functions.md#apply)
            - [also](android/src/main/kotlin/kotlin_fundamentals/scope_functions.md#also)
        - [Lambda Functions](android/src/main/kotlin/kotlin_fundamentals/functions.md#lambda-function)
        - [Extension function](android/src/main/kotlin/kotlin_fundamentals/functions.md#extension-functions)
        - [Infix functions](android/src/main/kotlin/kotlin_fundamentals/functions.md#infix-function)
    - [Class](android/src/main/kotlin/kotlin_fundamentals/classes.md)
        - [Data class](android/src/main/kotlin/kotlin_fundamentals/classes.md#data-class)
        - [Sealed Class](android/src/main/kotlin/kotlin_fundamentals/classes.md#sealed-class)
        - [Enum class](android/src/main/kotlin/kotlin_fundamentals/classes.md#enum-class)
        - [value class](android/src/main/kotlin/kotlin_fundamentals/classes.md#value-class)


12. [Coroutine](android/src/main/kotlin/coroutine/coroutine.md)


#### Awesome Behavioral Interviews - https://github.com/ashishps1/awesome-behavioral-interviews


