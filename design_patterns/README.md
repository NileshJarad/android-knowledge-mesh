# Design Patterns

This folder contains GoF (Gang of Four) design pattern implementations in Kotlin and Java.

## 📁 Folder Structure

The patterns are organized by category:

```
design_patterns/
├── creational/        # Object creation mechanisms
│   ├── abstract_factory/
│   ├── builder/
│   ├── factory/       ← Factory Method pattern
│   ├── prototype/
│   └── singletone/
├── structural/        # Object composition and relationships
│   ├── adapter/
│   ├── bridge/
│   └── facade/
└── behavioral/        # Object communication and interaction
    ├── iterator/
    ├── observer/
    └── strategy/
```

---

## 🎯 Available Patterns (10 of 27 GoF)

| Category | Patterns | Status |
|---|---|---|
| **Creational** | Abstract Factory, Builder, Factory Method, Prototype, Singleton | ✅ 5/5 |
| **Structural** | Adapter, Bridge, Facade | ⚠️ 3/7 |
| **Behavioral** | Iterator, Observer, Strategy | ⚠️ 3/11 |

---

## 🔄 Missing Patterns (17 needed)

### Creational (0 missing — all 5 present)
### Structural (7 missing)
- Composite
- Decorator
- **Flyweight**
- **Proxy**
- **Chain of Responsibility**
### Behavioral (11 missing)
- **Template Method**
- **Command**
- **Memento**
- **State**
- **Visitor**
- **Interpreter**
- **Mediator**
- **Chain of Responsibility**

---

## 📦 Each Pattern Includes

When a pattern is present, it typically contains:

- **`pattern_name.md`** — Markdown documentation with:
  - Pattern description & intent
  - Steps to implement
  - Pros & cons
  - UML diagram (PlantUML)
  - Kotlin code example
  - Java code example (where applicable)

- **`pattern_name.puml`** — PlantUML source for the class diagram

---

## 🛠️ Usage

Each pattern is self-contained in its folder. See the individual `README.md` in each pattern directory, or read the `.md` file for complete implementation details.

---

## 🔍 Quality Notes

- ⚠️ **`singletone`** — folder name has a typo (should be `singleton`). Fix recommended.
- ⚠️ **Naming inconsistency** — `factory/` folder contains `factory_method.md`; consider renaming to match or standardizing.
- ⚠️ **No Kotlin idioms** — Most patterns lack Kotlin-specific implementations (e.g., `object` declarations for Singleton; `data classes` for Memento).
- 📝 **Recommendation**: Add Kotlin code to all patterns, fix folder naming, and complete the 17 missing patterns.

---

## 📖 How to Read a Pattern

1. Open `design_patterns/<category>/<pattern_name>/<pattern_name>.md`
2. Review the PlantUML diagram in `design_patterns/<category>/<pattern_name>/<pattern_name>.puml`
3. Examine the code examples in the markdown
4. Run the Kotlin examples if available