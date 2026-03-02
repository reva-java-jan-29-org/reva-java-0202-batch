# One-to-One Bidirectional Relationship

**Package:** `com.training.onetoone.bidirectional`
**Tables:** `oto_bi_employees`, `oto_bi_profiles`

---

## What Is It?

A **bidirectional** One-to-One means both sides know about each other.
You can navigate in **both directions** from Java code.

```
Employee ◄────────────► Profile
  (knows Profile)         (knows Employee)
```

> **Important:** Bidirectional does NOT mean two FK columns.
> There is still **only ONE FK column** — on the **owning side** (Employee).

---

## Class Structure

```java
// OWNING SIDE — has @JoinColumn, has the FK
class Employee {
    int empId;
    String name;

    @OneToOne
    @JoinColumn(name = "fk_profile_id")
    Profile profile;            // owns the FK
}

// INVERSE SIDE — has mappedBy, NO FK column
class Profile {
    int profileId;
    String bio;

    @OneToOne(mappedBy = "profile")   // "profile" is the field name in Employee
    Employee employee;                // back-reference, no FK here
}
```

---

## Database Tables

```
oto_bi_employees                           oto_bi_profiles
───────────────────────────────────────    ─────────────────────────────
 emp_id │ name    │ city  │ fk_profile_id   profile_id │ bio         │ linkedin_url
────────┼─────────┼───────┼──────────────   ───────────┼─────────────┼─────────────
   1    │ Alice   │ Pune  │      1      ──►      1     │ Alice's bio │ linkedin/alice
   2    │ Bob     │ Mumbai│      2      ──►      2     │ Bob's bio   │ linkedin/bob

⚠️ oto_bi_profiles has NO fk column — FK is ONLY in employees table!
```

---

## Owning vs Inverse Side

This is the **most important concept** in bidirectional relationships.

| | Owning Side (Employee) | Inverse Side (Profile) |
|---|---|---|
| Annotation | `@OneToOne` + `@JoinColumn` | `@OneToOne(mappedBy = "profile")` |
| FK column in DB | ✅ Yes (`fk_profile_id`) | ❌ No |
| JPA writes FK | ✅ Yes — from this side | ❌ No — changes IGNORED |
| Navigate to other | `employee.getProfile()` | `profile.getEmployee()` |

---

## Understanding `mappedBy`

`mappedBy = "profile"` means:

> "The field named **`profile`** in the **Employee** class owns this relationship."

Key points:
- `mappedBy` value = **field name** in the owning class (NOT the column name!)
- It tells JPA: "Don't create a FK here. Read the relationship from the other side."
- Without `mappedBy`, JPA would create **TWO separate relationships** with FK in both tables!

```java
// In Profile.java
@OneToOne(mappedBy = "profile")    // "profile" = field name in Employee
private Employee employee;

// In Employee.java
@OneToOne
@JoinColumn(name = "fk_profile_id")
private Profile profile;           // ← This field is what mappedBy refers to
```

---

## The #1 Rule: Always Set BOTH Sides

Since both sides hold a reference in memory, you must keep them consistent.

### Wrong Way — Only Set Owning Side
```java
Employee emp = new Employee("Alice", "Pune");
Profile profile = new Profile("Alice's bio");

emp.setProfile(profile);           // ✅ Owning side — FK will be written to DB
// profile.setEmployee(emp);       // ❌ NOT set — in-memory state is STALE!

employeeRepository.save(emp);

// In the SAME session:
profile.getEmployee();             // returns null! (stale in-memory)

// After re-loading from DB: works correctly (JPA loads from DB)
```

### Correct Way — Set BOTH Sides (use a helper method)
```java
// Helper method in Employee.java
public void setProfileWithSync(Profile profile) {
    this.profile = profile;
    if (profile != null) {
        profile.setEmployee(this);   // Set the back-reference too!
    }
}

// Usage
Employee emp = new Employee("Alice", "Pune");
Profile profile = new Profile("Alice's bio");

emp.setProfileWithSync(profile);   // ✅ Sets BOTH sides at once
employeeRepository.save(emp);

// Now both work immediately (no reload needed):
emp.getProfile();         // ✅ Alice's bio
profile.getEmployee();    // ✅ Alice
```

---

## Cascade Behavior

Same as unidirectional — controlled from the **owning side** (Employee):

```java
@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
@JoinColumn(name = "fk_profile_id", nullable = false, unique = true)
private Profile profile;
```

Saving Employee cascades to Profile.
Deleting Employee cascades to Profile.
Profile side has NO cascade (it is not the owner).

---

## CRITICAL: Inverse Side Changes Are Ignored by JPA

This is a common source of bugs:

```java
// Load profile from DB
Profile profile = profileRepository.findById(1L).get();

// Try to change the relationship from INVERSE side
profile.setEmployee(anotherEmployee);       // ❌ IGNORED BY JPA!
profileRepository.save(profile);            // No FK change in DB!

// The correct way — change from OWNING side:
employee.setProfileWithSync(newProfile);    // ✅ FK will be updated
employeeRepository.save(employee);
```

> **Rule:** Only the **owning side** (`@JoinColumn` side = Employee) writes the FK.
> Changing the inverse side (`mappedBy` side = Profile) has **no effect on the database**.

---

## Code Example — Bidirectional Navigation

```java
// Save
Employee alice = new Employee("Alice", "Pune");
Profile profile = new Profile("Alice's bio", "linkedin.com/alice");
alice.setProfileWithSync(profile);
employeeRepository.save(alice);

// Navigate Employee → Profile (owning side)
Employee emp = employeeRepository.findById(1L).orElseThrow();
Profile p = emp.getProfile();
System.out.println(p.getBio());           // "Alice's bio"

// Navigate Profile → Employee (inverse side)
Profile prof = profileRepository.findById(1L).orElseThrow();
Employee e = prof.getEmployee();
System.out.println(e.getName());          // "Alice"
```

---

## Infinite Recursion in toString() / JSON

When both sides reference each other, calling `toString()` creates an infinite loop:
```
Employee.toString() → calls Profile.toString()
  Profile.toString() → calls Employee.toString()
    Employee.toString() → calls Profile.toString()
      ... StackOverflowError!
```

### Fix — Exclude the back-reference from toString()
```java
// In Employee.java
@Override
public String toString() {
    // Do NOT include profile.toString() here!
    return "Employee{empId=" + empId + ", name=" + name + "}";
}

// In Profile.java
@Override
public String toString() {
    // Do NOT include employee.toString() here!
    return "Profile{profileId=" + profileId + ", bio=" + bio + "}";
}
```

> Same issue applies with JSON serialization (use `@JsonManagedReference` / `@JsonBackReference` in Spring REST APIs).

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

    // BEST PRACTICE — helper to sync both sides
    public void setProfileWithSync(Profile profile) {
        this.profile = profile;
        if (profile != null) profile.setEmployee(this);
    }
}

// Profile.java — INVERSE SIDE
@Entity
public class Profile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;
    private String bio;
    private String linkedinUrl;

    @OneToOne(mappedBy = "profile")   // mappedBy = field name in Employee
    private Employee employee;        // no @JoinColumn here — NOT the owner
}
```

---

## Uni vs Bi — Side-by-Side Comparison

| | Unidirectional | Bidirectional |
|---|---|---|
| `@OneToOne` on Employee | ✅ | ✅ |
| `@OneToOne` on Profile | ❌ | ✅ (with `mappedBy`) |
| FK in employees table | ✅ | ✅ |
| FK in profiles table | ❌ | ❌ |
| `emp.getProfile()` | ✅ | ✅ |
| `profile.getEmployee()` | ❌ | ✅ |
| Need to sync both sides | ❌ N/A | ✅ Required |
| Extra complexity | Low | Medium |

---

## Summary

```
┌─────────────────────────────────────────────────────────────────┐
│  ONE-TO-ONE BIDIRECTIONAL — QUICK REFERENCE                     │
├───────────────────────────┬─────────────────────────────────────┤
│  Direction                │  Employee ◄───────► Profile         │
│  FK Location              │  employees table ONLY (fk_profile_id)│
│  Owning Side              │  Employee (has @JoinColumn)         │
│  Inverse Side             │  Profile (has mappedBy="profile")   │
│  Navigate Emp → Prof      │  ✅  emp.getProfile()               │
│  Navigate Prof → Emp      │  ✅  profile.getEmployee()          │
│  Who writes FK to DB      │  Only Employee (owning side)        │
│  Sync both sides          │  ✅ Required — use helper methods   │
│  mappedBy refers to       │  Field name in owning class         │
│  Inverse side changes     │  ❌ IGNORED by JPA                 │
└───────────────────────────┴─────────────────────────────────────┘
```
