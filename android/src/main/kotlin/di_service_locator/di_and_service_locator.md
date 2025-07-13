# Service Locator and Dependency Injection (DI) Framework

## Service Locator

- A central registry (or locator) that provides dependencies on request.
- The class asks for its dependencies from the locator. (pull model)
- Testability- Lower as classes are tightly coupled to the locator.
- Considered an anti-pattern by some because it's like a global variable

```kotlin
object ServiceLocator {
    private val userRepository = UserRepositoryImpl()

    fun provideUserRepository(): UserRepository {
        return userRepository
    }
}

class UserViewModel {
    private val repo = ServiceLocator.provideUserRepository()
}
```

Note:

- Easy to implement
- Harder to test because UserViewModel cannot be easily tested with a fake UserRepository.

---

## DI Framework

- A technique where dependencies are provided to a class (usually via constructor, field, or method).
- The framework provides dependencies to the class. (push model)
- Testability- higher as you can inject mocks/stubs in tests.
- Clean code pattern endorsed by SOLID principles

```kotlin
class UserViewModel(private val repo: UserRepository)

val viewModel = UserViewModel(FakeUserRepository()) // for test
```

---

## Which one should I use?

| Situation                                         | Recommendation                        |
|---------------------------------------------------|---------------------------------------|
| Writing a small utility app                       | Service locator *may* be OK for speed |
| Writing testable, scalable production code        | Use **Dependency Injection**          |
| Using modern Android architecture (Jetpack, MVVM) | Use **Hilt** or **Koin**              |

------ 

## Koin
- Koin is a popular dependency injection (DI) framework for Kotlin, offering a modern and lightweight solution for managing your application’s dependencies with minimal boilerplate code.
- Koin supports both DI and the Service Locator pattern, offering flexibility to developers. However, it strongly encourages the use of DI, particularly constructor injection, where dependencies are passed as constructor parameters. This approach promotes better testability and makes your code easier to reason about.
- **Koin As SL (Service Locator)**
  - **Global Context Usage:** By default, Koin provides a globally accessible component that acts like a service locator. This allows you to retrieve dependencies from a central registry using `KoinComponent` or `inject` functions.
  - **SL in Android Components:** In Android development, Koin often uses SL internally within components such as Application and Activity for ease of setup. From this point, Koin recommends DI, especially constructor injection

------ 

## Hilt


------ 

## Dagger
- Dagger is fully static,  compile time dependency injection framework
- Annotation based DI

### Core Concepts of Dagger 2
- `@Inject` annotation 
  - By marking constructors with @Inject, Dagger knows how to build them.
  ```kotlin
  class Engine @Inject constructor() {
      fun start(): String = "Engine started"
  }
  
  class Car @Inject constructor(private val engine: Engine) {
      fun drive(): String = engine.start()
  }
  ```

- `@Module` and `@Provides`
  - When You Can't Use `@Inject` on Constructor
  - Some classes like `Retrofit`, `RoomDatabase`, or 3rd-party libraries can’t be modified or don’t have `@Inject` constructors. For such cases, use `@Module`.
  - Modules are used in Components
  ```kotlin
  class Wheels(val size: Int)
  
  @Module
  class WheelsModule {
      @Provides
      fun provideWheels(): Wheels {
          return Wheels(18)
      }
  }
  
  @Component(modules = [WheelsModule::class])
  interface CarComponent {
    fun getWheels(): Wheels
  }
  ```
  
- `@Component` and how it connects everything
  - It tells Dagger where and how to inject dependencies.
  - In below example Dagger will generate code for this interface → `DaggerCarComponent`.
  ```kotlin
  @Component
  interface CarComponent {
      fun getCar(): Car
  }
  
  fun main() {
    val carComponent = DaggerCarComponent.create()
    val car = carComponent.getCar()
    println(car.drive()) // Output: Engine started
  }
  ```



### Scoping In Dagger 2
- `@Singleton` Scope
  - A special scope provided by Dagger. Ensures only one instance is created and shared.

