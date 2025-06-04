# Fragment

## Fragment lifecycle methods:

| Method            | Called When…                                                        | Use Case                                                                   |
|-------------------|---------------------------------------------------------------------|----------------------------------------------------------------------------|
| `onAttach()`      | Fragment is associated with its host activity                       | Use this to get `Context` or communicate with the activity (via interface) | 
| `onCreate()`      | Fragment is being created (non-UI logic)                            | Initialize non-UI resources (e.g., ViewModels, arguments)                  | 
| `onCreateView()`  | UI layout is being created                                          | Inflate layout and initialize root views                                   |
| `onViewCreated()` | View is created, fully accessible                                   | Setup UI logic, bind views, observe LiveData, etc.                         | 
| `onStart()`       | Fragment is visible (UI is on screen)                               | Start animations, audio, or visible-related tasks                          |
| `onResume()`      | Fragment is fully interactive and in foreground                     | Start live actions (camera, sensors, gestures, listeners)                  | 
| `onPause()`       | Fragment is partially hidden or another fragment/activity is on top | Pause ongoing tasks, save UI state                                         | 
| `onStop()`        | Fragment is fully hidden                                            | Stop animations, unregister listeners                                      | 
| `onDestroyView()` | Fragment’s view hierarchy is removed                                | Clean up views, avoid memory leaks (set binding/view = null)               | 
| `onDestroy()`     | Fragment instance is about to be destroyed                          | Clean up remaining resources                                               | 
| `onDetach()`      | Fragment is disassociated from Activity                             | Final cleanup, reference to Activity should be cleared                     | 


## Activity And Fragment lifecycle calls as per launch

**When you run the app**
 
- MainActivity: onCreate
- SampleFragment: onAttach
- SampleFragment: onCreate
- SampleFragment: onCreateView
- SampleFragment: onViewCreated
- MainActivity: onStart
- SampleFragment: onStart
- MainActivity: onResume
- SampleFragment: onResume

**And during exit:**

- MainActivity: onPause
- SampleFragment: onPause
- MainActivity: onStop
- SampleFragment: onStop
- SampleFragment: onDestroyView
- SampleFragment: onDestroy
- SampleFragment: onDetach
- MainActivity: onDestroy
