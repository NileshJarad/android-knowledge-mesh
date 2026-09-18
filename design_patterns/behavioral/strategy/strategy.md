# Strategy Pattern

- **Is a Behavioral design pattern that allows you to define a family of interchangeable algorithms, encapsulate each one as an object, and make them interchangeable at runtime.**


### Steps to create strategy method

1. **Strategy** : Defines an interface of operations to create strategy
2. **Concrete Strategy** : Implement the operation to create the concrete strategy
3. **Context/ Base** : Define a base for inheritance
4. **Client**  : Client code to decide the specific strategy to implement


### Pros
- Can swipe algorithms at runtime based
- Can replace inheritance with composition
- Open/Closed Principle. You can introduce new strategies without having to change the context.


### Cons
- Client should know the different strategies 

### UML

[PlantUML for Strategy Pattern](http://www.plantuml.com/plantuml/uml/XP51IyGm48NlyolUqJqau1wbXiMzUFSNn7Iw3SHKakcATFlVRIsfO2tUXY_p7czUJbbialujq8iaAtpvH7NJ6kSkkiJ2XTx_ytwa9viPAlbBAygWy0SysmnBT0wUeq3rxqhT3EPj8XTipj_pG52useIy2ei5eUaZCai1TzkT0ur4FWgdnZfUSLMjp9Y1i3WLcrbKCcpk7UZNyg2r7cdS4npt1R4JHk16q3LOUY2WUZNBZ9gThpdZe_MkNGqW7Vgv9E1gmmz-t6QTBW6azLd_rTA1EDQOGz1fcgQ__W80)

![Alt text](http://www.plantuml.com/plantuml/png/XP51IyGm48NlyolUqJqau1wbXiMzUFSNn7Iw3SHKakcATFlVRIsfO2tUXY_p7czUJbbialujq8iaAtpvH7NJ6kSkkiJ2XTx_ytwa9viPAlbBAygWy0SysmnBT0wUeq3rxqhT3EPj8XTipj_pG52useIy2ei5eUaZCai1TzkT0ur4FWgdnZfUSLMjp9Y1i3WLcrbKCcpk7UZNyg2r7cdS4npt1R4JHk16q3LOUY2WUZNBZ9gThpdZe_MkNGqW7Vgv9E1gmmz-t6QTBW6azLd_rTA1EDQOGz1fcgQ__W80)

---


How to identity when to user strategy pattern


1. The need to switch behavior dynamically: If you have a situation where you need to switch the behavior of an object at runtime, depending on certain conditions or user inputs, then the Strategy pattern can be a good choice. In contrast, normal inheritance is a static relationship between classes, and the behavior of an object is fixed at compile time. 
2. The need for different variations of behavior: If you have a situation where you need to support multiple variations of a behavior, then the Strategy pattern can be useful. For example, if you have multiple payment methods with different algorithms for processing payments, then you can encapsulate each algorithm in a separate payment strategy class. In contrast, normal inheritance typically involves creating subclasses that inherit the behavior of the parent class. 
3. The need to avoid code duplication: If you have a situation where you need to duplicate code across multiple classes that share similar behavior, then the Strategy pattern can be a good choice. By encapsulating the behavior in separate strategy classes, you can avoid duplicating code across different classes that implement the same behavior. In contrast, normal inheritance can lead to code duplication if you need to override the same behavior in multiple subclasses. 
4. The need for decoupling: If you have a situation where you want to decouple the implementation of a behavior from the class that uses it, then the Strategy pattern can be a good choice. By encapsulating the behavior in separate strategy classes, you can change the implementation of the behavior without affecting the class that uses it. In contrast, normal inheritance can lead to tight coupling between classes, making it difficult to change the behavior without affecting other parts of the system.

---

### Kotlin code

Strategy

```agsl
interface PaymentStrategy {
    fun pay(amount: Double)
}

```
Concrete Strategy

```agsl
class CreditCardStrategy(
    private val name: String,
    private val cardNumber: String,
    private val cvv: String,
    private val dateOfExpiry: String
) :
    PaymentStrategy {
    override fun pay(amount: Double) {
        // logic for processing credit card payment
    }
}


class PayPalStrategy(private val emailId: String, private val password: String) : PaymentStrategy {
    override fun pay(amount: Double) {
        // logic for processing PayPal payment
    }
}


class ApplePayStrategy(private val deviceToken: String) : PaymentStrategy {
    override fun pay(amount: Double) {
        // logic for processing Apple Pay payment
    }
}
```

Context 

```agsl
open class Base {
    private var strategy: PaymentStrategy? = null
    fun setStrategy(strategy: PaymentStrategy) {
        this.strategy = strategy
    }

    fun doPayment(amount: Double) {
        strategy?.pay(amount)
    }
}
```

Client

```agsl
class Client : Base() {
    fun doApplePayment() {
        setStrategy(ApplePayStrategy("token"))
        doPayment(40.0)
    }
}
```
---

### Java code

Strategy interface

```java
public interface PaymentStrategy {
    void pay(double amount);
}
```

Concrete Strategies

```java
public class CreditCardStrategy implements PaymentStrategy {
    private final String name;
    private final String cardNumber;
    private final String cvv;
    private final String dateOfExpiry;

    public CreditCardStrategy(String name, String cardNumber, String cvv, String dateOfExpiry) {
        this.name = name;
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.dateOfExpiry = dateOfExpiry;
    }

    @Override
    public void pay(double amount) {
        System.out.println("Processing credit card payment of $" + amount);
        System.out.println("Name: " + name);
        System.out.println("Card: " + cardNumber);
        // Payment logic...
    }
}

public class PayPalStrategy implements PaymentStrategy {
    private final String emailId;
    private final String password;

    public PayPalStrategy(String emailId, String password) {
        this.emailId = emailId;
        this.password = password;
    }

    @Override
    public void pay(double amount) {
        System.out.println("Processing PayPal payment of $" + amount);
        System.out.println("Email: " + emailId);
        // Payment logic...
    }
}

public class ApplePayStrategy implements PaymentStrategy {
    private final String deviceToken;

    public ApplePayStrategy(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    @Override
    public void pay(double amount) {
        System.out.println("Processing Apple Pay payment of $" + amount);
        System.out.println("Device token: " + deviceToken);
        // Payment logic...
    }
}
```

Context

```java
import java.util.Optional;

public class Base {
    private PaymentStrategy strategy;

    public void setStrategy(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    public void doPayment(double amount) {
        if (strategy != null) {
            strategy.pay(amount);
        } else {
            System.out.println("No strategy set!");
        }
    }
}
```

Client code

```java
public class Client {
    public static void main(String[] args) {
        Base paymentProcessor = new Base();

        paymentProcessor.setStrategy(new CreditCardStrategy("John Doe", "1234-5678-9012-3456", "123", "12/30"));
        paymentProcessor.doPayment(40.0);

        paymentProcessor.setStrategy(new PayPalStrategy("john@example.com", "password123"));
        paymentProcessor.doPayment(25.0);

        paymentProcessor.setStrategy(new ApplePayStrategy("device-token-abc"));
        paymentProcessor.doPayment(30.0);
    }
}
```
