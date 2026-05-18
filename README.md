# Confusion Atoms Refactoring Plugin for IntelliJ IDEA

An IntelliJ IDEA extension developed as part of the research project **"To Refactor or Not to Refactor Atoms of Confusion? Evidence from a Scoping Review, Bytecode-Level Analysis, and Tool Support"**. The plugin detects and refactors Atoms of Confusion (ACs) in Java source code using PSI-based semantic analysis via the JetBrains Plugin SDK.

---

## Overview

Atoms of Confusion are the smallest syntactic code patterns that are formally correct but commonly misinterpreted by developers. This plugin provides IDE-assisted detection and refactoring of selected ACs directly within IntelliJ IDEA, offering real-time warnings and quick fixes without requiring external tools.

---

## Requirements

- IntelliJ IDEA (Community or Ultimate) 2023.3+
- JDK 17+
- Gradle (managed via wrapper)

---

## Installation

### From source

```bash
git clone <repository-url>
cd aoc-refactor-extension
./gradlew buildPlugin
```

The generated `.zip` file will be located at `build/distributions/`. To install:

`Settings → Plugins → ⚙️ → Install Plugin from Disk`

### Running in sandbox

```bash
./gradlew runIde
```

---

## Project Structure

```
aoc-refactor/
├── build.gradle.kts
├── src/main/
│   ├── java/br/ufc/aocrefactor/
│   │   ├── inspection/
│   │   │   ├── ConditionalOperatorInspection.java
│   │   │   ├── InfixOperatorPrecedenceInspection.java
│   │   │   ├── PreIncrementDecrementInspection.java
│   │   │   ├── PostIncrementDecrementInspection.java
│   │   │   ├── ArithmeticAsLogicInspection.java
│   │   │   └── TypeConversionInspection.java
│   │   └── quickfix/
│   │       ├── ReplaceConditionalOperatorQuickFix.java
│   │       ├── WrapWithParenthesesQuickFix.java
│   │       ├── ReplacePreIncrementQuickFix.java
│   │       ├── ReplacePostIncrementQuickFix.java
│   │       ├── ReplaceArithmeticAsLogicQuickFix.java
│   │       └── TypeConversionQuickFix.java
│   └── resources/META-INF/
│       └── plugin.xml
```

---

## Implementation

The plugin was developed using the **JetBrains Plugin SDK** with the **Local Inspections** mechanism over the **PSI (Program Structure Interface)** — the semantic tree structure that IntelliJ uses internally to represent Java source code.

Each atom is implemented as an **Inspection/QuickFix** pair. The Inspection traverses the PSI tree via the **Visitor** design pattern, identifying nodes that match the atom's syntactic pattern. The QuickFix applies the refactoring directly on the tree, without textual manipulation, ensuring the transformation respects the program's semantic structure.

---

## Implemented Atoms

### 3.3.4 Conditional Operator

Detects every occurrence of the ternary operator `?:` and offers a refactoring to an explicit `if-else` structure.

```java
// Before
int b = a == 3 ? 2 : 1;

// After
int b;
if (a == 3) { b = 2; } else { b = 1; }
```

Covered contexts: variable assignment, method parameter, return statement.

---

### 3.3.1 Infix Operator Precedence

Detects arithmetic expressions where `*`, `/` or `%` appear under `+` or `-` without explicit parentheses, and logical expressions where `&&` and `||` are mixed without parentheses.

```java
// Before
int x = a + b * c;
boolean l = p || q && r;

// After
int x = a + (b * c);
boolean l = p || (q && r);
```

String concatenation expressions are excluded from detection.

---

### 3.3.2 Pre-Increment/Decrement

Detects the prefix `++`/`--` operator in contexts where it may cause confusion: variable assignment, binary operation, method parameter, array index, and return statement. Refactors by separating the increment into its own line **before** the parent statement.

```java
// Before
int b = ++a;

// After
a++;
int b = a;
```

---

### 3.3.3 Post-Increment/Decrement

Same set of contexts as Pre-Increment/Decrement, but for the suffix operator. The increment is placed **after** the parent statement, preserving the original value semantics.

```java
// Before
int b = a++;

// After
int b = a;
a++;
```

---

### 3.3.5 Arithmetic as Logic

Detects arithmetic expressions compared to zero via `==` or `!=` and replaces them with explicit logical equivalents.

| Before | After |
|---|---|
| `a * b == 0` | `a == 0 \|\| b == 0` |
| `a * b != 0` | `a != 0 && b != 0` |
| `a - b == 0` | `a == b` |
| `a - b != 0` | `a != b` |
| `a + b == 0` | `a == -b` |

---

### 3.3.10 Type Conversion

Detects explicit narrowing conversions between primitive types that are performed without the use of treatment APIs or the modulo operator. The following narrowing paths are covered:

| From | To |
|---|---|
| `short` | `byte`, `char` |
| `char` | `byte`, `short` |
| `int` | `byte`, `short`, `char` |
| `long` | `byte`, `short`, `char`, `int` |
| `float` | `byte`, `short`, `char`, `int`, `long` |
| `double` | `byte`, `short`, `char`, `int`, `long`, `float` |

When a narrowing conversion is detected, the developer is warned and may choose to apply a QuickFix that inserts a comment on the preceding line flagging the conversion and the possible precision loss involved.

```java
// Before
byte b = (byte) a;

// After (with QuickFix applied)
// Narrowing conversion: short -> byte (possible precision loss)
byte b = (byte) a;
```

Excluded from detection: casts whose operand contains a method invocation (possible API usage) or a modulo operation (explicit data treatment). For literal operands, detection only occurs when the value is outside the representable range of the target type.

---

## Technical Approach

The highlight range registered by `registerProblem` is deliberately set to the **parent node** of the detected expression in cases where the atom's context adds relevant semantic information — for example, the full binary expression `3 + a++` rather than just `a++`. This is achieved by the `getHighlightTarget` method in each inspection, which selects the most informative PSI node to highlight based on the parent context.

QuickFixes locate the target sub-expression using `PsiTreeUtil.findChildOfType`, which recursively descends the PSI tree from the highlighted node, ensuring the fix works correctly regardless of which node was used as the highlight target.

---

## Related Work

This plugin is part of the broader research project described in:

> *To Refactor or Not to Refactor Atoms of Confusion? Evidence from a Scoping Review, Bytecode-Level Analysis, and Tool Support* (Anonymous, 2026)

---

## License

This project is developed for academic research purposes.
