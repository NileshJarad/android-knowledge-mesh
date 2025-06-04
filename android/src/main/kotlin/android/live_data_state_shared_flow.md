# Live Data , State Flow and Share Flow


## Live Data
**LiveData** is an observable data holder class. Unlike a regular observable, LiveData is lifecycle-aware, meaning it respects the lifecycle of other app components, such as activities, fragments, or services. This awareness ensures LiveData only updates app component observers that are in an active lifecycle state.


**LiveData** considers an observer, which is represented by the Observer class, to be in an active state if its lifecycle is in the STARTED or RESUMED state. LiveData only notifies active observers about updates. Inactive observers registered to watch LiveData objects aren't notified about changes.

### Ways to set data in LiveData

1. **setValue(T value)**
    - Must be called from the main (UI) thread.
    - Sets the value immediately and notifies all active observers synchronously.
2. **postValue(T value)** - 
   - Can be called from any thread (background or main).
   - Posts the value asynchronously to the main thread. If multiple postValue calls happen in quick succession, only the last value is delivered.

### Declare the Live Data
```kotlin
class NameViewModel : ViewModel() {

    // Create a LiveData with a String
    val currentName: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
   
   // or 
   val currentName: MutableLiveData<String> = MutableLiveData()
   
   // or with default value
   val currentName: MutableLiveData<String> = MutableLiveData("NJ")


   // Rest of the ViewModel...
}

```

### Observe Live Data

```kotlin
 // Create the observer which updates the UI.
val nameObserver = Observer<String> { newName ->
    // Update the UI, in this case, a TextView.
    nameTextView.text = newName
}

// Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
model.currentName.observe(this, nameObserver)

Or

val liveData = MutableLiveData<String>()
liveData.observe(viewLifecycleOwner) { data ->
    // UI update
}

```


-----

## StateFlow 
* Hot Flow from Kotlin Coroutines
* Holds a state (always has a current value)
* Similar to LiveData but fully coroutine-based



### Ways to set data to StateFlow 

1. **.value**
    - The simplest way to set or update the current value.
    - immediately updates the value and emits it to collectors.
    ```kotlin
        val stateFlow = MutableStateFlow<Int>(0)
        
        // Update value
        stateFlow.value = 42
     ```
2. **.update {}** -
    - A thread-safe and atomic way to update the value based on the current one.
    - Useful for transformations that depend on the current value.
    
    ```kotlin
     stateFlow.update { currentValue ->
            currentValue + 1
        }
    ```



### Declare StateFlow:

```kotlin
val _stateFlow = MutableStateFlow("Initial")
val stateFlow: StateFlow<String> = _stateFlow

viewModelScope.launch {
    _stateFlow.value = "New Value"
}

```

### Observe / Collect

```kotlin
// Collecting in Activity (lifecycleScope required)
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.stateFlow.collect { data ->
            // update UI
        }
    }
}
```

---

## SharedFlow

* A hot flow that does not store state by default.
* Designed for events, one-time actions, and shared emissions.

### Setting Values in SharedFlow

1. Using emit(value: T) (suspend function)
   - This is the primary way to send values to a MutableSharedFlow. 
   - Suspend function → must be called inside a coroutine. 
   - Suspends if the buffer is full (depending on replay and buffer size).
    ```kotlin
    val sharedFlow = MutableSharedFlow<String>()
    
    // Inside coroutine
    sharedFlow.emit("Hello from SharedFlow")
    
    ```
2. Using tryEmit(value: T) (non-suspending function)
   - Attempts to emit a value immediately without suspension. 
   - Returns true if emission was successful, false otherwise (e.g., buffer full). 
   - Useful for fire-and-forget emissions where you don't want to suspend.
    ```kotlin
    val sharedFlow = MutableSharedFlow<String>(
         replay = 2,                    // Replays last 2 values to new collectors
         extraBufferCapacity = 64,     // Allows 64 more values beyond replay
         onBufferOverflow = BufferOverflow.DROP_OLDEST // Optional strategy
    )
    
    val emitted = sharedFlow.tryEmit("Try emit message")
    if (!emitted) {
        // handle failed emission (optional)
    }
    
    ```



### Declare SharedFlow:

```kotlin
val _sharedFlow = MutableSharedFlow<String>(replay = 0)
val sharedFlow: StateFlow<String> = _sharedFlow

viewModelScope.launch {
    _sharedFlow.emit("New Value")
}

```

### Observe / Collect

```kotlin
// Collecting in Activity (lifecycleScope required)
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.sharedFlow.collect { data ->
            // update UI
        }
    }
}
```
