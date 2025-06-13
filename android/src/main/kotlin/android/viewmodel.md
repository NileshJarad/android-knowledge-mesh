# ViewModel

A ViewModel is a component from Android Jetpack’s Architecture Components. It is designed to store and manage UI-related data in a lifecycle-conscious way. The ViewModel survives configuration changes such as screen rotations, so your data does not need to be reloaded or recalculated.

### Why Use ViewModel?

When an Activity or Fragment is recreated (like during screen rotation), its data (like lists, UI state) is lost unless it's saved manually. A ViewModel allows you to:
 - Avoid unnecessary re-fetching or recalculating data. 
 - Decouple UI data from the view logic. 
 - Easily share data between fragments.

 ---

### How ViewModel Works Internally

**Basic Flow:**
- **Creation**: When an Activity or Fragment is created, it can request a ViewModel from a ViewModelProvider.
- **Storage**: Android uses a special internal object called ViewModelStore (attached to the Activity or Fragment) to keep the ViewModel.
- **Retention**: When the configuration changes (e.g., rotation), the Activity/Fragment is destroyed and recreated, but ViewModelStore persists, and so does your ViewModel.
- **Reuse**: The new instance of the Activity or Fragment gets the same ViewModel from the ViewModelProvider.

---

### Scope of ViewModel
- `ViewModelProvider(this)` → scoped to current Activity or Fragment.
- `ViewModelProvider(requireActivity())` → shared between fragments in same activity.


----

### Code for Simple ViewModel creation 

```kotlin
class MyViewModel : ViewModel() {
    val counter = MutableLiveData<Int>(0)

    fun increment() {
        counter.value = (counter.value ?: 0) + 1
    }
}

```

```kotlin
class MyActivity : AppCompatActivity() {
    private lateinit var viewModel: MyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(MyViewModel::class.java)

        viewModel.counter.observe(this) { count ->
            // Update UI
        }

        // Even after rotation, this ViewModel will be the same instance
    }
}

```


------

### Manual ViewModel with Factory (for ViewModel with constructor params)

ViewModel with constructor

```kotlin
 class MyViewModel(private val repository: MyRepository) : ViewModel() {
    val data = MutableLiveData<String>()

    fun loadData() {
        data.value = repository.fetchData()
    }
}

```

Custom ViewModel Factory

```kotlin
class MyViewModelFactory(private val repository: MyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyViewModel::class.java)) {
            return MyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

```

Create in Activity

```kotlin
class MyActivity : AppCompatActivity() {

    private lateinit var myViewModel: MyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val repository = MyRepository()
        val factory = MyViewModelFactory(repository)

        myViewModel = ViewModelProvider(this, factory).get(MyViewModel::class.java)

        myViewModel.data.observe(this) {
            println("Data: $it")
        }

        myViewModel.loadData()
    }
}

```


### What is SavedStateHandle in ViewModel?
SavedStateHandle is a Jetpack class used inside a ViewModel to store and retrieve key-value pairs of data that can survive process death and configuration changes.

It acts like a bundle that is lifecycle-aware and works with ViewModel to help you retain small pieces of UI state (like form inputs, scroll position, tab selection, etc.).


### Why use SavedStateHandle?
ViewModel retains data only across configuration changes, but not across process death (e.g., when the OS kills the app due to memory).

With SavedStateHandle, your ViewModel can automatically restore state even after the app is killed and restarted.



````kotlin
class MyViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val KEY_COUNTER = "counter"
    }

    // LiveData tied to SavedStateHandle
    val counter: MutableLiveData<Int> =
        savedStateHandle.getLiveData(KEY_COUNTER, 0)

    fun increment() {
        val current = counter.value ?: 0
        savedStateHandle[KEY_COUNTER] = current + 1
    }
}

````

