# Intent and Intent filter


## What is an Intent?

An Intent is a messaging object used to request an action from another app component. It is used to start activities, services, or deliver a broadcast.

**Basic Uses of Intent:**
- Start an activity 
- Start a service 
- Deliver a broadcast

### Types of Intent
- **Explicit Intent**
  - Used to start a specific component (Activity/Service) within the same application. 
  - Use Case: Navigating from one activity to another
  
```kotlin
val intent = Intent(this, SecondActivity::class.java)
intent.putExtra("username", "JohnDoe")
startActivity(intent)
```

- **Implicit Intent**
  - Used when you want any app that can perform a specific action (e.g., send an email, view a webpage) to handle the intent. 
  - Use Case: Open a URL in the browser or send an email.
```kotlin
val intent = Intent(Intent.ACTION_VIEW)
intent.data = Uri.parse("https://www.google.com")
startActivity(intent)
```

###  Intent Filters

Intent Filters are declared in the AndroidManifest.xml and tell the system what types of intents a component can handle.

**Structure of an Intent Filter:**

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:scheme="http" />
</intent-filter>
```

**Intent Components Breakdown**

| Component | Purpose                                                              |
|-----------|----------------------------------------------------------------------|
| Action    | The general action to perform (e.g., `VIEW`, `SEND`, `MAIN`)         |
| Data      | URI data the intent is acting on                                     |
| Category  | Gives additional info about the action (e.g., `DEFAULT`, `LAUNCHER`) |




### More example

**Open Dialer (Implicit)**
```kotlin
val phone = "tel:1234567890"
val intent = Intent(Intent.ACTION_DIAL).apply {
    data = Uri.parse(phone)
}
startActivity(intent)

```

**Send Email (Implicit)**

```kotlin
val intent = Intent(Intent.ACTION_SEND).apply {
    type = "message/rfc822"
    putExtra(Intent.EXTRA_EMAIL, arrayOf("example@email.com"))
    putExtra(Intent.EXTRA_SUBJECT, "Subject Here")
    putExtra(Intent.EXTRA_TEXT, "Body Here")
}
startActivity(Intent.createChooser(intent, "Send Email"))

```


### Intent Actions

| Action                             | Description                                                                                        |
|------------------------------------|----------------------------------------------------------------------------------------------------|
| `Intent.ACTION_MAIN`               | Entry point of the app. Used to launch the main activity. Usually paired with `CATEGORY_LAUNCHER`. |
| `Intent.ACTION_VIEW`               | Display data to the user (e.g., open a URL, image, or contact).                                    |
| `Intent.ACTION_SEND`               | Send data to another app (e.g., share text or files).                                              |
| `Intent.ACTION_SENDTO`             | Send data to a specific recipient (e.g., email or SMS app).                                        |
| `Intent.ACTION_DIAL`               | Open the phone dialer with a number filled in, without making a call.                              |
| `Intent.ACTION_CALL`               | Directly make a phone call (requires permission).                                                  |
| `Intent.ACTION_EDIT`               | Edit the given data (e.g., open a contact for editing).                                            |
| `Intent.ACTION_INSERT`             | Insert new data (e.g., add a new contact or event).                                                |
| `Intent.ACTION_DELETE`             | Delete the given data.                                                                             |
| `Intent.ACTION_PICK`               | Allow the user to pick an item from data (e.g., image or contact picker).                          |
| `Intent.ACTION_GET_CONTENT`        | Allow user to select a piece of content (e.g., image, video).                                      |
| `Intent.ACTION_CHOOSER`            | Display a chooser dialog to let user select an app to handle the intent.                           |
| `Intent.ACTION_BOOT_COMPLETED`     | Broadcast sent after the system finishes booting.                                                  |
| `Intent.ACTION_TIME_TICK`          | Broadcast every minute (cannot be registered in manifest).                                         |
| `Intent.ACTION_POWER_CONNECTED`    | Broadcast when device is connected to power.                                                       |
| `Intent.ACTION_POWER_DISCONNECTED` | Broadcast when device is disconnected from power.                                                  |


### Intent Categories

| Category                               | Description                                                                             |
|----------------------------------------|-----------------------------------------------------------------------------------------|
| `Intent.CATEGORY_DEFAULT`              | Must be included for any activity that responds to an implicit intent.                  |
| `Intent.CATEGORY_LAUNCHER`             | Indicates that this activity should be displayed in the app launcher as an entry point. |
| `Intent.CATEGORY_BROWSABLE`            | Allows the activity to be started from a web browser (e.g., via an HTTP link).          |
| `Intent.CATEGORY_ALTERNATIVE`          | Provides an alternative action the user can perform on data.                            |
| `Intent.CATEGORY_SELECTED_ALTERNATIVE` | Indicates the user has selected an alternative action.                                  |
| `Intent.CATEGORY_HOME`                 | Main home screen activity (like launchers).                                             |
| `Intent.CATEGORY_APP_EMAIL`            | Designates app as an email client.                                                      |
| `Intent.CATEGORY_APP_MESSAGING`        | Designates app as a messaging client.                                                   |
| `Intent.CATEGORY_APP_CONTACTS`         | Designates app as a contacts app.                                                       |
| `Intent.CATEGORY_APP_BROWSER`          | Designates app as a web browser.                                                        |
