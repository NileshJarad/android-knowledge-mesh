# BroadcastReceiver

A BroadcastReceiver is a component that lets your app listen to and respond to system-wide events or app-specific broadcasts.

Think of it like a radio receiver—your app “listens” for specific intents (actions), and when it hears one, it responds accordingly.


## Types of Broadcasts
1. **System Broadcasts**
   - Sent by Android system. 
   - Examples:
        ```kotlin
        Intent.ACTION_BOOT_COMPLETED
        
        Intent.ACTION_BATTERY_LOW
        
        ConnectivityManager.CONNECTIVITY_ACTION
        ```
     
2. **Custom Broadcasts**
   - Sent by your app. 
   - Useful for communication within your app or between different apps.


## Types of BroadcastReceivers

1. **Manifest-declared (Static) Receiver**
   - Declared in AndroidManifest.xml 
   - Works even when the app is not running. 
   - Cannot receive some broadcasts in Android 8.0+ (background limitations).
   - **Receiver class**
   ```kotlin
     
        class BootReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    Log.d("BootReceiver", "Device Booted")
                }
            }
        }
      ```
   - AndroidManifest.xml
   ```xml
      <receiver android:name=".BootReceiver"
                android:enabled="true"
                android:exported="true">
          <intent-filter>
              <action android:name="android.intent.action.BOOT_COMPLETED"/>
          </intent-filter>
      </receiver>
        
      <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    ``` 
2. **Context-registered (Dynamic) Receiver**
   - Registered using registerReceiver() in code.
   - Active only while app/component is running. 
   - More flexible and efficient.
   - **Dynamic Broadcast Receiver**

    ```kotlin
    class MainActivity : AppCompatActivity() {
        private val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("BatteryReceiver", "Battery low!")
            }
        }
    
        override fun onResume() {
            super.onResume()
            val filter = IntentFilter(Intent.ACTION_BATTERY_LOW)
            registerReceiver(batteryReceiver, filter)
        }
    
        override fun onPause() {
            super.onPause()
            unregisterReceiver(batteryReceiver)
        }
    }
    ```

## When to Use
- When your app needs to respond to:
  - System events (connectivity, charging, boot completed)
  - Custom internal broadcasts 
- Decouple components using your own custom broadcasts 
- Use in MVVM architecture to notify ViewModel/UI of background events


## Best Practices
- Use dynamic receivers when listening only during specific app states (active UI). 
- Use manifest receivers only when required to receive at boot or for system-level events. 
- Don’t do heavy work in onReceive() — offload to services or workers. 
- In Android 8.0+, static receivers are limited unless for certain implicit actions (like BOOT_COMPLETED).



## Questions

**Q 1**:
Can you receive a broadcast in Android 8.0+ using a static receiver for CONNECTIVITY_ACTION? Why or why not?

**A** : No, starting with Android 8.0 (API level 26), manifest-declared receivers can't receive most implicit broadcasts like CONNECTIVITY_ACTION, SMS_RECEIVED, etc., unless your app is in the foreground.
This was done to improve performance and battery life.

Instead, register a dynamic receiver using registerReceiver() while the app is active.

---

**Q 2**:
What happens if you register a BroadcastReceiver dynamically but forget to unregister it?

**A** : If you don’t unregister a dynamically registered receiver (e.g., in onPause() or onStop()), it can cause:

- Memory leaks (since the context holds reference to the receiver)
- Unexpected behavior if it receives broadcasts while the Activity is destroyed 
- Crashes, especially during onReceive(), if the context is no longer valid

---
**Q 3**:
Can a BroadcastReceiver start a Service or Activity?

**A** : Yes, a `BroadcastReceiver` can start a Service or Activity using `startService()` or` startActivity()` inside `onReceive()`.

However, `onReceive()` runs on the main thread and must finish quickly.
If starting a long-running operation, it’s better to use:
- `startForegroundService()` for long background work
- `WorkManager` for deferred/background tasks

--- 
**Q 4**:
Can a BroadcastReceiver run when the app is killed?

**A** : Yes, but only if it's a static (manifest-declared) receiver and the broadcast is one of the allowed implicit broadcasts (e.g., BOOT_COMPLETED).

However, in Android 8.0+, most static receivers will not work unless the app has been started once or the device is rebooted with a valid permission like RECEIVE_BOOT_COMPLETED.

--- 

**Q 5**:
If two apps listen to the same broadcast, in what order are they notified?

**A** : **The broadcast is delivered based on priority:**

- Ordered Broadcasts (sendOrderedBroadcast()): Receivers are called in order of their priority (declared via intent filters).

- Normal Broadcasts (sendBroadcast()): All receivers get the broadcast, but order is not guaranteed.

**With ordered broadcasts, receivers can:**

- Modify the broadcast data 
- Stop the broadcast using abortBroadcast() (deprecated in newer Android versions)

--- 

**Q 6**:
What is the difference between sendBroadcast() and sendOrderedBroadcast()?

**A** :

| Method                 | -Behavior                                                                                               |
|------------------------|---------------------------------------------------------------------------------------------------------|
| sendBroadcast()        | 	All receivers receive it asynchronously and unordered                                                  |
| sendOrderedBroadcast() | 	Receivers are triggered one after another, based on priority.Can modify data or abort further delivery |


------

**Q 7**:
Can BroadcastReceiver receive a broadcast while the app is in Doze Mode?

**A** : In Doze mode, Android restricts background tasks, and most broadcasts (especially implicit ones) are deferred.

However, some high-priority system broadcasts (like `SMS_RECEIVED`, `PHONE_STATE`) can still be received.

To work reliably in Doze:

Use JobScheduler or WorkManager

Use `setAndAllowWhileIdle()` or `setExactAndAllowWhileIdle()` for alarms