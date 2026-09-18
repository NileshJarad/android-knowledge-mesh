# Factory method

1. **Factory is Creational design pattern. It uses factory method to deal object creation without having to specify the actual class of the object that will be created.**
2. **A Factory Pattern or Factory Method Pattern says that just define an interface or abstract class for creating an object but let the subclasses decide which class to instantiate.**
3. **The Factory Method Pattern is also known as  Virtual Constructor.**
4. **Adheres to Open closed principle.**
5. **It heavily relies on inheritance**




### Steps to create factory method 

1. Declare interface(it can be abstract class or interface) as **Product**
2. Make all concrete product follows the same interface **(Product)**
3. Add empty factory method inside the creator class that returns type of Product
4. Implement concrete creator for each product type
5. Added factory class to create object based on parameter to function


### Pros
1. Single Responsibility Principle. Makes it easy to extend product construction code independently from rest of application
2. Open/Closed Principle. Allows adding new product without breaking the existing code



### Cons
1. The code may become more complicated since you need to introduce a lot of new subclasses to implement the pattern


### UML

[PlantUML for Factory Pattern](http://www.plantuml.com/plantuml/uml/fPA_JiGm3CPtFyKtmd2mLrLrqaqCI0oyG9gwj2X_Jeax8E3T2Rcqfmj_bfCwM9_zsJ-xsKTHGHPhw2duOT62zcEtmy4x7LYuY7G8hCI7osGHYIe1F_nea7CSqH7Hx8J3g3IZvDjsaxhkdN0yoOyianzkRkapcRHHCTQDVwdDnQgF4fGMRE3jAkK1QL9It_uPK8nLxJR0UM64-MKI-B6cQfeBA0AUfpaYVJ8nPdiqRDc9ajcxPC_gHcop1sFmbh0U2Djzxi-L9s2Pb22mBC55j7rN_pfs0xvf2xFXCIDNUpPDlUOLqANdtz04FAImF0dba3ABCl3zAsjPkvSZtfrNsATeiUOB)

![Alt text](http://www.plantuml.com/plantuml/png/fPA_JiGm3CPtFyKtmd2mLrLrqaqCI0oyG9gwj2X_Jeax8E3T2Rcqfmj_bfCwM9_zsJ-xsKTHGHPhw2duOT62zcEtmy4x7LYuY7G8hCI7osGHYIe1F_nea7CSqH7Hx8J3g3IZvDjsaxhkdN0yoOyianzkRkapcRHHCTQDVwdDnQgF4fGMRE3jAkK1QL9It_uPK8nLxJR0UM64-MKI-B6cQfeBA0AUfpaYVJ8nPdiqRDc9ajcxPC_gHcop1sFmbh0U2Djzxi-L9s2Pb22mBC55j7rN_pfs0xvf2xFXCIDNUpPDlUOLqANdtz04FAImF0dba3ABCl3zAsjPkvSZtfrNsATeiUOB "a title")


----


### Kotlin code


Creator interface

```agsl
interface PlanCreator {
    fun createPlan(): Plan
}

```
Concrete creators
```agsl
class DomesticPlanCreator : PlanCreator {
    override fun createPlan() = DomesticPlan()
}

class CommercialPlanCreator : PlanCreator {
    override fun createPlan() = CommercialPlan()
}

class InstitutionalPlanCreator : PlanCreator {
    override fun createPlan() = InstitutionalPlan()
}
```


Product abstract class

```agsl
abstract class Plan {
    private val baseRate = 100f
    fun getPlanDetails(): Float {
        return baseRate + getRate()
    }

    abstract fun getRate(): Float
}

```

Concrete Product

```agsl
class DomesticPlan : Plan() {
    override fun getRate(): Float {
        return 200f
    }
}

class CommercialPlan : Plan() {
    override fun getRate(): Float {
        return 400f
    }
}

class InstitutionalPlan : Plan() {
    override fun getRate(): Float {
        return 600f
    }
}
```

Factory Utility

```agsl
class ByTypePlanFactory : PlanFactory() {
    override fun getPlan(plan: PlansEnum): Plan? {
        return when (plan) {
            PlansEnum.Domestic -> DomesticPlanCreator().createPlan()
            PlansEnum.Commercial -> CommercialPlanCreator().createPlan()
            PlansEnum.Institutional -> InstitutionalPlanCreator().createPlan()
            else -> null
        }

    }

}
```

Client code

```agsl
    val planFactory = ByTypePlanFactory()
    val institutionalPlan = planFactory.getPlan(PlanFactory.PlansEnum.Institutional)
    val commercialPlan = planFactory.getPlan(PlanFactory.PlansEnum.Commercial)
    val domesticPlan = planFactory.getPlan(PlanFactory.PlansEnum.Domestic)
    println()
    println("Institutional Plan detail = ${institutionalPlan?.getPlanDetails()}")
    println("Commercial Plan detail = ${commercialPlan?.getPlanDetails()}")
    println("Domestic Plan detail = ${domesticPlan?.getPlanDetails()}")
```

---

### Java code

Product abstract class

```java
public abstract class Plan {
    private final float baseRate = 100f;

    public float getPlanDetails() {
        return baseRate + getRate();
    }

    protected abstract float getRate();
}
```

Concrete Products

```java
public class DomesticPlan extends Plan {
    @Override
    protected float getRate() { return 200f; }
}

public class CommercialPlan extends Plan {
    @Override
    protected float getRate() { return 400f; }
}

public class InstitutionalPlan extends Plan {
    @Override
    protected float getRate() { return 600f; }
}
```

Creator interface and concrete creators

```java
public interface PlanCreator {
    Plan createPlan();
}

public class DomesticPlanCreator implements PlanCreator {
    @Override
    public Plan createPlan() { return new DomesticPlan(); }
}

public class CommercialPlanCreator implements PlanCreator {
    @Override
    public Plan createPlan() { return new CommercialPlan(); }
}

public class InstitutionalPlanCreator implements PlanCreator {
    @Override
    public Plan createPlan() { return new InstitutionalPlan(); }
}
```

Factory utility

```java
public abstract class PlanFactory {
    public enum PlansEnum {
        Domestic, Commercial, Institutional
    }

    public abstract Plan getPlan(PlansEnum plan);
}

public class ByTypePlanFactory extends PlanFactory {
    @Override
    public Plan getPlan(PlansEnum plan) {
        switch (plan) {
            case Domestic: return new DomesticPlanCreator().createPlan();
            case Commercial: return new CommercialPlanCreator().createPlan();
            case Institutional: return new InstitutionalPlanCreator().createPlan();
            default: return null;
        }
    }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        ByTypePlanFactory planFactory = new ByTypePlanFactory();

        Plan institutionalPlan = planFactory.getPlan(PlanFactory.PlansEnum.Institutional);
        Plan commercialPlan = planFactory.getPlan(PlanFactory.PlansEnum.Commercial);
        Plan domesticPlan = planFactory.getPlan(PlanFactory.PlansEnum.Domestic);

        System.out.println("Institutional Plan detail = " + institutionalPlan.getPlanDetails());
        System.out.println("Commercial Plan detail = " + commercialPlan.getPlanDetails());
        System.out.println("Domestic Plan detail = " + domesticPlan.getPlanDetails());
    }
}
```
