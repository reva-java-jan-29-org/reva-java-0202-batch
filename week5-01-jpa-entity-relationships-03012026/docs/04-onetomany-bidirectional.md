# One-to-Many Bidirectional Relationship

**Package:** `com.training.onetomany.bidirectional`
**Tables:** `otm_bi_departments`, `otm_bi_employees`

---

## What Is It?

A **bidirectional One-to-Many** means both sides know about each other:
- Department has a list of Employees → `dept.getEmployees()`
- Employee knows its Department → `emp.getDepartment()`

```
Department ◄──────────────► [Employee, Employee, Employee]
  (has List<Employee>)          (each has a dept reference)
```

---

## Why Bidirectional is Preferred

| | Unidirectional `@OneToMany` | Bidirectional `@OneToMany` |
|---|---|---|
| Navigate Dept → Emp | ✅ | ✅ |
| Navigate Emp → Dept | ❌ | ✅ |
| FK Location | Employee table (with @JoinColumn) | Employee table |
| INSERT SQL | INSERT + extra UPDATE | INSERT with FK included ✅ |
| Performance | ⚠️ Extra UPDATE per row | ✅ Better |
| Complexity | Low | Medium |

---

## The #1 Rule: `@ManyToOne` is ALWAYS the Owning Side

In a **bidirectional @OneToMany**, there is a strict rule:

> **The `@ManyToOne` side (Employee) is ALWAYS the owning side.**
> **The `@OneToMany` side (Department) is ALWAYS the inverse side.**

This means:
- Employee has `@JoinColumn` → has the FK column → is the OWNER
- Department has `mappedBy` → no FK column → is the INVERSE

---

## Class Structure

```java
// OWNING SIDE — Employee has the FK
class Employee {
    int empId;
    String name;

    @ManyToOne
    @JoinColumn(name = "fk_dept_id")   // Employee holds the FK
    Department department;
}

// INVERSE SIDE — Department maps back via mappedBy
class Department {
    int deptId;
    String deptName;

    @OneToMany(mappedBy = "department")    // "department" = field name in Employee
    List<Employee> employees;
}
```

---

## Database Tables

```
otm_bi_departments          otm_bi_employees
────────────────────        ──────────────────────────────────────────
 dept_id │ dept_name         emp_id │ name  │ city   │ salary │ fk_dept_id
─────────┼──────────        ────────┼───────┼────────┼────────┼───────────
    1    │ Engineering           1  │ Alice │ Pune   │ 60000  │     1
    2    │ Marketing             2  │ Bob   │ Mumbai │ 55000  │     1
                                3  │ Carol │ Delhi  │ 70000  │     2

⚠️ departments table has NO FK column
✅ employees table holds fk_dept_id — Employee is the OWNER
```

---

## Annotations Deep Dive

### Employee (Owning Side)
```java
@ManyToOne(fetch = FetchType.LAZY)    // ← LAZY recommended (default is EAGER!)
@JoinColumn(name = "fk_dept_id", nullable = false)
private Department department;
```

- `@ManyToOne`: Many employees → one department
- `@JoinColumn`: This side has the FK column
- `fetch = LAZY`: Don't load department with every employee query

### Department (Inverse Side)
```java
@OneToMany(
    mappedBy = "department",      // field name in Employee class
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.LAZY        // default for @OneToMany, keep LAZY
)
private List<Employee> employees = new ArrayList<>();
```

- `mappedBy = "department"`: Employee.department owns the FK
- Department has NO `@JoinColumn` — no FK in departments table!

---

## Understanding `mappedBy` in @OneToMany

```
mappedBy = "department"
              ↑
              This is the FIELD NAME in Employee.java
              (not the column name "fk_dept_id")

Employee.java:
    @ManyToOne
    @JoinColumn(name = "fk_dept_id")
    private Department department;    ← This field is named "department"
                                        ← mappedBy = "department" refers to THIS
```

---

## SQL Comparison — Why Bidirectional is Better

### Unidirectional @OneToMany SQL (extra UPDATE problem)
```sql
-- JPA inserts employees WITHOUT FK first (no department field on Employee)
INSERT INTO employees (name, city) VALUES ('Alice', 'Pune')   -- no fk_dept_id!
INSERT INTO employees (name, city) VALUES ('Bob', 'Mumbai')

-- Then updates FK separately (extra round-trips!)
UPDATE employees SET fk_dept_id = 1 WHERE emp_id = 1
UPDATE employees SET fk_dept_id = 1 WHERE emp_id = 2
```

### Bidirectional @OneToMany SQL (FK in INSERT directly)
```sql
-- JPA can include FK in the INSERT because Employee has department field
INSERT INTO departments (dept_name) VALUES ('Engineering')
INSERT INTO employees (name, city, fk_dept_id) VALUES ('Alice', 'Pune', 1)   -- ✅ FK included!
INSERT INTO employees (name, city, fk_dept_id) VALUES ('Bob', 'Mumbai', 1)   -- ✅ FK included!
-- No extra UPDATE needed!
```

---

## The Helper Method Pattern (Best Practice)

Always use a helper method to set both sides of the relationship:

```java
// In Department.java
public void addEmployee(Employee employee) {
    this.employees.add(employee);       // Step 1: add to inverse side list
    employee.setDepartment(this);       // Step 2: set owning side FK reference
}

public void removeEmployee(Employee employee) {
    this.employees.remove(employee);
    employee.setDepartment(null);
}
```

### Why the Helper is Essential

```java
// WRONG WAY — only set inverse side
dept.getEmployees().add(alice);   // ← inverse side only
// alice.setDepartment(dept);     ← SKIPPED!
departmentRepository.save(dept);
// Result: fk_dept_id is NULL in alice's row! (owning side never set)

// CORRECT WAY — use helper method
dept.addEmployee(alice);          // ← sets BOTH sides
departmentRepository.save(dept);
// Result: fk_dept_id = 1 correctly saved in alice's row ✅
```

---

## Code Examples

### Save with cascade
```java
Department dept = new Department("Engineering", "Bangalore");

// Use helper! Sets emp.setDepartment(dept) internally
dept.addEmployee(new Employee("Alice", "Pune", 60000.0));
dept.addEmployee(new Employee("Bob", "Mumbai", 55000.0));

// cascade = ALL saves employees automatically
Department saved = departmentRepository.save(dept);
```

### Bidirectional navigation
```java
// Load a department → get its employees (inverse side)
Department dept = departmentRepository.findById(1L).orElseThrow();
List<Employee> employees = dept.getEmployees();   // LAZY load triggered

// Load an employee → get its department (owning side)
Employee emp = employeeRepository.findById(1L).orElseThrow();
Department empDept = emp.getDepartment();          // LAZY load triggered
System.out.println(empDept.getDeptName());         // "Engineering"
```

### Transfer employee to different department
```java
Employee alice = employeeRepository.findById(1L).orElseThrow();
Department newDept = departmentRepository.findById(2L).orElseThrow();

// Load the old department to update its list
Department oldDept = alice.getDepartment();
oldDept.removeEmployee(alice);       // helper: removes from list AND sets dept=null

newDept.addEmployee(alice);          // helper: adds to list AND sets dept=newDept
departmentRepository.save(newDept);  // cascade saves the change
// SQL: UPDATE employees SET fk_dept_id = 2 WHERE emp_id = 1
```

### Find employees by department
```java
// Spring Data JPA can query using the relationship field
List<Employee> engineers = employeeRepository.findByDepartment(engineeringDept);
// OR by department ID (no need to load the department first):
List<Employee> engineers = employeeRepository.findByDepartmentDeptId(1L);
```

### JOIN FETCH — avoid LazyInitializationException
```java
// Problem: if transaction is closed before accessing employees,
// you get LazyInitializationException

// Solution: use JOIN FETCH in JPQL to load eagerly in one query
// In DepartmentRepository:
@Query("SELECT d FROM OtmBiDepartment d JOIN FETCH d.employees WHERE d.deptId = :id")
Optional<Department> findByIdWithEmployees(Long id);

// Usage:
Department deptWithEmployees = departmentRepository.findByIdWithEmployees(1L).orElseThrow();
// employees already loaded — no lazy issue
```

---

## orphanRemoval vs CascadeType.REMOVE

```java
@OneToMany(mappedBy = "department",
    cascade = CascadeType.ALL,       // includes REMOVE
    orphanRemoval = true)

// CascadeType.REMOVE fires when Department is DELETED:
departmentRepository.delete(dept);   // deletes dept AND all employees

// orphanRemoval fires when Employee is REMOVED FROM THE LIST:
dept.removeEmployee(alice);          // alice deleted from DB
departmentRepository.save(dept);     // ← triggers orphanRemoval
```

---

## Default Strategy Table

| Aspect | @OneToMany (Department) | @ManyToOne (Employee) |
|---|---|---|
| Role | INVERSE side | OWNING side |
| Has `@JoinColumn` | ❌ No | ✅ Yes |
| Has `mappedBy` | ✅ Yes | ❌ No |
| FK column | ❌ Not in dept table | ✅ In employees table |
| Default Fetch | **LAZY** (keep it) | **EAGER** (change to LAZY!) |
| Default Cascade | NONE | NONE |
| Writes FK to DB | ❌ IGNORED | ✅ Yes |

---

## Common Mistakes

### Mistake 1 — Forgetting `mappedBy` on Department
```java
// ❌ Without mappedBy, JPA treats as TWO separate relationships!
// Creates a JOIN TABLE in addition to the FK column in employees
@OneToMany
private List<Employee> employees;

// ✅ Correct
@OneToMany(mappedBy = "department")
private List<Employee> employees;
```

### Mistake 2 — @ManyToOne with EAGER fetch (default!)
```java
// ❌ Default is EAGER — loads department with EVERY employee query!
@ManyToOne
@JoinColumn(name = "fk_dept_id")
private Department department;

// ✅ Always set LAZY
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "fk_dept_id")
private Department department;
```

### Mistake 3 — Setting only the inverse side
```java
// ❌ dept.employees is the INVERSE side — changes are IGNORED by JPA
dept.getEmployees().add(emp);    // NO FK written to DB!

// ✅ Must set the OWNING side (emp.department)
dept.addEmployee(emp);           // helper sets both sides
```

---

## Summary

```
┌─────────────────────────────────────────────────────────────────────┐
│  ONE-TO-MANY BIDIRECTIONAL — QUICK REFERENCE                        │
├──────────────────────────────┬──────────────────────────────────────┤
│  Direction                   │  Department ◄───────► [Employee, ...] │
│  FK Location                 │  employees table ONLY (fk_dept_id)   │
│  Owning Side (@ManyToOne)    │  Employee (has @JoinColumn)          │
│  Inverse Side (@OneToMany)   │  Department (has mappedBy)           │
│  Navigate Dept → Emp         │  ✅  dept.getEmployees()             │
│  Navigate Emp → Dept         │  ✅  emp.getDepartment()             │
│  Who writes FK to DB         │  Only Employee (owning side)         │
│  Helper method               │  ✅ Required — dept.addEmployee()    │
│  Default Fetch @OneToMany    │  LAZY (keep LAZY)                    │
│  Default Fetch @ManyToOne    │  EAGER → change to LAZY!             │
│  SQL vs Unidirectional       │  ✅ More efficient (FK in INSERT)    │
└──────────────────────────────┴──────────────────────────────────────┘
```
