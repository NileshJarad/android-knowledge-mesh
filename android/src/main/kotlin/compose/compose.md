## What are annotations?

In Jetpack Compose, annotations are special markers you add to code to give the compiler extra instructions.

- Most important: `@Composable`
  This tells the compiler that the function can be used in Compose UI code.

    ```kotlin
    @Composable
    fun Greeting(name: String) {
        Text(text = "Hello, $name!")
    }
    
    ```


## What is a composable function?

A Composable function is a function marked with @Composable and is used to describe part of the UI.

- Think of it like a widget or view.
- Compose builds the UI by calling these functions

    ```kotlin
    @Composable
    fun MyButton() {
        Button(onClick = { /* Do something */ }) {
            Text("Click Me")
        }
    }
    
    ```

## What is Preview?

`@Preview` is an annotation that lets you see what your composable looks like inside Android Studio, without running the app.


```kotlin
    @Preview(showBackground = true)
    @Composable
    fun PreviewGreeting() {
        Greeting("Compose")
    }
 ```   

## What are containers? Box, Column, Row?

In Compose, containers are layout composables used to arrange children.

Box → Overlapping children (like FrameLayout).

Column → Vertically aligned children.

Row → Horizontally aligned children.

```kotlin
@Composable
fun LayoutExample() {
    Column {
        Text("Line 1")
        Text("Line 2")
    }
}

```

## What is LazyColum

In Jetpack Compose, a scrollable list can be made using the LazyColumn composable. The difference between a LazyColumn
and a Column is that a Column should be used when you have a small number of items to display, as Compose loads them all
at once. A Column can only hold a predefined, or fixed, number of composables. A LazyColumn can add content on demand,
which makes it good for long lists and particularly when the length of the list is unknown. A LazyColumn also provides
scrolling by default, without additional code. Declare a LazyColumn composable inside of the AffirmationList() function.
Pass the modifier object as an argument to the LazyColumn.

## What is a scaffold?
Scaffold provides a basic layout structure with slots like:

topBar, bottomBar, floatingActionButton, drawerContent, etc.

It’s like a pre-built layout skeleton that follows Material guidelines.

Example:

```kotlin
@Composable
fun MyScaffoldScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("My App") }) },
        floatingActionButton = { FloatingActionButton(onClick = {}) { Text("+") } }
    ) {
        Text("Hello, world!", modifier = Modifier.padding(it))
    }
}

```

## What is a Modifier?

`Modifier` is how you style and position UI elements in Compose.

Used for padding, size, alignment, background, click events, etc.

Example:

```kotlin
Text(
    text = "Styled Text",
    modifier = Modifier
        .padding(16.dp)
        .background(Color.Yellow)
)

```

Modifiers are chained, and the order matters!

## What is state hoist?
State hoisting is moving state from a Composable to its caller.

Makes the Composable stateless and reusable.

Encourages separation of UI and business logic.

**Without hoisting:**

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}

```

**With hoisting:**

```kotlin
@Composable
fun Counter(count: Int, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("Count: $count")
    }
}

```


## What is composition?
Composition is the process of describing the UI by calling composable functions.

```java
@Composable
fun Greeting(name: String) {
    Text("Hello, $name!")
}

```
## What is the composition cycle? enter -> recompose -> exit
- Enter: When a composable enters the composition. 
- Recompose: When input to a composable changes, it re-runs to reflect new data. 
- Exit: When it's no longer needed and removed from the composition.

In below example each button click will trigger Re-composition
```kotlin
@Composable
fun CounterExample() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Clicked $count times")
    }
}

```

## What is recomposition?

Recomposition is the process where Compose re-executes a composable function to update the UI based on state changes.


## How does recomposition trigger?
When a @Composable function reads a state and that state changes, recomposition is triggered automatically.


## When recomposition trigger?

- When a State object changes. 
- When derivedStateOf changes. 
- When a lambda passed as a parameter changes.

## What is the state of your app, state of data, and state wrapper?
- App state: High-level state like login, theme, etc. 
- Data state: UI-specific data like a list of items. 
- State wrapper: Compose provides wrappers like mutableStateOf, remember, State<T>.


## What is mutableStateOf?
It creates an observable state object.

```kotlin
var name by remember { mutableStateOf("Jetpack") }
```

## What is remember? 
The remember function helps store a value in memory across recompositions. 

However, it does not persist the state during configuration changes, such as screen rotation or process recreation.

```kotlin
@Composable
fun RememberExample() {
    val count = remember { mutableStateOf(0) }
    Button(onClick = { count.value++ }) {
        Text("Count: ${count.value}")
    }
}
```

## In how many ways can you use remember?

- With mutableStateOf 
- With derived state 
- With lambdas 
- With any computed value
```kotlin
val greeting = remember { "Hello!" }
val derived = remember(count) { count * 2 }
```

## What is rememberSaveable? 

rememberSaveable is an extension of remember that retains the state across configuration changes by saving it into a Bundle, which is part of Android’s saved instance state mechanism.

```kotlin
val count = rememberSaveable { mutableStateOf(0) }
```

## What is Saver?
A Saver helps rememberSaveable store and restore complex objects by converting them into a format that can be saved into a Bundle (e.g., a Map or List).


### Steps to Create a Custom Saver:
- Define the Object You Want to Save:
  - Create a data class or object for your state.
- Implement a Saver:
  - Write a Saver that converts the object into a savable format and restores it when needed.
- Use the Saver in rememberSaveable:
  - Pass the Saver to rememberSaveable manage your custom object’s state

```kotlin
data class User(val name: String, val age: Int)

val UserSaver = Saver<User, Map<String, Any>>(
    save = { mapOf("name" to it.name, "age" to it.age) },
    restore = { User(it["name"] as String, it["age"] as Int) }
)

@Composable
fun CustomSaverExample() {
    var user by rememberSaveable(stateSaver = UserSaver) {
        mutableStateOf(User(name = "Akshay", age = 28))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Name: ${user.name}, Age: ${user.age}")
        Button(onClick = { user = user.copy(age = user.age + 1) })  
        { Text("Increase Age") }
     }
}
```

## How does rememberSaveable internally work?
- Uses Android’s SavedStateRegistry. 
- Converts the state into a Bundle via a Saver. 
- Restores it after config changes.

-------

## Testing

What is composite Test rule