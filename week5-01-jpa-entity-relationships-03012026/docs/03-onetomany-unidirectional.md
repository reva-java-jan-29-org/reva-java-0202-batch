# One-to-Many Unidirectional Relationship

**Package:** `com.training.onetomany.unidirectional`
**Tables:** `otm_uni_departments`, `otm_uni_employees`

---

## What Is It?

A **One-to-Many** relationship means:
- ONE entity is associated with **many** instances of another entity
- In **unidirectional**, only **one side knows about the other**

```
Department ─────────────► [Employee, Employee, Employee, ...]
  (has a List<Employee>)     (knows nothing about Department)
```

---

## Real-World Analogy

> One Department has many Employees.
> The Employee records alone don't know which department they belong to — you look up the Department to find its employees.

---

## Class Structure

```java
// OWNING SIDE — Department has a list of Employees
class Department {
    int deptId;
    String deptName;

    @OneToMany
    @JoinColumn(name = "fk_dept_id")
    List<Employee> employees;       // Department references Employees
}

// NON-REFERENCED SIDE — Employee knows nothing
class Employee {
    int empId;
    String name;
    // No reference to Department!
}
```

---

## Two Mapping Options for Unidirectional @OneToMany

This is the most important decision when using unidirectional @OneToMany.

### Option A — Without `@JoinColumn` (NOT recommended)

```java
@OneToMany
private List<Employee> employees;
```

JPA creates a **separate JOIN TABLE**:

```
otm_uni_dept_employees (join table)
──────────────────────────────
 department_dept_id  │  employees_emp_id
─────────────────────┼──────────────────
        1            │         1
        1            │         2
        2            │         3
```

Problems:
- Extra table in the database
- Extra SQL for every operation
- Not intuitive for a simple parent-child relationship

---

### Option B — With `@JoinColumn` (RECOMMENDED)

```java
@OneToMany
@JoinColumn(name = "fk_dept_id")
private List<Employee> employees;
```

JPA puts the FK in the **Employee table** — no join table!

```
otm_uni_departments          otm_uni_employees
────────────────────         ──────────────────────────────────────
 dept_id │ dept_name          emp_id │ name  │ city   │ fk_dept_id
─────────┼──────────         ────────┼───────┼────────┼───────────
    1    │ Engineering            1  │ Alice │ Pune   │     1
    2    │ Marketing              2  │ Bob   │ Mumbai │     1
                                 3  │ Carol │ Delhi  │     2
```

---

## The Hidden Cost of Unidirectional @OneToMany

Even with `@JoinColumn`, the SQL is **less efficient** than bidirectional.

When saving a Department with 3 new employees:

```sql
-- Step 1: Insert employees WITHOUT the FK (Employee has no dept reference!)
INSERT INTO otm_uni_employees (name, city, salary) VALUES ('Alice', 'Pune', 60000)
INSERT INTO otm_uni_employees (name, city, salary) VALUES ('Bob', 'Mumbai', 55000)
INSERT INTO otm_uni_employees (name, city, salary) VALUES ('Carol', 'Delhi', 70000)

-- Step 2: Insert department
INSERT INTO otm_uni_departments (dept_name, location) VALUES ('Engineering', 'Bangalore')

-- Step 3: UPDATE employees to set FK (extra round-trip!)
UPDATE otm_uni_employees SET fk_dept_id = 1 WHERE emp_id = 1
UPDATE otm_uni_employees SET fk_dept_id = 1 WHERE emp_id = 2
UPDATE otm_uni_employees SET fk_dept_id = 1 WHERE emp_id = 3
```

> **3 extra UPDATE statements** because Employee has no `department` field — JPA can't include FK in the INSERT.
>
> Bidirectional @OneToMany avoids this (see `04-onetomany-bidirectional.md`).

---

## Annotations Deep Dive

```java
@OneToMany(
    cascade = CascadeType.ALL,       // PERSIST, MERGE, REMOVE, REFRESH, DETACH
    orphanRemoval = true,            // delete employee when removed from list
    fetch = FetchType.LAZY           // employees not loaded until accessed (default)
)
@JoinColumn(name = "fk_dept_id", nullable = false)
private List<Employee> employees = new ArrayList<>();
```

### Cascade Types (for @OneToMany)

| Cascade | What It Does |
|---|---|
| `PERSIST` | Save dept → saves all employees in the list |
| `MERGE` | Update dept → updates all employees |
| `REMOVE` | Delete dept → deletes all employees |
| `ALL` | All of the above |
| `NONE` (default) | Must save each employee separately |

---

## orphanRemoval vs CascadeType.REMOVE

These are related but different:

| | `orphanRemoval = true` | `CascadeType.REMOVE` |
|---|---|---|
| Trigger | Employee **removed from the list** | Department is **deleted** |
| Effect | Employee is deleted from DB | All employees are deleted from DB |
| When useful | "Child lives only through parent" | Cleanup when parent is gone |

```java
// orphanRemoval in action
Department dept = departmentRepository.findById(1L).get();
dept.getEmployees().remove(0);      // Remove first employee from list
departmentRepository.save(dept);    // ← orphanRemoval deletes that employee from DB!

// CascadeType.REMOVE in action
departmentRepository.delete(dept);  // ← Deletes dept AND all its employees
```

---

## Fetch Types

### LAZY (default for @OneToMany — keep this!)
```java
@OneToMany(fetch = FetchType.LAZY)
```
Employees are **not loaded** when Department is fetched.
```sql
SELECT * FROM otm_uni_departments WHERE dept_id = ?  -- no employee JOIN
```
Employees only loaded when `dept.getEmployees()` is called.

### EAGER (avoid for @OneToMany!)
```java
@OneToMany(fetch = FetchType.EAGER)  -- ⚠️ Dangerous!
```
Every time you load a Department, ALL its employees are also loaded.
If you have 100 departments each with 50 employees → 5000 employees loaded into memory!

---

## Code Examples

### Save (with cascade)
```java
Department dept = new Department("Engineering", "Bangalore");

Employee alice = new Employee("Alice", "Pune", 60000.0);
Employee bob   = new Employee("Bob", "Mumbai", 55000.0);

dept.addEmployee(alice);    // dept.employees.add(alice)
dept.addEmployee(bob);

// Save department → cascade saves all employees
Department saved = departmentRepository.save(dept);
```

### Read
```java
Department dept = departmentRepository.findById(1L).orElseThrow();

// Navigate: Department → Employees (one-way)
List<Employee> employees = dept.getEmployees();   // triggers LAZY load
employees.forEach(System.out::println);

// Cannot navigate: Employee → Department (unidirectional!)
// employees.get(0).getDepartment()  ← method does NOT exist
```

### Remove employee (orphanRemoval)
```java
Department dept = departmentRepository.findById(1L).orElseThrow();
Employee toRemove = dept.getEmployees().get(0);
dept.getEmployees().remove(toRemove);
departmentRepository.save(dept);
// ← toRemove is now deleted from the DB (orphanRemoval = true)
```

---

## Default Strategy Table

| Aspect | Default | Recommended |
|---|---|---|
| Mapping Strategy | JOIN TABLE (ugly) | Use `@JoinColumn` to avoid join table |
| FK Location | Join table column | Employee table column (with `@JoinColumn`) |
| Fetch Type | **LAZY** | Keep LAZY (EAGER causes performance issues) |
| Cascade | **NONE** | Use `ALL` if employees are managed through dept |
| orphanRemoval | `false` | Set `true` for "child belongs to parent" scenarios |

---

## Common Mistakes

### Mistake 1 — No `@JoinColumn` (creates unnecessary join table)
```java
// ❌ Creates a join table: otm_uni_dept_employees
@OneToMany
private List<Employee> employees;
```

### Mistake 2 — Not initializing the List
```java
// ❌ NullPointerException when calling dept.addEmployee()
private List<Employee> employees;

// ✅ Always initialize with empty ArrayList
private List<Employee> employees = new ArrayList<>();
```

### Mistake 3 — Using EAGER fetch
```java
// ❌ Loads ALL employees every time ANY department is loaded
@OneToMany(fetch = FetchType.EAGER)
private List<Employee> employees;
```

---

## Summary

```
┌─────────────────────────────────────────────────────────────────┐
│  ONE-TO-MANY UNIDIRECTIONAL — QUICK REFERENCE                   │
├───────────────────────────┬─────────────────────────────────────┤
│  Direction                │  Department ──────► [Employee, ...]  │
│  FK Location              │  employees table (fk_dept_id)        │
│  Owning Side              │  Department (declares @OneToMany)    │
│  @JoinColumn              │  Required! (avoids join table)       │
│  Navigate Dept → Emp      │  ✅  dept.getEmployees()            │
│  Navigate Emp → Dept      │  ❌  Not possible (unidirectional)  │
│  Default Fetch            │  LAZY (keep as LAZY)                 │
│  SQL Efficiency           │  ⚠️  Extra UPDATEs (see note above) │
│  Recommendation           │  Prefer BIDIRECTIONAL for better SQL │
└───────────────────────────┴─────────────────────────────────────┘
```
