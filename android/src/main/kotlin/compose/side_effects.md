# Side Effects

In Jetpack Compose, side effects refer to operations that cause a change in the outside world or system state.

In the context of Jetpack Compose, side effects are necessary to interact with the environment, like triggering a network request, modifying a UI element, or interacting with a system service.

Simple words non compose thing in compose function is side effects.

Jetpack Compose provides a set of APIs for handling side effects, and knowing when and how to use them is crucial for writing maintainable and efficient applications.


## What is Launched Effect?
LaunchedEffect is a special composable that allows you to launch coroutines in response to changes in the composition or input keys.

It’s used when you want to perform a side effect (like a network call, delay, animation, etc.) only once or every time a key changes.

```kotlin
@Composable
fun SideEffectsExample() {
    val count by remember { mutableIntStateOf(0) }
    LaunchedEffect(key1 = count) { // Whenever count state changes this coroutine is cancelled and re-launched
        delay(100)
        println("")
    }
}
```

If key1, key2, etc. change, the block is canceled and restarted

-----

## What is rememberCoroutineScope?

`rememberCoroutineScope` gives you a `CoroutineScope` that is tied to the Composable’s lifecycle. It survives recompositions and is canceled when the Composable leaves the composition.

- Useful when you want to launch coroutines in response to events (e.g., button click).
- Unlike LaunchedEffect, it doesn’t launch automatically—you control when to use it.
- Mostly we will not using this as we have View Model to handle the states

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

-----

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

------


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

-----

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

------



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
------
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
