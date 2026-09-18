# Interpreter Pattern

- **Interpreter is a Behavioral design pattern that provides a way to evaluate language grammar or expression by using a tree-like structure of objects.**
- It defines a representation for a language's grammar and an interpreter to use the representation to interpret sentences.

### Use Case
- Mathematical expression evaluators
- Rule engines
- SQL parsers
- Regular expression engines

### Steps to create Interpreter

1. **Expression** : Declares the abstract interface for interpreting expressions.
2. **Terminal Expression** : Implements interpret for terminal symbols (e.g., numbers).
3. **Non-terminal Expression** : Implements interpret for operators combining sub-expressions.
4. **Context** : Contains global information for the interpreter.

### Pros
1. Easy to change and extend the grammar.
2. Implements the grammar easily.

### Cons
1. Can be inefficient for complex grammars.
2. Hard to maintain for large grammars.

---

### Kotlin code

Expression interface

```kotlin
interface Expression {
    fun interpret(context: Context): Boolean
}
```

Terminal Expression — Variable

```kotlin
class VariableExpression(private val name: String) : Expression {
    override fun interpret(context: Context): Boolean {
        return context.lookup(name) == true
    }
}
```

Non-terminal Expression — And

```kotlin
class AndExpression(
    private val expr1: Expression,
    private val expr2: Expression
) : Expression {
    override fun interpret(context: Context): Boolean {
        return expr1.interpret(context) && expr2.interpret(context)
    }
}
```

Non-terminal Expression — Or

```kotlin
class OrExpression(
    private val expr1: Expression,
    private val expr2: Expression
) : Expression {
    override fun interpret(context: Context): Boolean {
        return expr1.interpret(context) || expr2.interpret(context)
    }
}
```

Non-terminal Expression — Not

```kotlin
class NotExpression(private val expr: Expression) : Expression {
    override fun interpret(context: Context): Boolean {
        return !expr.interpret(context)
    }
}
```

Context — stores variable values

```kotlin
class Context {
    private val variables = mutableMapOf<String, Boolean>()

    fun assign(varName: String, value: Boolean) {
        variables[varName] = value
    }

    fun lookup(varName: String): Boolean {
        return variables.getOrDefault(varName, false)
    }
}
```

Client code

```kotlin
fun main() {
    val context = Context()
    context.assign("A", true)
    context.assign("B", false)

    val expression = AndExpression(
        VariableExpression("A"),
        OrExpression(
            VariableExpression("B"),
            NotExpression(VariableExpression("A"))
        )
    )

    val result = expression.interpret(context)
    println("Result: $result")
}
```

### Java code

Expression interface

```java
public interface Expression {
    boolean interpret(Context context);
}
```

Terminal Expression — Variable

```java
public class VariableExpression implements Expression {
    private final String name;

    public VariableExpression(String name) {
        this.name = name;
    }

    @Override
    public boolean interpret(Context context) {
        return context.lookup(name);
    }
}
```

Non-terminal Expression — And

```java
public class AndExpression implements Expression {
    private final Expression expr1;
    private final Expression expr2;

    public AndExpression(Expression expr1, Expression expr2) {
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    @Override
    public boolean interpret(Context context) {
        return expr1.interpret(context) && expr2.interpret(context);
    }
}
```

Non-terminal Expression — Or

```java
public class OrExpression implements Expression {
    private final Expression expr1;
    private final Expression expr2;

    public OrExpression(Expression expr1, Expression expr2) {
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    @Override
    public boolean interpret(Context context) {
        return expr1.interpret(context) || expr2.interpret(context);
    }
}
```

Non-terminal Expression — Not

```java
public class NotExpression implements Expression {
    private final Expression expr;

    public NotExpression(Expression expr) {
        this.expr = expr;
    }

    @Override
    public boolean interpret(Context context) {
        return !expr.interpret(context);
    }
}
```

Context class

```java
import java.util.HashMap;
import java.util.Map;

public class Context {
    private final Map<String, Boolean> variables = new HashMap<>();

    public void assign(String varName, boolean value) {
        variables.put(varName, value);
    }

    public boolean lookup(String varName) {
        return variables.getOrDefault(varName, false);
    }
}
```

Client code

```java
public class Main {
    public static void main(String[] args) {
        Context context = new Context();
        context.assign("A", true);
        context.assign("B", false);

        Expression expression = new AndExpression(
                new VariableExpression("A"),
                new OrExpression(
                        new VariableExpression("B"),
                        new NotExpression(new VariableExpression("A"))
                )
        );

        boolean result = expression.interpret(context);
        System.out.println("Result: " + result);
    }
}
```

---

### Android Use Case
- **ConstraintLayout**: Parses constraint expressions.
- **Room**: SQLite query compiler uses expression trees.
- **Firebase Rules**: Evaluates security rules.