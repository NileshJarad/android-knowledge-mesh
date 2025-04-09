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

## What are the Side Effects?

In Jetpack Compose, side effects refer to operations that cause a change in the outside world or system state.

In the context of Jetpack Compose, side effects are necessary to interact with the environment, like triggering a network request, modifying a UI element, or interacting with a system service.

Jetpack Compose provides a set of APIs for handling side effects, and knowing when and how to use them is crucial for writing maintainable and efficient applications.



## What is Launched Effect?
LaunchedEffect is a special composable that allows you to launch coroutines in response to changes in the composition or input keys.

It’s used when you want to perform a side effect (like a network call, delay, animation, etc.) only once or every time a key changes.

```kotlin
LaunchedEffect(key1, key2, ...) {
    // Coroutine scope here
}
```

If key1, key2, etc. change, the block is canceled and restarted

## What is SideEffect Block?

SideEffect is a Composable function that allows you to run non-suspending, synchronous code after every successful recomposition.

It’s useful when you want to interact with parts of your app that are outside of Compose, like:

- Updating external classes 
- Logging 
- Analytics 
- Triggering callbacks or changes to imperative code

📌 It always runs after a recomposition completes successfully.

**Real Use Case: Update an external object**
```kotlin
class UserTracker {
    var currentScreen: String = ""
}

val tracker = UserTracker()

@Composable
fun HomeScreen() {
    SideEffect {
        tracker.currentScreen = "Home"
    }

    Text("Home")
}
```

**What happens if you don't use SideEffect?**

```kotlin
@Composable
fun WrongExample(userName: String) {
    tracker.currentUser = userName // ❌ Do not update during composition!
    Text("Hello $userName")
}
```



## What is rememberUpdatedState?

**rememberUpdatedState** is a side-effect utility in Jetpack Compose that lets you "remember" the latest value of a variable across recompositions, especially inside long-lived side-effect scopes like LaunchedEffect, DisposableEffect, or produceState.

It helps prevent stale values being captured by lambdas or coroutines in Compose.

**Why we need?**

In Compose, recomposition can happen often. If you launch a coroutine (e.g., in LaunchedEffect) and capture a lambda or value before recomposition, you might end up using old or outdated values.

rememberUpdatedState ensures your coroutine or callback always accesses the latest version of that value or lambda.

**Basic Example (Without rememberUpdatedState)**

```kotlin
@Composable
fun Greeting(onHello: () -> Unit) {
    // Suppose onHello changes due to recomposition

    LaunchedEffect(Unit) {
        delay(5000)
        onHello() // ⚠️ Might call the OLD lambda if recomposition happened
    }
}
```
Above code might call an outdated onHello after 5 seconds if recomposition changed it in the meantime.

**Fixed Example (With rememberUpdatedState)**

```kotlin
@Composable
fun Greeting(onHello: () -> Unit) {
    val currentOnHello by rememberUpdatedState(newValue = onHello)

    LaunchedEffect(Unit) {
        delay(5000)
        currentOnHello() // ✅ Always calls the latest lambda
    }
}
```
`rememberUpdatedState` keeps track of the most recent onHello.

Even if `onHello` changes, `currentOnHello` will always reflect the new value.

## What is DisposableEffect?

`DisposableEffect` is used when you need to perform side effects with cleanup. It runs a block of code when the Composable enters the composition and provides a onDispose callback for cleanup when the Composable is removed or a key changes.
- Use it for setting up and cleaning up listeners, broadcast receivers, observers, etc.
- Re-run and clean up when the key changes.

**Example**
```kotlin
@Composable
fun NetworkListener() {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Handle network change
            }
        }

        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(receiver, intentFilter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}
```

## What is rememberCoroutineScope?

`rememberCoroutineScope` gives you a `CoroutineScope` that is tied to the Composable’s lifecycle. It survives recompositions and is canceled when the Composable leaves the composition.

- Useful when you want to launch coroutines in response to events (e.g., button click). 
- Unlike LaunchedEffect, it doesn’t launch automatically—you control when to use it.

**Example**

```kotlin
@Composable
fun SaveButton() {
    val coroutineScope = rememberCoroutineScope()

    Button(onClick = {
        coroutineScope.launch {
            // Do something suspendable, like saving data
            saveDataToDatabase()
        }
    }) {
        Text("Save")
    }
}
```


## What is derivedStateOf?

`derivedStateOf` is used to create computed values that only recompute when their inputs change. This helps avoid unnecessary recompositions by memoizing the derived value.
- Only recalculates the result when the dependent state(s) actually change. 
- Optimizes performance in scenarios like filtering, sorting, or formatting data.

**Example**
```kotlin
@Composable
fun FilteredList(searchQuery: String, items: List<String>) {
    val filteredItems by remember(items, searchQuery) {
        derivedStateOf {
            items.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    LazyColumn {
        items(filteredItems) { item ->
            Text(item)
        }
    }
}
```

## What is produceState?
`produceState` allows you to launch a coroutine in the composition and expose its result as a State, which can then be observed by the UI. It’s especially handy when fetching data asynchronously and binding it to UI state.

- Coroutine launched inside it is automatically canceled when the Composable leaves the composition. 
- Good for integrating APIs, database queries, or long-running jobs into Compose.

**Example**

```kotlin
@Composable
fun UserProfile(userId: String) {
    val userState by produceState<User?>(initialValue = null, userId) {
        value = fetchUserFromNetwork(userId) // suspending function
    }

    if (userState == null) {
        CircularProgressIndicator()
    } else {
        Text("Hello, ${userState!!.name}")
    }
}
```

## What is the order of execution of all?

1. SideEffect

Called after every successful recomposition.

Useful for committing side-effects that must run after Compose has applied changes to the UI tree.

1. SideEffect
   - Called after every successful recomposition. 
   - Useful for committing side-effects that must run after Compose has applied changes to the UI tree.

2. LaunchedEffect
   - Called after the first composition or when the key(s) change.
   - Starts a coroutine that runs independently from recompositions unless keys change.

3. DisposableEffect
   - Called after the first composition or when the key(s) change.
   - Also provides a onDispose callback for cleanup before the next effect or when leaving composition.

4. produceState
   - Runs a coroutine to initialize and update a State object from suspend functions or async data sources.
   - Typically starts after the composition, like LaunchedEffect, but is specifically meant to emit State.

5. rememberUpdatedState
   - Always runs during recomposition.
   - Used to hold the latest lambda or value to avoid stale captures in long-running effects (e.g., in LaunchedEffect or callbacks).

6. rememberCoroutineScope
   - Initializes once per composition; provides a coroutine scope but doesn’t launch anything itself.

7. derivedStateOf
  - Used during recomposition.

Only recomputes when its dependencies change, for optimal performance

## When to use which side effect?

| **Use Case**                          | **Use This**             |
|---------------------------------------|--------------------------|
| Start coroutine when key changes      | `LaunchedEffect`         |
| Trigger code after each recomposition | `SideEffect`             |
| Cleanup resources on exit             | `DisposableEffect`       |
| Use latest value in long-running code | `rememberUpdatedState`   |
| Launch coroutine manually             | `rememberCoroutineScope` |
| Derive value from state               | `derivedStateOf`         |
| Convert async data to state           | `produceState`           |

---

## Summary

These side-effect APIs in Jetpack Compose help manage lifecycle-aware and state-aware operations. Use them to handle side-effects properly during recompositions and state updates.


## Testing

What is composite Test rule