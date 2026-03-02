# One-to-One Unidirectional Relationship

**Package:** `com.training.onetoone.unidirectional`
**Tables:** `oto_uni_employees`, `oto_uni_profiles`

---

## What Is It?

A **One-to-One** relationship means:
- One entity is associated with **exactly one** instance of another entity
- In **unidirectional**, only **one side knows about the other**

```
Employee ────────────► Profile
  (knows Profile)       (knows nothing about Employee)
```

---

## Real-World Analogy

> Each Employee has exactly one professional Profile.
> The Profile page alone doesn't know who the employee is — you look up an Employee to find their Profile.

---

## Class Structure

```java
// OWNING SIDE — Employee knows about Profile
class Employee {
    int empId;
    String name;

    @OneToOne
    Profile profile;        // Employee references Profile
}

// NON-REFERENCED SIDE — Profile knows nothing
class Profile {
    int profileId;
    String bio;
    // No reference to Employee here!
}
```

---

## Database Tables

```
oto_uni_employees                           oto_uni_profiles
─────────────────────────────────────────   ─────────────────────────────
 emp_id │ name    │ city  │ fk_profile_id   profile_id │ bio         │ linkedin_url
────────┼─────────┼───────┼──────────────   ───────────┼─────────────┼─────────────
   1    │ Alice   │ Pune  │      1      ──►      1     │ Alice's bio │ linkedin/alice
   2    │ Bob     │ Mumbai│      2      ──►      2     │ Bob's bio   │ linkedin/bob
```

> **FK lives in the employees table** — the side that "owns" the relationship holds the foreign key.

---

## Why is Employee the Owning Side?

In a **unidirectional** `@OneToOne`, the entity that **declares** the `@OneToOne` field is always the **owning side**.

The owning side:
1. Has the `@JoinColumn` annotation
2. Has the actual FK column in its database table
3. Controls when/what the FK value is written to the DB

---

## Annotations Deep Dive

### Without `@JoinColumn` (default — not recommended)

```java
@OneToOne
private Profile profile;
```

JPA auto-generates the FK column name as:
```
<entity_name>_<pk_column_name>  →  profile_profile_id
```
This is ugly and hard to understand. Always use `@JoinColumn`.

---

### With `@JoinColumn` (recommended)

```java
@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
@JoinColumn(name = "fk_profile_id", nullable = false, unique = true)
private Profile profile;
```

| Attribute | Value | Meaning |
|---|---|---|
| `name` | `"fk_profile_id"` | Explicit FK column name in employees table |
| `nullable = false` | `false` | Employee must always have a Profile |
| `unique = true` | `true` | Enforces true 1:1 at the DB level (DB constraint) |

---

## Default Strategy Table

| Aspect | Default | Recommended |
|---|---|---|
| Mapping Strategy | FK-based (`@JoinColumn`) | Keep FK-based |
| FK Ownership | Side with `@JoinColumn` (Employee) | Employee |
| Fetch Type | **EAGER** | Change to **LAZY** |
| Cascade | **NONE** | `CascadeType.ALL` if Profile is always created with Employee |
| Optional | `true` (FK can be null) | Set `false` if profile is mandatory |

---

## Fetch Types Explained

### EAGER (default — avoid for OneToOne)
```java
@OneToOne   // EAGER by default
private Profile profile;
```
When you load an Employee, JPA runs:
```sql
SELECT e.*, p.*
FROM oto_uni_employees e
LEFT JOIN oto_uni_profiles p ON e.fk_profile_id = p.profile_id
WHERE e.emp_id = ?
```
Profile is always loaded whether you need it or not.

### LAZY (recommended)
```java
@OneToOne(fetch = FetchType.LAZY)
private Profile profile;
```
When you load an Employee, JPA runs:
```sql
SELECT * FROM oto_uni_employees WHERE emp_id = ?
-- Profile NOT loaded yet
```
Profile only loaded when you call `employee.getProfile()`.

---

## Cascade Types

| Cascade | What Happens |
|---|---|
| `NONE` (default) | You must save Profile **first**, then Employee |
| `PERSIST` | Saving Employee also saves Profile |
| `MERGE` | Updating Employee also updates Profile |
| `REMOVE` | Deleting Employee also deletes Profile |
| `ALL` | All of the above |

### Without Cascade (NONE — the hard way)
```java
// Step 1: Save Profile first (must have an ID)
Profile savedProfile = profileRepository.save(new Profile("Alice's bio"));

// Step 2: Assign saved profile and save Employee
Employee alice = new Employee("Alice", "Pune", savedProfile);
employeeRepository.save(alice);
```

### With CascadeType.ALL (recommended — the easy way)
```java
Profile profile = new Profile("Alice's bio");    // NOT yet saved
Employee alice = new Employee("Alice", "Pune", profile);
employeeRepository.save(alice);                 // Saves BOTH via cascade!
```

---

## Code Example — Save

```java
// Create Profile (not saved yet)
Profile profile = new Profile("Alice's professional bio", "linkedin.com/alice");

// Create Employee with Profile
Employee employee = new Employee("Alice", "Pune", profile);

// Save Employee → cascade saves Profile too
Employee saved = employeeRepository.save(employee);

// Output:
// INSERT INTO oto_uni_profiles (bio, linkedin_url) VALUES (?, ?)
// INSERT INTO oto_uni_employees (name, city, fk_profile_id) VALUES (?, ?, ?)
```

---

## Code Example — Read

```java
Employee emp = employeeRepository.findById(1L).orElseThrow();

// Navigate: Employee → Profile (works!)
Profile profile = emp.getProfile();
System.out.println(profile.getBio());

// Navigate: Profile → Employee (CANNOT — unidirectional!)
// profile.getEmployee()  ← This method does NOT EXIST
```

---

## Code Example — Delete (with cascade)

```java
// Deleting Employee also deletes its Profile (CascadeType.ALL includes REMOVE)
employeeRepository.deleteById(empId);

// SQL output:
// DELETE FROM oto_uni_employees WHERE emp_id = ?
// DELETE FROM oto_uni_profiles WHERE profile_id = ?
```

---

## Common Mistakes

### Mistake 1 — Saving Employee before Profile (without cascade)
```java
// ❌ WRONG — Profile is transient (not saved), has no ID
Employee alice = new Employee("Alice", "Pune", new Profile("bio"));
employeeRepository.save(alice);
// Throws: TransientPropertyValueException
```

### Mistake 2 — No @JoinColumn (ugly default column name)
```java
// ❌ NOT RECOMMENDED
@OneToOne
private Profile profile;
// Creates column: profile_profile_id (auto-generated, unclear)
```

### Mistake 3 — Missing unique = true
```java
// ❌ Not a true 1:1 at DB level
@JoinColumn(name = "fk_profile_id")
// Two employees could point to the same profile!
```

---

## Full Entity Code (Simplified)

```java
// Employee.java — OWNING SIDE
@Entity
public class Employee {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long empId;
    private String name;
    private String city;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "fk_profile_id", nullable = false, unique = true)
    private Profile profile;
}

// Profile.java — PLAIN ENTITY (no @OneToOne here)
@Entity
public class Profile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;
    private String bio;
    private String linkedinUrl;
    // No reference to Employee — this is UNIDIRECTIONAL
}
```

---

## Summary

```
┌─────────────────────────────────────────────────────────────┐
│  ONE-TO-ONE UNIDIRECTIONAL — QUICK REFERENCE                │
├──────────────────────┬──────────────────────────────────────┤
│  Direction           │  Employee ──────► Profile            │
│  FK Location         │  employees table (fk_profile_id)     │
│  Owning Side         │  Employee (declares @OneToOne)       │
│  Non-Owning Side     │  Profile (no annotation)             │
│  Navigate Emp → Prof │  ✅ Yes   emp.getProfile()           │
│  Navigate Prof → Emp │  ❌ No    (Profile has no field)     │
│  Default Fetch       │  EAGER → change to LAZY              │
│  Default Cascade     │  NONE  → use ALL if tightly coupled  │
└──────────────────────┴──────────────────────────────────────┘
```
