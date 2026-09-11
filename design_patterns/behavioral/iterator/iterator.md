# Iterator Pattern

- ** behavioral design pattern that lets you traverse elements of a collection without exposing its underlying
  representation**

### Steps to create observer method

1. **Iterator interface** : Declares operation required to traversing collection
2. **Concrete Iterator** : Implementation of specific algorithm for traversing collection.
3. **Iterable Interface** : Declares the methods for getting iterators compatible with collection. Return types of the
   method must be `Iterator` interface
4. **Concrete Collection** : Implements method to return iterator instance when client requests

### UML

[PlantUML for Strategy Pattern](https://www.plantuml.com/plantuml/uml/XT31Ii0m383XUvuYn-smFi0ePU1LyE0JT5scbQs5D1j8tRlhS9c81s_bxnTeCnJnBDkwmDDOY7TYj1_6u4DEWKb8Apnluni5mOxZowjq3lMcntoT2W24nPU25ww07UwUVqIFZx68rLa7WFEMq4-JfaPjZvhQRyIqvft-TobBdDyrbgP5E01Y4kBP1xgvjNTphWAUm_hIfeL3V_Dd5nL-qsgCEKu_K9g-Lla9)

![Alt text](https://www.plantuml.com/plantuml/png/TOuzoiCm38PtdKBZzmtn0K88WKvPklK0RghyWEqKMu62wxit868QJ4UzUjwVBh3sl5a3FPFNl7fitbM5bzKDdfYoMcDlduOiEGx2tWqLhAW5AxdABzFC2h-uMw000B0u9Z_gWVApQRBMjX4GnytXH7p4_Vr30XYC3g4O4A6aQyn3eb8gGIfhz-UlsYkzUA-KUzP1RaXGRf2W26Uhb9xjnv2Et6EpvWC0)


---

### kotlin code

Iterator interface

```kotlin
interface Iterator<E> {
    fun hasNext() : Boolean
    fun next() : E
}
```

Concrete Iterator

```kotlin
private class ListIterator<T>(private val items: List<T>) : Iterator<T> {
    private var position = 0

    override fun hasNext() : Boolean {
        return position < items.size
    }

    override fun next() : T {
        val item = items[position]
        position++
        return item
    }
}
```

Iterable interface

```kotlin
interface Iterable<E> {
    fun iterator() : Iterator<E>
    fun add(item : E)
}
```

Concrete Iterable

```kotlin
private class OwnList<E> : Iterable<E> {
    val items = mutableListOf<E>()

    override fun hasNext() : Boolean {
        return items.isNotEmpty()
    }

    override fun iterator() : Iterator<E> {
        return ListIterator(items)
    }

    override fun add(item : E) {
        items.add(item)
    }
}
```

Client code

```kotlin
fun main() {
    val ownList = OwnList<String>()
    ownList.add("A")
    ownList.add("B")
    ownList.add("C")

    val iterator = ownList.iterator()
    while (iterator.hasNext()) {
        println(iterator.next())
    }
}
```
