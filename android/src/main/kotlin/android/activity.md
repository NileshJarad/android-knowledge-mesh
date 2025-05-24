# Activity

An android provides the window in which the app draws its UI. This window typically fills the screen but may be smaller than the screen and float on top of other windows. Generally, one android implements one screen in an app

## Activity lifecycle methods:

1. **onCreate()**
   - **_Called When_** - The android is first created (only once per instance).
   - **_Purpose_** - Initialize the android. Setup Views, restore states

2. **onStart()**
   - **_Called When_**: After onCreate() or when coming back from background.
   - **_Purpose_**: Make the android visible to the user, but not interactive yet.

3. **onResume()**
   - **_Called When_**: After onStart() or when returning from a paused state.
   - **_Purpose_**: The android is now in the foreground and the user can interact with it.
4. **onPause()**

   - **_Called When_**: Activity is partially obscured, like:
     - A new android is started 
     - A dialog appears
   - **_Purpose_**:
     - Pause animations, music, or video 
     - Save unsaved data (if lightweight)

5. **onStop()**
   - **_Called When_**: Activity is no longer visible (completely hidden).

   - **_Purpose_**:
     - Release resources that are not needed when off-screen
     - Stop heavy processes

6. **onRestart()**

   - **Called When**: The user navigates back to the android from the stopped state (e.g., back button).
   - **Purpose**: Prepare the android to go back into foreground.

7. **onDestroy()**

   - **_Called When_**:
     - The android is finishing (user presses back or calls finish())
     - The system destroys the android (e.g., configuration change)

   - **_Purpose_**: Cleanup all resources to avoid memory leaks


## Starting a New Activity and Coming back

**Launch  MainActivity**
- MainActivity: onCreate()
- MainActivity: onStart()
- MainActivity: onResume()

**Launch Flow (from MainActivity to SecondActivity):**
- MainActivity: onPause()
- SecondActivity: onCreate()
- SecondActivity: onStart()
- SecondActivity: onResume()
- MainActivity: onStop()

**Press Back (from SecondActivity to MainActivity):**

- SecondActivity: onPause()
- MainActivity: onRestart()
- MainActivity: onStart()
- MainActivity: onResume()
- SecondActivity: onStop()
- SecondActivity: onDestroy()


## Starting a New Activity and user press home button 

- MainActivity: onCreate()
- MainActivity: onStart()
- MainActivity: onResume()
- MainActivity: onPause()
- MainActivity: onStop() - _The android is not destroyed. It just goes to the background (stopped state), and stays in memory (unless Android kills it due to low memory)._



## Scenario: System Dialog appears on top of your android

- Incoming call screen
- Battery saver alert 
- Permission dialog 
- Airplane mode warning 
- Notification drawer being pulled (only partial impact)

### Depends on whether the dialog is:
- Partially obstructing (e.g., floating permission dialog)
- Fully covering (e.g., system alert that takes full focus)

### Android Activity Lifecycle – System Dialog Summary
| 🧪 System Dialog Type       | 📱 Activity Visibility           | 🔁 Lifecycle Methods Called            | 🧷 Final Activity State |
|-----------------------------|----------------------------------|----------------------------------------|-------------------------|
| Permission Dialog (partial) | Partially visible                | `onPause()`                            | Paused                  |
| Incoming Call Screen        | Not visible (fully covered)      | `onPause()`, `onStop()`                | Stopped                 |
| Battery/Low Power Alert     | Depends (partial/full)           | `onPause()` or `onPause()`, `onStop()` | Paused / Stopped        |
| Notification Drawer Pull    | Still visible (no loss of focus) | *(No lifecycle method called)*         | Resumed                 |
| Airplane Mode Toggle Dialog | Partially visible                | `onPause()`                            | Paused                  |

### When Dialog is Dismissed

| 💬 Previous State | 🚀 Lifecycle Methods Called on Return    |
|-------------------|------------------------------------------|
| Paused            | `onResume()`                             |
| Stopped           | `onRestart()`, `onStart()`, `onResume()` |


---

## Start Activity for Result

**1. Define the launcher in your activity or fragment:**
```kotlin
private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val data: Intent? = result.data
        val resultValue = data?.getStringExtra("key") // Or whatever data you're expecting
        Toast.makeText(this, "Result: $resultValue", Toast.LENGTH_SHORT).show()
    }
}
```

**2. Launch another activity and expect a result:**
```kotlin
val intent = Intent(this, SecondActivity::class.java)
launcher.launch(intent)

```
**3. Set result in SecondActivity**

```kotlin
val resultIntent = Intent()
resultIntent.putExtra("key", "Some Data")
setResult(Activity.RESULT_OK, resultIntent)
finish()

```
### [List of ActivityResultContracts](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts)

**Note:**  
- Each launcher must have a unique name (you cannot define private val activityLauncher multiple times with the same name)
- Each launcher should be defined only once (in onCreate, or class body, not dynamically inside methods)

## Android Activity Priority Levels

When the Android system is low on memory and needs to reclaim resources, it decides which activity or component to kill based on priority and recency, not just the runtime duration (short-run vs long-run). Let's break it down clearly.

Android assigns importance levels (often referred to as "priority") to each process. The system kills the lowest priority process first when memory is needed.

**From highest to lowest priority:**

1. Foreground Activity – currently visible and interacting with the user.

2. Visible Activity – visible but not in the foreground (e.g., dialog partially over it).

3. Service Process – running a foreground or background service.

4. Background Activity – not visible, in back stack.

5. Empty Process – not holding any active component, just cached for faster reload.



### Important Concepts
- Both activities having same priority usually means they are in the background state.
- When multiple background activities are candidates for killing:

  - The system considers memory consumption, process age, and LRU (Least Recently Used) order.

  - Long-running background activity may have higher memory usage.

  - If both are equally recent in the LRU list, the one consuming more memory is more likely to be killed.

  - If all else is equal, the oldest one (least recently used) will be killed first.

----


## Questions
1. We have two launcher activities defined in our manifest file. While running the application, What will be the outcome?
>If you have two launcher Activities in your manifest When running the application, It will install two instances of the application(both instances behave the same). If you uninstall any one instance of the app, both instances will be uninstalled.


2. What happens to the android when the device is rotated?
> The android is destroyed and recreated by default. This happens because rotation triggers a configuration change.
> 
> onPause() → onStop() → onDestroy()
> 
> onCreate() → onStart() → onResume()
3. What's the difference between finish() and pressing the back button?
>  Both will:
> Call onPause() → onStop() → onDestroy() on the current android.
> 
> But:
> 
> finish() is programmatic — you call it explicitly in code.
>
>Back button is user-driven, and may be intercepted via onBackPressedDispatcher.

4. Can an android be in the onPause state but still be visible?
> Yes.
>
> Example: A dialog or transparent android appears on top.
>
> The underlying android is paused, but still partially visible.
5. What if you call finish() inside onCreate()?
> The android is created, then immediately destroyed.
>
> Lifecycle calls:
> onCreate() → onDestroy()
> 
> onStart() and onResume() are not called.

6. What happens if two activities have the same intent filter with MAIN and LAUNCHER?
> When you launch the app, the system will ask you which one to open, showing a chooser dialog.
>
> If one is marked as “Always”, that becomes the default launcher.
7. What if the system kills the android in the background? Will onDestroy() be called?
> ❌ No.
>
>If the system kills your android (e.g., low memory), onDestroy() is NOT guaranteed to be called.
>
>You must save essential data in onSaveInstanceState().
8. Is onStop() always called before onDestroy()?
>  ✅ Yes, in most cases.
>
>However:
>
>If the system is under heavy load, it might skip onStop(), especially in low memory scenarios. But this is rare.

        