# TDD Adoption — Team Leadership Interview Question

## Interview Question

**How would you ask your team to comply with TDD development? What would you do if people still don't follow TDD even after you have explained and asked them to follow it?**

---

## Answer

I would not start by forcing the team to follow TDD. First, I would try to understand **why the team is not adopting it**.

There could be several reasons:

* Developers don't understand TDD properly.
* They are not comfortable writing tests first.
* They feel TDD slows down development.
* The existing test infrastructure is difficult to use.
* There is pressure to deliver features quickly.
* They don't see the value of TDD.
* The team has had bad experiences with poorly designed tests.

### 1. Understand the resistance

I would have discussions with the team and individual developers to understand the actual problem.

For example:

> "What is making TDD difficult for you?"

Rather than assuming that developers are simply unwilling to follow the process, I would first identify the underlying problem.

---

### 2. Educate and demonstrate

If the problem is lack of understanding, I would conduct a short practical session and demonstrate the TDD cycle:

**Red → Green → Refactor**

For example:

1. Write a failing test.
2. Write the minimum implementation required to make it pass.
3. Refactor the implementation.
4. Repeat.

I would use an actual feature from our project rather than giving only theoretical training.

---

### 3. Lead by example

As a senior/lead engineer, I should demonstrate the behavior I expect from the team.

For example, if we are implementing a new business rule, I would write the tests first and invite another developer for pair programming.

This helps the team see that TDD is a practical development technique rather than just a process imposed by management.

---

### 4. Start with a pilot

I wouldn't necessarily ask the entire team to completely change their development process overnight.

I would select an appropriate feature or module and implement it using TDD.

Then we can evaluate:

* Number of defects
* Regression issues
* Test maintainability
* Development effort
* Refactoring confidence
* Production issues

If the experiment demonstrates value, we can gradually expand the practice.

---

### 5. Remove technical obstacles

Sometimes developers don't follow TDD because the testing environment itself is painful.

As a lead, I would help improve things such as:

* Test setup
* Mocking infrastructure
* Test fixtures
* Test utilities
* CI execution time
* Test isolation
* Documentation
* Examples/templates

For Android, for example, I would establish clear testing patterns for:

```text
ViewModel
    ↓
Use Case
    ↓
Repository
    ↓
Data Source
```

and demonstrate how to unit-test each layer effectively.

---

### 6. Make it a team engineering standard

If we have educated the team, removed the obstacles, and demonstrated the benefits, I would then establish the expectation as a **team engineering standard**.

For example:

> "For business-critical production logic, automated tests are part of our Definition of Done."

The expectation should be clear and agreed upon by the team.

---

### 7. Use code review and CI for consistency

I wouldn't depend only on people remembering to follow the process.

We can use:

* Pull-request reviews
* CI checks
* Automated tests
* Static analysis
* Definition of Done
* Coverage thresholds where appropriate

However, I would **not make 100% code coverage the objective**.

The objective is:

> **Confidence in the software and prevention of regressions.**

Coverage is only one metric that can help us understand testing quality.

---

## What if someone still refuses?

If a developer continues not to follow the agreed practice, I would have a **one-on-one conversation**.

First, I would determine whether there is still a legitimate technical concern.

If there is, we address it.

If there isn't, I would explain that engineering standards are **team agreements, not individual preferences**.

For example:

> "You may personally prefer not to use TDD, but the team has agreed that business-critical production logic requires automated tests. As a team member, we need everyone to follow that standard."

If the person continues to ignore the standard despite coaching, support, and clear expectations, then it becomes an **accountability/performance issue**, rather than a technical disagreement.

I would handle that progressively and fairly, rather than immediately escalating.

---

## Leadership Approach

My overall approach would be:

**Understand → Educate → Demonstrate → Pilot → Measure → Standardize → Enforce**

As a senior/lead engineer, my responsibility is not simply to tell people what to do.

My responsibility is to:

* Explain **why** the practice matters.
* Understand resistance.
* Remove obstacles.
* Lead by example.
* Build team consensus.
* Establish clear engineering standards.
* Create accountability when necessary.

---

## Important Staff-Level Point

I would also avoid treating TDD as a religious rule.

Not every piece of code necessarily needs exactly the same development approach.

I would apply engineering judgment based on:

* Business criticality
* Complexity
* Risk
* Expected change frequency
* Cost of failure
* Type of component

For example, **business/domain logic** is often an excellent candidate for TDD because it is deterministic and highly testable.

The goal isn't:

> "Everyone must write tests first because TDD is the rule."

The goal is:

> **"We should choose practices that give the team high confidence in the correctness, maintainability, and reliability of our software."**

That distinction demonstrates engineering maturity.

---

## Short Interview Version

If the interviewer wants a quick answer:

> "I wouldn't start by forcing TDD. First, I'd understand why the team isn't adopting it — whether it's lack of knowledge, tooling, time pressure, or disagreement with the approach. I'd educate them, demonstrate TDD myself, and run a small pilot to show measurable value. Then I'd establish appropriate testing practices as part of our engineering standards and Definition of Done, supported by code reviews and CI.
>
> If someone still refuses after coaching and the expectations are clear, I'd have a one-on-one discussion. If there is a legitimate technical concern, I'd address it. Otherwise, engineering standards are team agreements rather than individual preferences, so continued non-compliance becomes an accountability issue.
>
> My approach is: **Understand → Educate → Demonstrate → Pilot → Measure → Standardize → Enforce.**"
