# Observer Pattern

- **Is a Behavioral design pattern that allows objects to be notified of changes to the state of another object, known as the subject. In Kotlin, the Observer pattern can be implemented using a combination of interfaces and classes.**

### Type of observer 
1. **Push model:**
<br>In the push model, the subject sends the update directly to the observers, without requiring the observer to request it. This means that the subject "pushes" the update to the observers, hence the name.

2. **Pull model:**
<br>In the pull model, the observers request the update from the subject when they need it. This means that the observer "pulls" the update from the subject, hence the name.


### Steps to create observer method

1. **Observer** : Defines an updating interface for objects that should be notified ofchanges
   in a subject.
2. **Concrete Observer** : implements the Observer updating interface to keep its state consistent
   with the subject's
3. **Subject** : knows its observers. An y number o fObserver objects ma y observe a subject.



### UML

[PlantUML for Strategy Pattern](http://www.plantuml.com/plantuml/uml/XT31Ii0m383XUvuYn-smFi0ePU1LyE0JT5scbQs5D1j8tRlhS9c81s_bxnTeCnJnBDkwmDDOY7TYj1_6u4DEWKb8Apnluni5mOxZowjq3lMcntoT2W24nPU25ww07UwUVqIFZx68rLa7WFEMq4-JfaPjZvhQRyIqvft-TobBdDyrbgP5E01Y4kBP1xgvjNTphWAUm_hIfeL3V_Dd5nL-qsgCEKu_K9g-Lla9)

![Alt text](http://www.plantuml.com/plantuml/png/XT31Ii0m383XUvuYn-smFi0ePU1LyE0JT5scbQs5D1j8tRixSPc81s_bxnTeCnJnBDkwmDDOY7TYj1_6u4DEWKb8Apnluni5mOxZowjq3lMcntoT2W24nPU25ww07UwUVqIFZx68rLa7WFEMq4-JfaPjZvhQRyIqvft-TobBdDyrbgP5E01Y4kBP1xgvjNTphW9-3UjBcnOE_SsVNLJuJQinvZZzG6awD_a9)


---


### kotlin code


Push base code

```agsl
interface Observer {
    fun update(value: Int)
}

class Subject {
    private val observers = mutableListOf<Observer>()

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    fun setValue(value: Int) {
        notifyObservers(value)
    }

    private fun notifyObservers(value: Int) {
        observers.forEach { it.update(value) }
    }
}

class ConcreteObserver : Observer {
    override fun update(value: Int) {
        println("Observer updated with value $value")
    }
}


```
Pull base code

```agsl
interface Observer {
    fun update()
}

class Subject {
    private val observers = mutableListOf<Observer>()
    private var value: Int = 0

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    fun setValue(value: Int) {
        this.value = value
    }

    fun getValue(): Int {
        return value
    }

    fun notifyObservers() {
        observers.forEach { it.update() }
    }
}

class ConcreteObserver(private val subject: Subject) : Observer {
    private var value: Int = 0

    init {
        value = subject.getValue()
    }

    override fun update() {
        value = subject.getValue()
        println("Observer updated with value $value")
    }
}

```


---

### Java code

Subject interface

```java
import java.util.ArrayList;
import java.util.List;

public interface Observer {
    void update(int value);
}

public interface Subject {
    void register(Observer observer);
    void remove(Observer observer);
    void notifyObservers();
}
```

Concrete Subject

```java
public class WeatherStation implements Subject {
    private final List<Observer> observers = new ArrayList<>();
    private int temperature;

    @Override
    public void register(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void remove(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(temperature);
        }
    }

    public void setMeasurements(int temperature) {
        this.temperature = temperature;
        System.out.println("WeatherStation: Current temperature is " + temperature);
        notifyObservers();
    }
}
```

Concrete Observers

```java
public class CurrentConditionsDisplay implements Observer {
    private int temperature;

    @Override
    public void update(int temperature) {
        this.temperature = temperature;
        display();
    }

    public void display() {
        System.out.println("CurrentConditionsDisplay: " + temperature + "F degrees");
    }
}

public class StatisticsDisplay implements Observer {
    private int temperature;
    private int minTemperature = Integer.MAX_VALUE;
    private int maxTemperature = Integer.MIN_VALUE;
    private int count = 0;

    @Override
    public void update(int temperature) {
        this.temperature = temperature;
        count++;
        updateMinMax(temperature);
        display();
    }

    private void updateMinMax(int temperature) {
        if (temperature < minTemperature) minTemperature = temperature;
        if (temperature > maxTemperature) maxTemperature = temperature;
    }

    public void display() {
        System.out.println("StatisticsDisplay: " + temperature
                + "F (min: " + minTemperature + ", max: " + maxTemperature + ")");
    }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        WeatherStation station = new WeatherStation();

        Observer currentDisplay = new CurrentConditionsDisplay();
        Observer statisticsDisplay = new StatisticsDisplay();

        station.register(currentDisplay);
        station.register(statisticsDisplay);

        station.setMeasurements(80);
        station.setMeasurements(82);
        station.setMeasurements(78);
    }
}
```
