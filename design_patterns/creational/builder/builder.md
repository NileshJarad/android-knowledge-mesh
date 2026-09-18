# Builder

1. **Builder is creational design pattern**
2. **It allows to create complex object step by step**
3. **This pattern allows to create the different object from same construction code**


### Steps to create builder pattern

1. Declare interface(it can be abstract class or interface) as **Builder**
2. Make concrete builder follows the Builder
3. Define product 
4. Client



### Pros
1. You can create object step by step
2. You can reuse the same code construction code for various representation of product



### Cons
1. Overall complexity increases as it needs extra classes




### UML

[PlantUML for Factory Pattern](http://www.plantuml.com/plantuml/uml/TP312eCm44Jl-nLxr4C2hJSHaVe3fNyGuhOLqWZPpKdzzwOqpK7ePPYyC3Em0ui94byEBd5s4mNiDgrnNBmD99GXm3KiausIVfN2J5jy6aO3CBgPlA1IMtzCjXYP663sGk5kBFt2NLTGtw_WJrKD_loH9ic3v4OSdLHrYtaRTW2mpZ0Naf-7pM_RghNUKsLnJNR_oWVoKph46m00)

![Alt text](http://www.plantuml.com/plantuml/png/TP312eCm44Jl-nLxr4C2hJSHaVe3fNyGuhOLqWZPpKdzzwOqpK7ePPYyC3Em0ui94byEBd5s4mNiDgrnNBmD99GXm3KiausIVfN2J5jy6aO3CBgPlA1IMtzCjXYP663sGk5kBFt2NLTGtw_WJrKD_loH9ic3v4OSdLHrYtaRTW2mpZ0Naf-7pM_RghNUKsLnJNR_oWVoKph46m00)









### kotlin code

Builder interface
```agsl
interface HouseBuilder {
    fun buildWalls(numWalls: Int)
    fun buildDoor(numDoors: Int)
    fun buildWindows(numWindows: Int)
    fun buildGarden()
    fun buildGarage()
    fun buildSteps(numSteps: Int)
    fun getResult(): House
}
```


Concrete builder

```agsl
class MyHouseBuilder : HouseBuilder {

    private var walls = 0
    private var doors = 0
    private var windows = 0
    private var garden = false
    private var garage = false
    private var steps = 0
    override fun buildWalls(numWalls: Int) {
        this.walls = numWalls
    }

    override fun buildDoor(numDoors: Int) {
        this.doors = numDoors
    }

    override fun buildWindows(numWindows: Int) {
        this.windows = numWindows
    }

    override fun buildGarden() {
        this.garden = true
    }

    override fun buildGarage() {
        this.garage = true
    }

    override fun buildSteps(numSteps: Int) {
        this.steps = numSteps
    }

    override fun getResult(): House {
        return House(walls, doors, windows, garden, garage, steps)
    }


}
```

Product 

```agsl
data class House(
    private val walls: Int = 0,
    private val doors: Int = 0,
    private val windows: Int = 0,
    private val garden: Boolean = false,
    private val garage: Boolean = false,
    private val steps: Int = 0
)
```

client

```agsl
    val gardenHouseBuilder = MyHouseBuilder().apply {
        this.buildDoor(2)
        this.buildGarden()
        this.buildWalls(4)
        this.buildSteps(9)
        this.buildWindows(4)
    }


    val gardenHouse = gardenHouseBuilder.getResult()
    println("Garden House  = $gardenHouse")

    val gardenWithGarageHouseBuilder = MyHouseBuilder().apply {
        this.buildDoor(2)
        this.buildGarden()
        this.buildGarage()
        this.buildWalls(4)
        this.buildSteps(25)
        this.buildWindows(4)
    }


    val gardenWithGardenHouse = gardenWithGarageHouseBuilder.getResult()
    println("Garden with garage House  = $gardenWithGardenHouse")
```
---

### Java code

Builder interface

```java
public interface HouseBuilder {
    void buildWalls(int numWalls);
    void buildDoor(int numDoors);
    void buildWindows(int numWindows);
    void buildGarden();
    void buildGarage();
    void buildSteps(int numSteps);
    House getResult();
}
```

Concrete Builder

```java
public class MyHouseBuilder implements HouseBuilder {
    private int walls = 0;
    private int doors = 0;
    private int windows = 0;
    private boolean garden = false;
    private boolean garage = false;
    private int steps = 0;

    @Override
    public void buildWalls(int numWalls) { this.walls = numWalls; }
    @Override
    public void buildDoor(int numDoors) { this.doors = numDoors; }
    @Override
    public void buildWindows(int numWindows) { this.windows = numWindows; }
    @Override
    public void buildGarden() { this.garden = true; }
    @Override
    public void buildGarage() { this.garage = true; }
    @Override
    public void buildSteps(int numSteps) { this.steps = numSteps; }

    @Override
    public House getResult() {
        return new House(walls, doors, windows, garden, garage, steps);
    }
}
```

Product

```java
public class House {
    private final int walls;
    private final int doors;
    private final int windows;
    private final boolean garden;
    private final boolean garage;
    private final int steps;

    public House(int walls, int doors, int windows, boolean garden, boolean garage, int steps) {
        this.walls = walls;
        this.doors = doors;
        this.windows = windows;
        this.garden = garden;
        this.garage = garage;
        this.steps = steps;
    }

    @Override
    public String toString() {
        return "House(walls=" + walls + ", doors=" + doors + ", windows=" + windows
                + ", garden=" + garden + ", garage=" + garage + ", steps=" + steps + ")";
    }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        MyHouseBuilder gardenHouseBuilder = new MyHouseBuilder();
        gardenHouseBuilder.buildDoor(2);
        gardenHouseBuilder.buildGarden();
        gardenHouseBuilder.buildWalls(4);
        gardenHouseBuilder.buildSteps(9);
        gardenHouseBuilder.buildWindows(4);

        House gardenHouse = gardenHouseBuilder.getResult();
        System.out.println("Garden House = " + gardenHouse);

        MyHouseBuilder gardenWithGarageHouseBuilder = new MyHouseBuilder();
        gardenWithGarageHouseBuilder.buildDoor(2);
        gardenWithGarageHouseBuilder.buildGarden();
        gardenWithGarageHouseBuilder.buildGarage();
        gardenWithGarageHouseBuilder.buildWalls(4);
        gardenWithGarageHouseBuilder.buildSteps(25);
        gardenWithGarageHouseBuilder.buildWindows(4);

        House gardenWithGarageHouse = gardenWithGarageHouseBuilder.getResult();
        System.out.println("Garden with garage House = " + gardenWithGarageHouse);
    }
}
```
