# Design Patterns

This folder contains all 27 GoF (Gang of Four) design pattern implementations in **Kotlin** and **Java**, with PlantUML diagrams and Android use cases.

## 📁 Folder Structure

```
design_patterns/
├── creational/        # Object creation mechanisms (5 patterns)
│   ├── abstract_factory/
│   ├── builder/
│   ├── factory/            ← Factory Method pattern
│   ├── prototype/
│   ├── simple_factory/     ← Added
│   └── singleton/          ← Fixed typo (was singletone/)
├── structural/        # Object composition and relationships (7 patterns)
│   ├── adapter/
│   ├── bridge/
│   ├── composite/          ← Added
│   ├── decorator/          ← Added
│   ├── facade/
│   ├── flyweight/          ← Added
│   └── proxy/              ← Added
└── behavioral/        # Object communication and interaction (11 patterns)
    ├── chain_of_responsibility/  ← Added
    ├── command/                ← Added
    ├── interpreter/            ← Added
    ├── iterator/
    ├── mediator/               ← Added
    ├── memento/                ← Added
    ├── observer/
    ├── state/                  ← Added
    ├── strategy/
    ├── template_method/        ← Added
    └── visitor/                ← Added
```

---

## 🎯 All 27 GoF Patterns — ✅ 100% Complete

| Category | Patterns | Status |
|---|---|---|
| **Creational** | Abstract Factory, Builder, Factory Method, Prototype, Singleton | ✅ 5/5 |
| **Structural** | Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy | ✅ 7/7 |
| **Behavioral** | Chain of Responsibility, Command, Interpreter, Iterator, Mediator, Memento, Observer, State, Strategy, Template Method, Visitor | ✅ 11/11 |

---

## 📦 Each Pattern Includes

Every pattern folder contains:

- **`pattern_name.md`** — Complete documentation with:
  - Pattern description & intent
  - Steps to implement
  - Pros & cons
  - UML diagram (PlantUML source + rendered image link)
  - **Kotlin code** — idiomatic Kotlin implementation
  - **Java code** — Java implementation for comparison
  - Android use case

- **`pattern_name.puml`** — PlantUML source for the class diagram

- **`pattern_name.java`** — Standalone Java implementation (where applicable)

---

## 🛠️ Usage

Each pattern is self-contained in its folder. To explore a pattern:

1. Open `design_patterns/<category>/<pattern_name>/<pattern_name>.md`
2. Review the PlantUML diagram at `design_patterns/<category>/<pattern_name>/<pattern_name>.puml`
3. Examine the Kotlin and Java code examples in the markdown
4. Check the Android Use Case section for real-world applications

---

## 🔍 Quality Notes

- ✅ **`singleton`** — folder name typo fixed (was `singletone`)
- ✅ **Kotlin idioms** — Singleton uses `object` declaration; Memento uses data classes
- ✅ **Java examples** — All 27 patterns have embedded Java code in their markdown files
- ✅ **UML diagrams** — All 27 patterns have PlantUML `.puml` files
- ✅ **Android use cases** — Each pattern includes relevant Android application context

---

## 📖 Quick Reference

### Creational Patterns
| Pattern | Description |
|---|---|
| **Abstract Factory** | Creates families of related objects without specifying their concrete classes |
| **Builder** | Constructs complex objects step by step |
| **Factory Method** | Defines an interface for creating objects, lets subclasses decide which class to instantiate |
| **Prototype** | Creates new objects by copying existing ones |
| **Singleton** | Ensures a class has only one instance with global access |
| **Simple Factory** | A single factory method that creates objects based on a parameter |

### Structural Patterns
| Pattern | Description |
|---|---|
| **Adapter** | Converts one interface to another for compatibility |
| **Bridge** | Separates abstraction from implementation |
| **Composite** | Composes objects into tree structures for part-whole hierarchies |
| **Decorator** | Adds responsibilities to objects dynamically |
| **Facade** | Provides a simplified interface to a complex subsystem |
| **Flyweight** | Shares fine-grained objects to minimize memory usage |
| **Proxy** | Provides a placeholder for another object to control access |

### Behavioral Patterns
| Pattern | Description |
|---|---|
| **Chain of Responsibility** | Passes requests along a chain of handlers |
| **Command** | Encapsulates requests as objects |
| **Interpreter** | Defines grammar and interprets sentences |
| **Iterator** | Traverses collections without exposing underlying representation |
| **Mediator** | Reduces chaotic dependencies between objects |
| **Memento** | Captures and restores internal state |
| **Observer** | Notifies objects of state changes |
| **State** | Alters behavior based on internal state |
| **Strategy** | Defines family of interchangeable algorithms |
| **Template Method** | Defines skeleton of algorithm, defers steps to subclasses |
| **Visitor** | Adds new operations without changing element classes |

---

## 📚 How to Read a Pattern

1. Open `design_patterns/<category>/<pattern_name>/<pattern_name>.md`
2. Review the PlantUML diagram in `design_patterns/<category>/<pattern_name>/<pattern_name>.puml`
3. Examine the Kotlin and Java code examples in the markdown
4. Check the Android Use Case section for real-world applications

---

## 🔧 Technologies Used

- **Kotlin** — Primary implementation language (idiomatic patterns)
- **Java** — Secondary reference implementations
- **PlantUML** — UML class diagrams (`*.puml` files)
- **Android** — Real-world use case examples for each pattern
