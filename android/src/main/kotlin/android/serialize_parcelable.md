# Serialize / Parcelable


| Feature       | `Serializable`                                   | `Parcelable`                                                  |
|---------------|--------------------------------------------------|---------------------------------------------------------------|
| Purpose       | Java standard interface for object serialization | Android-specific interface for high-performance serialization |
| Package       | `java.io.Serializable`                           | `android.os.Parcelable`                                       |
| Speed         | **Slower** – uses Java Reflection                | **Faster** – optimized for Android                            |
| Use Case      | Small objects, or not performance-critical       | Recommended for Android app IPC and Bundles                   |
| Code Required | Minimal – Just `implements Serializable`         | Verbose – need to implement multiple methods                  |
| Boilerplate   | Almost none                                      | A lot, though Kotlin `Parcelize` helps                        |
| Efficiency    | Creates temporary objects, uses reflection       | Manual serialization; more memory-efficient                   |




---- 

**Use Parcelable when:**

- You are passing objects through Intents, Bundles, or IPC in Android. 
- You care about performance. 
- You are working on Android components (Activities, Fragments, Services).

**Use Serializable when:**

- You're dealing with Java code, or using file/network I/O. 
- You want quick prototyping or need Java compatibility. 
- Performance isn't critical.




-----------


## Serializable implementation


```kotlin
import java.io.Serializable

data class User(
    val name: String,
    val age: Int
) : Serializable

```

--------

## Parcelable implementation


| Method / Member                         | Purpose                                                                                             |
|-----------------------------------------|-----------------------------------------------------------------------------------------------------|
| `writeToParcel(Parcel dest, int flags)` | Used to serialize the object to a `Parcel`. You write each property manually here.                  |
| `describeContents()`                    | Describes special objects in the `Parcelable` (e.g., file descriptors). Most of the time returns 0. |
| `CREATOR` object                        | Companion object that generates instances of your `Parcelable` class from a `Parcel`.               |


### Manual Parcelable – Traditional Way (Verbose)

```kotlin
data class User(val name: String, val age: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(age)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}

```

*
*How to Keep It Safe**
- Always match write and read order 
- Document the order if it's a large class 
- Use @Parcelize – it handles this automatically and safely
- In manual implementation, write unit tests for Parceling



### @Parcelize – Kotlin Way (Recommended)

```kotlin
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val name: String,
    val age: Int
) : Parcelable

```

#### Plugin

```groovy
plugins {
    id 'kotlin-parcelize'
}

```


--------


