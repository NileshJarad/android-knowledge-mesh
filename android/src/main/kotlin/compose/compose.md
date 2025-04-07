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

## Testing

What is composite Test rule