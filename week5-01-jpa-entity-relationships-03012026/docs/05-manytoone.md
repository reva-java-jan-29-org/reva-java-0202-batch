# Many-to-One Relationship

**Package:** `com.training.manytoone`
**Tables:** `mto_employees`, `mto_departments`

---

## What Is It?

A **Many-to-One** relationship means:
- **MANY** instances of one entity are associated with **ONE** instance of another entity
- In **unidirectional**, only the "many" side knows about the "one" side

```
[Employee, Employee, Employee] ──────────► Department
  (each knows their dept)                    (knows nothing about employees)
```

---

## Real-World Analogy

> Many Employees work in one Department.
> Each employee knows their department (can look it up).
> But the Department record itself doesn't maintain a list of employees.

---

## Why @ManyToOne is the Most Fundamental Relationship

Understanding `@ManyToOne` is the key to understanding ALL other relationships:

```
@ManyToOne alone              = ManyToOne Unidirectional
@ManyToOne + @OneToMany(mappedBy) = OneToMany Bidirectional
@ManyToOne on both sides      = self-referencing (tree structure)
```

> **`@ManyToOne` ALWAYS owns the FK. No exceptions.**

---

## Class Structure

```java
// OWNING SIDE — Employee references Department
class Employee {
    int empId;
    String name;

    @ManyToOne
    @JoinColumn(name = "fk_dept_id")   // Employee holds the FK
    Department department;
}

// TARGET SIDE — Department is a plain entity
class Department {
    int deptId;
    String deptName;
    // No reference to employees (unidirectional)
}
```

---

## Database Tables

```
mto_employees                                   mto_departments
────────────────────────────────────────────    ─────────────────────────────────
 emp_id │ name  │ city       │ fk_dept_id        dept_id │ dept_name   │ location
────────┼───────┼────────────┼───────────        ────────┼─────────────┼──────────
   1    │ Alice │ Pune       │     1         ──►    1    │ Engineering │ Bangalore
   2    │ Bob   │ Mumbai     │     1         ──►    1    │ Engineering │ Bangalore
   3    │ Carol │ Delhi      │     2         ──►    2    │ Marketing   │ Mumbai

FK (fk_dept_id) is in the employees table
Multiple rows in employees can point to the same department
```

---

## Save Order — Critical Rule

Since `cascade = NONE` by default on `@ManyToOne`:

> **You MUST save Department FIRST, then Employee.**

Employee needs the Department to have a DB-generated ID before you can link them.

```java
// ✅ CORRECT ORDER

// Step 1: Save Department first (gets an ID from DB)
Department engineering = new Department("Engineering", "Bangalore");
Department savedDept = departmentRepository.save(engineering);
// savedDept now has deptId = 1

// Step 2: Create Employee with the SAVED department (has ID)
Employee alice = new Employee("Alice", "Pune", 60000.0, savedDept);
employeeRepository.save(alice);
// alice.fk_dept_id = 1 ← correctly set
```

```java
// ❌ WRONG ORDER

// Save Employee with an UNSAVED (transient) Department
Department dept = new Department("Engineering", "Bangalore");   // no ID yet!
Employee alice = new Employee("Alice", "Pune", 60000.0, dept);
employeeRepository.save(alice);
// Throws: TransientPropertyValueException:
// object references an unsaved transient instance — save the transient instance before flushing
```

---

## Annotations Deep Dive

```java
@ManyToOne(fetch = FetchType.LAZY)       // ← Change from EAGER to LAZY!
@JoinColumn(name = "fk_dept_id", nullable = false)
private Department department;
```

| Attribute | Default | Recommended |
|---|---|---|
| `fetch` | `EAGER` | Change to **`LAZY`** |
| `cascade` | `NONE` | Keep `NONE` (dept is independent) |
| `optional` | `true` | Set `false` if dept is always required |
| `@JoinColumn name` | `<entity>_<pk>` | Explicit name e.g. `fk_dept_id` |

---

## Default Fetch Type — EAGER (and why it's dangerous)

The default fetch for `@ManyToOne` is **EAGER** — unlike `@OneToMany` which is LAZY.

```java
// If fetch = EAGER (default):
Employee alice = employeeRepository.findById(1L).orElseThrow();
// SQL: SELECT e.*, d.* FROM employees e JOIN departments d ON e.fk_dept_id = d.dept_id

// Department loaded even if you never use alice.getDepartment()!
```

**Always change to LAZY:**
```java
@ManyToOne(fetch = FetchType.LAZY)
```

---

## The N+1 Query Problem

This is a critical performance issue caused by EAGER fetch (or naive LAZY usage in a loop).

### The Problem

```java
// Get all employees (N employees found)
List<Employee> all = employeeRepository.findAll();
// SQL: SELECT * FROM mto_employees                    ← 1 query

// Loop and access each employee's department
for (Employee emp : all) {
    System.out.println(emp.getDepartment().getDeptName());
    // SQL: SELECT * FROM mto_departments WHERE dept_id = ?  ← N queries (one per employee!)
}
// Total: 1 + N queries = "N+1 Problem" ❌
```

If you have 1000 employees → **1001 SQL queries!**

### Fix 1 — JOIN FETCH in JPQL (recommended for bulk loads)

```java
// In EmployeeRepository:
@Query("SELECT e FROM MtoEmployee e JOIN FETCH e.department")
List<Employee> findAllWithDepartment();

// SQL: SELECT e.*, d.* FROM employees e JOIN departments d ON ...
// ← Just ONE query that loads everything! ✅
```

### Fix 2 — Keep `@ManyToOne(fetch = FetchType.LAZY)` and load only when needed

```java
// LAZY: department not loaded until accessed
Employee emp = employeeRepository.findById(1L).orElseThrow();
// Only ONE query: SELECT * FROM employees WHERE emp_id = 1

// Department loaded only when you need it
String deptName = emp.getDepartment().getDeptName();   // triggers one more query
```

---

## Code Examples

### Find employees by department name (Spring Data traversal)
```java
// Spring Data JPA auto-generates the JOIN query
List<Employee> engineers = employeeRepository.findByDepartmentDeptName("Engineering");
// SQL: SELECT * FROM employees e JOIN departments d ON e.fk_dept_id = d.dept_id
//      WHERE d.dept_name = 'Engineering'
```

### Transfer employee to different department
```java
Employee alice = employeeRepository.findById(1L).orElseThrow();
Department newDept = departmentRepository.findById(2L).orElseThrow();

alice.setDepartment(newDept);       // Update FK reference (owning side)
employeeRepository.save(alice);
// SQL: UPDATE mto_employees SET fk_dept_id = 2 WHERE emp_id = 1
```

### Find all employees in same department
```java
// By department object
List<Employee> deptMembers = employeeRepository.findByDepartment(savedDept);

// By department ID (no need to load dept object first)
List<Employee> deptMembers = employeeRepository.findByDepartmentDeptId(1L);
```

---

## Default Strategy Table

| Aspect | Default | Recommendation |
|---|---|---|
| FK Location | employees table (always) | — |
| Owning Side | Employee (always for @ManyToOne) | — |
| Fetch Type | **EAGER** | Change to **LAZY** |
| Cascade | **NONE** | Keep NONE (save dept before emp) |
| Optional | `true` | Set `false` if dept always required |
| FK Column Name | `<entity>_<pk>` (ugly) | Use `@JoinColumn` for explicit name |

---

## @ManyToOne vs @OneToMany — What's the Difference?

```
@ManyToOne  = from the "many" side  →  always owns the FK
@OneToMany  = from the "one" side   →  always inverse side (in bidirectional)

Employee (@ManyToOne) ──── Department
    Many employees         One department
    emp has fk_dept_id     dept has no FK
```

In **unidirectional ManyToOne** → only Employee knows about Department.
In **bidirectional OneToMany** → Department ALSO gets a `@OneToMany(mappedBy)` list.

---

## Common Mistakes

### Mistake 1 — Not changing EAGER to LAZY
```java
// ❌ Default EAGER causes unnecessary joins
@ManyToOne
@JoinColumn(name = "fk_dept_id")
private Department department;

// ✅ Always set LAZY
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "fk_dept_id")
private Department department;
```

### Mistake 2 — Save order violation
```java
// ❌ Department not saved yet (transient)
employeeRepository.save(new Employee("Alice", "Pune", 60000.0, new Department("Eng")));

// ✅ Save department first
Department saved = departmentRepository.save(new Department("Eng", "Bangalore"));
employeeRepository.save(new Employee("Alice", "Pune", 60000.0, saved));
```

### Mistake 3 — N+1 without JOIN FETCH
```java
// ❌ Causes N+1 queries
employeeRepository.findAll().forEach(e -> System.out.println(e.getDepartment().getDeptName()));

// ✅ Use JOIN FETCH
employeeRepository.findAllWithDepartment().forEach(e -> System.out.println(e.getDepartment().getDeptName()));
```

---

## Summary

```
┌─────────────────────────────────────────────────────────────────┐
│  MANY-TO-ONE — QUICK REFERENCE                                  │
├──────────────────────────┬──────────────────────────────────────┤
│  Direction               │  [Employee, ...] ──────► Department  │
│  FK Location             │  employees table (fk_dept_id)        │
│  Owning Side             │  Employee (ALWAYS for @ManyToOne)    │
│  Has @JoinColumn         │  ✅ Yes — on Employee                │
│  Navigate Emp → Dept     │  ✅  emp.getDepartment()             │
│  Navigate Dept → Emp     │  ❌  Not possible (unidirectional)   │
│  Default Fetch           │  EAGER → change to LAZY!             │
│  Default Cascade         │  NONE — save dept before emp         │
│  N+1 Fix                 │  JOIN FETCH in @Query                │
│  Fundamentals            │  Base of ALL other relationships      │
└──────────────────────────┴──────────────────────────────────────┘
```
