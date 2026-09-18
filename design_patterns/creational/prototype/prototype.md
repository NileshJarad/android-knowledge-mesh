# Prototype

1. **Prototype is Creational design pattern**
2. **It lets you to copy existing object even complex once without depending on their specific classes**

### Steps to create prototype pattern

1. Add **Prototype** interface/abstract class
2. Implement **Prototype** interface/abstract to **ConcretePrototype** which clones data to new cloned object

### Pros

1. Clones object without coupling to class
2. Can get rid of repeated initializing code.

### Cons

1. It gets tricky when cloning of complex object which has circular dependency

### UML

[PlantUML for Prototype Pattern](https://www.plantuml.com/plantuml/uml/ROv1ImCn48NlyolUKIakw74ffU39cmV_G9gTTID99YJJWwpzxwuRkb4gv32yz-NZ7OkQhEVWwJMBYducnlwhwpeyIxIPbRUSo4NbF1hB-0Nlj_Hmm4oxsPoAPcFr0kibyjqz-iRJXSXwKygjVwO7Z8xzy7WzdeXs_pkYA2ExzqaXOslwViK9U9jSmV8CqZOri4uoTCHnm8-0_chQeblTYyA5v3bmr0AT66jpLQ8YLfBZMijYTT5uoEc3hKAb_gZbchKTn-Kw1_yD)

![Alt text](http://www.plantuml.com/plantuml/png/ROv1ImCn48NlyolUKIakw74ffU39cmV_G9gTTID99YJJWwpzxwuRkb4gv32yz-NZ7OkQhEVWwJMBYducnlwhwpeyIxIPbRUSo4NbF1hB-0Nlj_Hmm4oxsPoAPcFr0kibyjqz-iRJXSXwKygjVwO7Z8xzy7WzdeXs_pkYA2ExzqaXOslwViK9U9jSmV8CqZOri4uoTCHnm8-0_chQeblTYyA5v3bmr0AT66jpLQ8YLfBZMijYTT5uoEc3hKAb_gZbchKTn-Kw1_yD)

### Kotlin code

Prototype interface

```agsl
interface Duck {
    fun clone(): Duck
}

```

ConcretePrototype class

```agsl
class ToyDuck : Duck {

    var color: String = ""
    var mfDate: String = ""

    override fun clone(): Duck {
        return ToyDuck().also {
            it.color = this.color
            it.mfDate = this.mfDate
        }
    }

    override fun toString(): String {
        return "ToyDuck(color='$color', mfDate='$mfDate')"
    }
}
```

Client code

```agsl
    val toyDuck = ToyDuck()
    toyDuck.color = "red"
    toyDuck.mfDate = "12/3/2023"

    val toyDuckClone = toyDuck.clone() as ToyDuck
    toyDuckClone.color = "redCloned"

    println("toyDuck  = $toyDuck")
    println("toyDuckClone  = $toyDuckClone")
```
---

### Java code

Prototype interface

```java
public interface Duck {
    Duck clone();
}
```

Concrete Prototype

```java
public class ToyDuck implements Duck {
    private String color;
    private String mfDate;

    public void setColor(String color) { this.color = color; }
    public void setMfDate(String mfDate) { this.mfDate = mfDate; }

    @Override
    public Duck clone() {
        ToyDuck cloned = new ToyDuck();
        cloned.color = this.color;
        cloned.mfDate = this.mfDate;
        return cloned;
    }

    @Override
    public String toString() {
        return "ToyDuck(color='" + color + "', mfDate='" + mfDate + "')";
    }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        ToyDuck toyDuck = new ToyDuck();
        toyDuck.setColor("red");
        toyDuck.setMfDate("12/3/2023");

        ToyDuck toyDuckClone = (ToyDuck) toyDuck.clone();
        toyDuckClone.setColor("redCloned");

        System.out.println("toyDuck = " + toyDuck);
        System.out.println("toyDuckClone = " + toyDuckClone);
    }
}
```
