# Singleton

**Singleton is creation design pattern. It ensures that you have only one instance.It is widely used design pattern.**


### Steps to create Singleton
1. Create private constructor for the class
2. Create static method for that will call the private constructor and save that in static instance. Whenever in feature we call static method it should return the previously cached(stored) object.


### Cons
1. Singleton has same pros and cons as global variables. It breaks modularity and Single Responsibility Principle.
2. Most of limitation comes with while creating the unit test

### UML

[PlantUML for Singleton pattern](https://www.plantuml.com/plantuml/uml/TOwnKe8n48JxFCMMEX8VWB6WfTONE9E3PBnScEHIAFBkvaS6i70cnMPdzzlRTADceuXxpReNjgfu-VOUrwzpGQZtct5q0E1nd5NBuOIiKsdWYtyDFZoUdIVisRQNpFHEpcarXl3EK7Ut_7fzHtfBkMC-mkyC2fTtRi-EDMBf_q_vEmTi5OGonBAtr9WYHz3X83bCSDKLRDagRAd6rSptL8T467qm0ZLUyFrJGsfLyzUvStV5PifHv0S0)

![Alt text](http://www.plantuml.com/plantuml/png/TOsnKiKW44LxlkAMEeeVyCRYjQdr1v1i4aPOCh0fDERVtIIEQF4OYkLozjnh5LTaIT6y11uZIMOyVrcBtwSnFhfQMx0QwB5OTLM2tn1O9_PqvWsykp-I1umatG-ZsMOCPo546WHvQFpbyiVqEeKAlGvd3HAbDofFXhZ7ld_RyhyEw5WjQOaIh4o5N4yzt3p7u5MS-L4HSUy4xmhjglh1VYgPm-VntnglEYqfVWC0 "a title")

----

### Kotlin code

// Kotlin idiomatic Singleton - object declaration
object Singleton {
    init {
        println("Singleton initialized")
    }
    
    fun doSomething() {
        println("Doing something")
    }
}

// Usage
fun main() {
    Singleton.doSomething()  // Access directly via object name
}

// Alternative: Thread-safe lazy initialization with double-check equivalent
class KotlinSingleton private constructor() {
    companion object {
        @Volatile
        private var instance: KotlinSingleton? = null
        
        fun getInstance(): KotlinSingleton {
            return instance ?: synchronized(this) {
                instance ?: KotlinSingleton().also { instance = it }
            }
        }
    }
}

### Java code

```
public final class Singleton {
    private static volatile Singleton instance;

    public static Singleton getInstance() {

        /**
         * This is DCL method more details at https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
         */
        Singleton result = instance;
        if (result != null) {
            return result;
        }

        synchronized (Singleton.class) {
            if (instance == null) {
                instance = new Singleton();
            }
            return instance;
        }
    }
}
```