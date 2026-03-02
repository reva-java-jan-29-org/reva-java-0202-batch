# JPA Entity Relationships — Complete Overview

**Project:** `week5-01-jpa-entity-relationships-03012026`
**Framework:** Spring Boot 4 + Spring Data JPA + Hibernate
**Tests:** `@DataJpaTest` with H2 in-memory (no MySQL needed for tests)

---

## Quick Navigation

| # | File | Topic |
|---|---|---|
| 01 | [01-onetoone-unidirectional.md](01-onetoone-unidirectional.md) | `@OneToOne` — Employee → Profile |
| 02 | [02-onetoone-bidirectional.md](02-onetoone-bidirectional.md) | `@OneToOne` — Employee ↔ Profile |
| 03 | [03-onetomany-unidirectional.md](03-onetomany-unidirectional.md) | `@OneToMany` — Department → [Employee] |
| 04 | [04-onetomany-bidirectional.md](04-onetomany-bidirectional.md) | `@OneToMany` — Department ↔ [Employee] |
| 05 | [05-manytoone.md](05-manytoone.md) | `@ManyToOne` — [Employee] → Department |
| 06 | [06-manytomany-unidirectional.md](06-manytomany-unidirectional.md) | `@ManyToMany` — Student → [Course] |
| 07 | [07-manytomany-bidirectional.md](07-manytomany-bidirectional.md) | `@ManyToMany` — Student ↔ [Course] |

---

## All Relationships at a Glance

```
ONE-TO-ONE
──────────
  Unidirectional:   Employee ──────────► Profile
  Bidirectional:    Employee ◄─────────► Profile

ONE-TO-MANY
───────────
  Unidirectional:   Department ─────────► [Employee, ...]
  Bidirectional:    Department ◄──────────► [Employee, ...]
                    (same as ManyToOne bidirectional below)

MANY-TO-ONE
───────────
  Unidirectional:   [Employee, ...] ──────► Department
  (This + OneToMany inverse = OneToMany Bidirectional)

MANY-TO-MANY
────────────
  Unidirectional:   Student ────────────► [Course, ...]
  Bidirectional:    Student ◄────────────► [Course, ...]
```

---

## The Most Important Concept: Owning Side vs Inverse Side

Every JPA relationship has exactly **two sides**:

| Term | Meaning | How to Identify |
|---|---|---|
| **Owning Side** | Has the FK column. JPA reads this side to write FK. | Has `@JoinColumn` or `@JoinTable` |
| **Inverse Side** | No FK column. JPA ignores changes from this side. | Has `mappedBy` attribute |

```
@OneToOne/@ManyToOne:     side with @JoinColumn  = owning
@OneToMany:               side with mappedBy      = owning side is the @ManyToOne side
@ManyToMany:              side with @JoinTable    = owning
```

> **Rule:** Changes to the **inverse side** (mappedBy side) are **IGNORED** by JPA when writing to the DB.
> Always make changes from the **owning side**. Use helper methods to keep both sides in sync.

---

## FK Placement Rules

| Relationship | Where FK Lives |
|---|---|
| `@OneToOne` | Owning entity's table (one FK column) |
| `@ManyToOne` | The "many" entity's table (always!) |
| `@OneToMany` unidirectional (no @JoinColumn) | Separate JOIN TABLE |
| `@OneToMany` unidirectional (with @JoinColumn) | The "many" entity's table |
| `@OneToMany` bidirectional | The "many" entity's table (via `@ManyToOne` side) |
| `@ManyToMany` | Separate JOIN TABLE (always!) |

---

## Default Strategies — Master Table

| Aspect | `@OneToOne` | `@ManyToOne` | `@OneToMany` | `@ManyToMany` |
|---|---|---|---|---|
| **FK Location** | Owning table | Many-side table | Join table or many-side | Join table |
| **Fetch Default** | **EAGER** | **EAGER** | **LAZY** | **LAZY** |
| **Cascade Default** | NONE | NONE | NONE | NONE |
| **Optional Default** | true | true | — | — |

> **Memory trick for fetch defaults:**
> - "Single entity" relationships (`@OneToOne`, `@ManyToOne`) → **EAGER** by default
> - "Collection" relationships (`@OneToMany`, `@ManyToMany`) → **LAZY** by default

---

## Recommendations — Always Apply These

### 1. Always Change EAGER to LAZY

```java
// @OneToOne — change to LAZY
@OneToOne(fetch = FetchType.LAZY)

// @ManyToOne — MUST change to LAZY (default EAGER causes N+1 problem!)
@ManyToOne(fetch = FetchType.LAZY)

// @OneToMany — already LAZY, keep it
@OneToMany(fetch = FetchType.LAZY)  // default, keep as is

// @ManyToMany — already LAZY, keep it
@ManyToMany(fetch = FetchType.LAZY)  // default, keep as is
```

### 2. Always Use @JoinColumn for Explicit FK Names

```java
// ❌ Auto-generated: profile_profile_id (ugly!)
@OneToOne
private Profile profile;

// ✅ Explicit: fk_profile_id (clear and readable)
@OneToOne
@JoinColumn(name = "fk_profile_id", nullable = false, unique = true)
private Profile profile;
```

### 3. Always Use @JoinTable for @ManyToMany

```java
// ❌ Auto-generated table name (unpredictable)
@ManyToMany
private List<Course> courses;

// ✅ Explicit join table with named columns
@ManyToMany
@JoinTable(
    name = "student_courses",
    joinColumns = @JoinColumn(name = "student_id"),
    inverseJoinColumns = @JoinColumn(name = "course_id")
)
private List<Course> courses;
```

### 4. Never Use CascadeType.REMOVE on @ManyToMany

```java
// ❌ DANGEROUS — would delete all Courses when deleting Student!
@ManyToMany(cascade = CascadeType.ALL)

// ✅ Safe — only propagate PERSIST and MERGE
@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
```

### 5. Always Use Helper Methods for Bidirectional Relationships

```java
// In the parent entity (inverse side):
public void addEmployee(Employee emp) {
    this.employees.add(emp);   // inverse side
    emp.setDepartment(this);   // owning side ← critical!
}

// Always call: dept.addEmployee(emp);
// Never call:  dept.getEmployees().add(emp);  alone!
```

### 6. Always Save the Right Entity First (when cascade = NONE)

```java
// @ManyToOne with no cascade:
Department savedDept = departmentRepository.save(dept);  // Step 1: save parent first
Employee emp = new Employee("Alice", "Pune", savedDept); // Step 2: assign saved parent
employeeRepository.save(emp);                            // Step 3: save child
```

---

## Common Mistakes Summary

| Mistake | Effect | Fix |
|---|---|---|
| Only setting inverse side | FK not written to DB | Use helper method that sets both sides |
| Cascade = NONE, save child first | `TransientPropertyValueException` | Save parent first, then child |
| Cascade = ALL on @ManyToMany | Deletes shared entities | Use only `{PERSIST, MERGE}` |
| EAGER on @ManyToOne | N+1 query problem | Change to LAZY |
| EAGER on @OneToMany | Loads all children always | Keep LAZY (default) |
| No @JoinColumn | Ugly auto-generated FK names | Always use @JoinColumn |
| No @JoinTable | Ugly auto-generated join table | Always use @JoinTable for @ManyToMany |
| toString() on bidirectional | StackOverflowError | Don't include back-reference in toString() |
| @Data (Lombok) on bidirectional | StackOverflow in equals/hashCode | Use @Getter @Setter separately |

---

## N+1 Query Problem — Quick Reference

```
Problem:
  Load 100 employees → 1 query for employees
  Access employee.getDepartment() in loop → 100 queries for departments
  Total: 101 queries ← "N+1 Problem"

Solution 1 — JOIN FETCH:
  @Query("SELECT e FROM Employee e JOIN FETCH e.department")
  List<Employee> findAllWithDepartment();
  → Only 1 query total!

Solution 2 — Keep LAZY, batch when needed:
  @Query with JOIN FETCH only where you need all data at once
```

---

## Package Structure Reference

```
com.training
├── onetoone
│   ├── unidirectional           Employee ──► Profile
│   │   ├── Employee.java          @OneToOne @JoinColumn (owning)
│   │   ├── Profile.java           (plain entity, no annotation)
│   │   ├── EmployeeRepository.java
│   │   └── ProfileRepository.java
│   └── bidirectional            Employee ◄► Profile
│       ├── Employee.java          @OneToOne @JoinColumn (owning)
│       ├── Profile.java           @OneToOne(mappedBy="profile") (inverse)
│       ├── EmployeeRepository.java
│       └── ProfileRepository.java
│
├── onetomany
│   ├── unidirectional           Department ──► [Employee]
│   │   ├── Department.java        @OneToMany @JoinColumn (owning with JoinColumn)
│   │   ├── Employee.java          (plain entity)
│   │   ├── DepartmentRepository.java
│   │   └── EmployeeRepository.java
│   └── bidirectional            Department ◄► [Employee]
│       ├── Department.java        @OneToMany(mappedBy) (INVERSE)
│       ├── Employee.java          @ManyToOne @JoinColumn (OWNING)
│       ├── DepartmentRepository.java
│       └── EmployeeRepository.java
│
├── manytoone                    [Employee] ──► Department
│   ├── Employee.java              @ManyToOne @JoinColumn (owning)
│   ├── Department.java            (plain entity)
│   ├── EmployeeRepository.java
│   └── DepartmentRepository.java
│
└── manytomany
    ├── unidirectional           Student ──► [Course]
    │   ├── Student.java           @ManyToMany @JoinTable (owning)
    │   ├── Course.java            (plain entity)
    │   ├── StudentRepository.java
    │   └── CourseRepository.java
    └── bidirectional            Student ◄► [Course]
        ├── Student.java           @ManyToMany @JoinTable (OWNING)
        ├── Course.java            @ManyToMany(mappedBy="courses") (INVERSE)
        ├── StudentRepository.java
        └── CourseRepository.java
```
