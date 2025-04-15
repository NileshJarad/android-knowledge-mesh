# Launch mode

In Android, Launch Modes define how a new instance of an Activity is associated with the current task and back stack when it's started. Understanding launch modes is crucial for managing how your app navigates between activities and handles the back stack.


### When an activity is started, Android can either:

- Create a new instance of the activity (even if it already exists),
- Or bring an existing instance to the front (depending on launch mode).

This affects how the activity stack behaves.

| Launch Mode    | Description                                                                                 |
|----------------|---------------------------------------------------------------------------------------------|
| standard       | Default mode. New instance is always created.                                               |
| singleTop      | If the activity is already on top, it is reused instead of creating a new one.              |
| singleTask     | A single instance in the entire task. If exists, all other activities above it are cleared. |
| singleInstance | Similar to singleTask but the activity lives in its own separate task.                      |


### You can define launch modes in two ways:

- Manifest file using android:launchMode 
- Intent flags at runtime using Intent.FLAG_ACTIVITY_*


## Real-World Example Using 4 Activities: A → B → C → D

### 1.  `standard` (default)
```text
Start A → Start B → Start C → Start D
Stack: A → B → C → D
```

### 2.  `singleTop` 
If an activity is launched and it is already on top, no new instance is created; otherwise, new one is added.
```text
Start A → Start B → Start C → Start D
Stack: A → B → C → D

Now again start D
Stack: A → B → C → D (new instance not added, but onNewIntent() is called)

Now start C again
Stack: A → B → C → D → C (new instance added, since C was not on top)
```

Only works if you're launching the same activity already at the top.


### 3.  `singleTask`
Only one instance is allowed in the task. If it already exists in the stack, it is brought to front, and all activities on top of it are cleared
```text
Start A → B → C → D
Stack: A → B → C → D

Now start B again (singleTask)
Result: B is brought to top with calling onNewIntent(), C and D are removed from stack 
Stack: A → B

```
This is useful when you want only one instance and clear all above it.


### 4.  `singleInstance`
Same as singleTask, but this activity lives in a completely separate task.

```text
Start A → B → C 
Now start D (singleInstance)

Result:
Task 1: A → B → C  
Task 2: D

Now if you press back in D → You’ll go to last opened activity in Android stack if it's empty then it will got Home screen

```

Used for activities like login screens, video players, or call UIs that should live outside the normal flow.

```xml
<activity
    android:name=".ActivityE"
    android:launchMode="singleInstance"
    android:taskAffinity="com.example.e_task"
    android:excludeFromRecents="false">
</activity>
```

 - **singleInstance**: puts it in its own task 
 - **taskAffinity**: creates true separation in Recents 
 - **excludeFromRecents**=false: so it appears in Recents



## Questions

1: You have Activity A → B → C. Now C launches B again, and B has launchMode="singleTask". What will the stack look like?

>B already exists in the stack.
>
>Since launchMode="singleTask", the existing B will be brought to the top, and C will be removed.
>
>️ Final Stack: A → B

2: If an activity has launchMode="singleTop" and it launches itself, will a new instance be created?
>
>No, if it's already on top, onNewIntent() will be called instead.
>
>Yes, if it's not on top (then it will create a new instance).

3: Which launch mode is best when you want to avoid creating multiple instances of the same activity in Recent Apps (Overview Screen)?

>singleInstance — since it uses a separate task, only one recent entry exists.

4: Can singleTop activity have multiple instances in the back stack?


>Yes, if it's not on top during each launch, it will create multiple instances.
>
>Example:
>Stack: A → B → B → B
> 
>If B is not on top when it's started, new instance is created even if it's singleTop.

5: What's the difference between using launchMode="singleTop" and setting Intent.FLAG_ACTIVITY_SINGLE_TOP?

>No difference in behavior, both ensure that if the activity is already on top, it is reused.
>
>Difference is where it's declared:
>
>launchMode → in manifest, permanent
>
>FLAG → in code, dynamic/per-launch

6: If an activity has launchMode="singleTask" and you start it from a different task, what happens?


>Android will bring the existing instance to the front (if any).
>
>If not found, a new instance is created in the task that launches it.
>
>So, it does not force a new task, unlike singleInstance.

7: What callback method is called when an already-running singleTop or singleTask activity is brought to the front?


>onNewIntent(intent: Intent?) is called.
>
>onCreate() is not called again.

8: What's the effect of Intent.FLAG_ACTIVITY_CLEAR_TOP?


>If the activity already exists in the stack, all activities above it are cleared.
>
>If used with FLAG_ACTIVITY_SINGLE_TOP, it reuses the existing activity and calls onNewIntent().
>
>Example:
>
>Stack: A → B → C
>Start A with FLAG_ACTIVITY_CLEAR_TOP
 Final Stack: A (B and C cleared)

9: Can singleInstance activity share task with other activities?

>No, singleInstance lives in its own task and cannot share it with others.
>
>Even if you start another activity from it, that activity opens in a new task.


