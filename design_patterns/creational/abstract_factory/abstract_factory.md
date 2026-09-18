# Abstract Factory

- **Abstract Factory is a creational design pattern that lets you produce families of related objects without specifying their concrete classes.**




### Steps to create factory method 

1. **Abstract Factory** : Defines an interface of operations to create abstract objects
2. **Concrete Factory** : Implement the operation to create the concrete objects
3. **Abstract Product** : Define an interface for the specific type of object
4. **Concrete Object**  : Class definition of the object to be created by the Concrete Factory
5. **Client**: Class that uses interface defined in abstract factory


### Pros
1. You can be sure that the products you’re getting from a factory are compatible with each other. 
2. Single Responsibility Principle. Makes it easy to extend product construction code independently from rest of application
3. Open/Closed Principle. Allows adding new product without breaking the existing code

### Cons
1. The code may become more complicated since you need to introduce a lot of new subclasses to implement the pattern


### UML

[PlantUML for Factory Pattern](http://www.plantuml.com/plantuml/uml/fPCnJyCm48Lt_mgFC7Y8HrHPjIinzoyOzqeZ1GUThmEY_7SCf2Qnhn4IOr_VU_VT9TacyaZdbq7inXWG2FOu7iw1N2ULkXXv_IaVP1zFW3qbKRJDHIbP1_NSMceVVQ0HtwPM-vG8ipAfyqJzrsB6978Z2e1Tabk6tBBaTbdBiGzm9CNitENa3i4J9Fdweyai6gO2--SJrs6vZYoQXaFVvj1CHnPDKx72mNNOhmuX-aY0Hnz0_XvoKqHZtiKrOU_knKTEXC6drAxQ67cVRdmpXlebQb32znBxagyKnqmkp-b_SVtdU7DHT-kYRksYhckCl4_NlYt4G_xyFm40)

![Alt text](http://www.plantuml.com/plantuml/png/fPCnJyCm48Lt_mgFC7Y8HrHPjIinzoyOzqeZ1GUThmEY_7SCf2Qnhn4IOr_VU_VT9TacyaZdbq7inXWG2FOu7iw1N2ULkXXv_IaVP1zFW3qbKRJDHIbP1_NSMceVVQ0HtwPM-vG8ipAfyqJzrsB6978Z2e1Tabk6tBBaTbdBiGzm9CNitENa3i4J9Fdweyai6gO2--SJrs6vZYoQXaFVvj1CHnPDKx72mNNOhmuX-aY0Hnz0_XvoKqHZtiKrOU_knKTEXC6drAxQ67cVRdmpXlebQb32znBxagyKnqmkp-b_SVtdU7DHT-kYRksYhckCl4_NlYt4G_xyFm40 "a title")

---

### Kotlin code

Product 1
```agsl
/***
 * Product 1 and its concrete implementation
 */
interface Refill {
    fun color(): String
}

class BlueRefill : Refill {
    override fun color(): String {
        return "Blue"
    }

}

class BlackRefill : Refill {
    override fun color(): String {
        return "Black"
    }

}
```

Product 2

```agsl
/***
 * Product 2 and its concrete implementation
 */
interface Body {
    fun metal(): String
}

class GelPenBody : Body {
    override fun metal(): String {
        return "Steel"
    }

}

class BallPenBody : Body {
    override fun metal(): String {
        return "Plastic"
    }

}
```

Abstract factory
```agsl
***
 * Abstract factory and its concrete implementations
 */
interface PenFactory {
    fun getBody(): Body
    fun getRefill(): Refill
}

class BlueGelPenFactory : PenFactory {
    override fun getBody(): Body {
        return GelPenBody()
    }

    override fun getRefill(): Refill {
        return BlueRefill()
    }

}

class BlackBallPenFactory : PenFactory {
    override fun getBody(): Body {
        return GelPenBody()
    }

    override fun getRefill(): Refill {
        return BlackRefill()
    }

}
```
Client code

```agsl
fun main() {

    val type: Pen.PenType = Pen.PenType.Ball

    val penFactory: PenFactory = if (type == Pen.PenType.Ball) {
        BlackBallPenFactory()
    } else {
        BlueGelPenFactory()
    }

    Pen().write(penFactory)

}


class Pen {
    enum class PenType {
        Ball, Gel
    }

    fun write(factory: PenFactory) {
        print("Writing using˳${factory.getRefill().color()} ink and ${factory.getBody().metal()} body")
    }

}
```

---

### Java code

Product 1

```java
public interface Refill {
    String color();
}

public class BlueRefill implements Refill {
    @Override
    public String color() { return "Blue"; }
}

public class BlackRefill implements Refill {
    @Override
    public String color() { return "Black"; }
}
```

Product 2

```java
public interface Body {
    String metal();
}

public class GelPenBody implements Body {
    @Override
    public String metal() { return "Steel"; }
}

public class BallPenBody implements Body {
    @Override
    public String metal() { return "Plastic"; }
}
```

Abstract Factory

```java
public interface PenFactory {
    Body getBody();
    Refill getRefill();
}

public class BlueGelPenFactory implements PenFactory {
    @Override
    public Body getBody() { return new GelPenBody(); }
    @Override
    public Refill getRefill() { return new BlueRefill(); }
}

public class BlackBallPenFactory implements PenFactory {
    @Override
    public Body getBody() { return new BallPenBody(); }
    @Override
    public Refill getRefill() { return new BlackRefill(); }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        PenFactory penFactory = new BlueGelPenFactory();
        Pen pen = new Pen();
        pen.write(penFactory);
    }
}

class Pen {
    public void write(PenFactory factory) {
        System.out.println("Writing using " + factory.getRefill().color()
                + " ink and " + factory.getBody().metal() + " body");
    }
}
```
