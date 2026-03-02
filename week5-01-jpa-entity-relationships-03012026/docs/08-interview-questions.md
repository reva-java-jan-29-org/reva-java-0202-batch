# JPA Entity Relationships — Interview Questions

> Covers: `@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`,
> owning side, `mappedBy`, cascade, fetch types, N+1 problem, join tables.

---

## Section 1 — Core Concepts (Beginner)

---

**Q1. What are the four types of entity relationships in JPA?**

> **Answer:**
> 1. `@OneToOne` — One entity is linked to exactly one other entity
> 2. `@OneToMany` — One entity is linked to many instances of another
> 3. `@ManyToOne` — Many instances of one entity are linked to one instance of another
> 4. `@ManyToMany` — Many instances of one entity are linked to many instances of another
>
> Each can be **unidirectional** (one side knows about the other) or **bidirectional** (both sides know about each other).

---

**Q2. What is the difference between a unidirectional and a bidirectional relationship?**

> **Answer:**
>
> | | Unidirectional | Bidirectional |
> |---|---|---|
> | Navigation | One-way: A → B | Two-way: A ↔ B |
> | Annotation | Only on one class | On both classes |
> | `mappedBy` | Not needed | Required on inverse side |
> | DB structure | Same (FK in same place) | Same (no extra FK added) |
>
> ```java
> // Unidirectional — only Employee knows Profile
> class Employee {
>     @OneToOne
>     Profile profile;
> }
> class Profile {
>     // no reference to Employee
> }
>
> // Bidirectional — both know each other
> class Employee {
>     @OneToOne
>     @JoinColumn(name = "fk_profile_id")
>     Profile profile;     // owning side
> }
> class Profile {
>     @OneToOne(mappedBy = "profile")
>     Employee employee;   // inverse side
> }
> ```

---

**Q3. What is the "owning side" of a relationship? How do you identify it?**

> **Answer:**
> The **owning side** is the entity that:
> - Holds the FK column in the database table
> - Has the `@JoinColumn` or `@JoinTable` annotation
> - Is the side whose changes JPA writes to the DB
>
> **How to identify:**
> - Side **without** `mappedBy` = **Owning side**
> - Side **with** `mappedBy` = **Inverse (non-owning) side**
>
> ```java
> // Owning side — has @JoinColumn, has the FK
> @OneToOne
> @JoinColumn(name = "fk_profile_id")
> private Profile profile;
>
> // Inverse side — has mappedBy, no FK column
> @OneToOne(mappedBy = "profile")
> private Employee employee;
> ```

---

**Q4. What does `mappedBy` mean and what value does it take?**

> **Answer:**
> `mappedBy` tells JPA:
> - "This side does NOT own the relationship"
> - "The FK is managed by the field named `<value>` in the other class"
>
> The value of `mappedBy` is the **field name** in the **owning class** — NOT the column name.
>
> ```java
> // In Employee.java (owning side):
> @OneToOne
> @JoinColumn(name = "fk_profile_id")   // column name = fk_profile_id
> private Profile profile;               // field name = "profile"  ← mappedBy uses THIS
>
> // In Profile.java (inverse side):
> @OneToOne(mappedBy = "profile")        // "profile" = field name in Employee, NOT column name
> private Employee employee;
> ```
>
> **Common mistake:** Confusing `mappedBy` value with the DB column name.

---

**Q5. Where does the Foreign Key (FK) column get placed for each relationship type?**

> **Answer:**
>
> | Relationship | FK Location |
> |---|---|
> | `@OneToOne` | Owning entity's table |
> | `@ManyToOne` | "Many" entity's table (always) |
> | `@OneToMany` unidirectional (no `@JoinColumn`) | Separate JOIN TABLE |
> | `@OneToMany` unidirectional (with `@JoinColumn`) | "Many" entity's table |
> | `@OneToMany` bidirectional | "Many" entity's table (via `@ManyToOne` side) |
> | `@ManyToMany` | Separate JOIN TABLE (always) |

---

**Q6. What are the default fetch types for each relationship annotation?**

> **Answer:**
>
> | Annotation | Default Fetch | Recommended |
> |---|---|---|
> | `@OneToOne` | **EAGER** | Change to LAZY |
> | `@ManyToOne` | **EAGER** | Change to LAZY |
> | `@OneToMany` | **LAZY** | Keep LAZY |
> | `@ManyToMany` | **LAZY** | Keep LAZY |
>
> **Memory trick:** Single-entity references (OneToOne, ManyToOne) → EAGER by default.
> Collection references (OneToMany, ManyToMany) → LAZY by default.

---

**Q7. What is `CascadeType`? List the cascade types and explain what they do.**

> **Answer:**
> Cascade types control which JPA operations on the **parent** are automatically propagated to the **child**.
>
> | CascadeType | What It Does |
> |---|---|
> | `PERSIST` | Saving parent also saves child |
> | `MERGE` | Updating parent also updates child |
> | `REMOVE` | Deleting parent also deletes child |
> | `REFRESH` | Refreshing parent also refreshes child |
> | `DETACH` | Detaching parent also detaches child |
> | `ALL` | All of the above |
>
> **Default:** NONE — no cascade.
>
> ```java
> // Without cascade — must save Profile separately first
> @OneToOne
> private Profile profile;
>
> // With cascade ALL — saving Employee auto-saves Profile
> @OneToOne(cascade = CascadeType.ALL)
> private Profile profile;
> ```

---

## Section 2 — Intermediate

---

**Q8. In a bidirectional `@OneToMany`, which side is the owning side and why?**

> **Answer:**
> The **`@ManyToOne` side (Employee) is ALWAYS the owning side** in a bidirectional `@OneToMany`.
>
> **Why?**
> - Each Employee row holds a FK column (`fk_dept_id`) that points to a Department
> - It makes logical sense for the entity with the FK column to own the relationship
> - JPA enforces this: you cannot put `@JoinColumn` on the `@OneToMany` side in bidirectional
>
> ```java
> // OWNING side — @ManyToOne has @JoinColumn
> @ManyToOne(fetch = FetchType.LAZY)
> @JoinColumn(name = "fk_dept_id")
> private Department department;    // Employee is the OWNER
>
> // INVERSE side — @OneToMany has mappedBy
> @OneToMany(mappedBy = "department")
> private List<Employee> employees;  // Department is the INVERSE
> ```
>
> **Rule:** In bidirectional `@OneToMany`, `@ManyToOne` = owning, `@OneToMany(mappedBy)` = inverse.

---

**Q9. What happens if you only set the inverse side of a bidirectional relationship and then save?**

> **Answer:**
> The FK is **NOT written to the database**. JPA ignores changes made to the inverse side.
>
> ```java
> Department dept = departmentRepository.findById(1L).get();
> Employee alice = new Employee("Alice", "Pune", 60000.0);
>
> // ❌ WRONG — Only inverse side set, owning side not set
> dept.getEmployees().add(alice);   // inverse side — IGNORED by JPA
> departmentRepository.save(dept);
>
> // Result: alice.fk_dept_id = NULL in the database!
>
> // ✅ CORRECT — Set owning side
> dept.addEmployee(alice);          // helper sets BOTH sides
> // internally: dept.employees.add(alice) + alice.setDepartment(dept)
> departmentRepository.save(dept);
> // Result: alice.fk_dept_id = 1 correctly saved
> ```

---

**Q10. What is the N+1 query problem? How does it occur and how do you fix it?**

> **Answer:**
> The N+1 problem occurs when loading a list of N entities triggers N additional queries to load their related entities.
>
> **How it occurs:**
> ```java
> // 1 query: SELECT * FROM employees
> List<Employee> employees = employeeRepository.findAll();
>
> // N queries: SELECT * FROM departments WHERE dept_id = ? (one per employee!)
> employees.forEach(e -> System.out.println(e.getDepartment().getDeptName()));
>
> // Total: 1 + N = N+1 queries
> ```
>
> **Why it happens:**
> - `@ManyToOne` default fetch is EAGER — each employee triggers a department query
> - Or with LAZY, each `getDepartment()` call in the loop triggers a separate query
>
> **Fix 1 — JOIN FETCH (best for bulk loads):**
> ```java
> @Query("SELECT e FROM Employee e JOIN FETCH e.department")
> List<Employee> findAllWithDepartment();
> // ONE query with JOIN — loads everything at once
> ```
>
> **Fix 2 — Change to LAZY and only access when needed:**
> ```java
> @ManyToOne(fetch = FetchType.LAZY)  // Don't load dept unless needed
> private Department department;
> ```

---

**Q11. What is the difference between `orphanRemoval = true` and `CascadeType.REMOVE`?**

> **Answer:**
>
> | | `CascadeType.REMOVE` | `orphanRemoval = true` |
> |---|---|---|
> | **Trigger** | Parent entity is **deleted** | Child is **removed from the parent's collection** |
> | **Effect** | Deletes all children when parent is deleted | Deletes child when it loses its parent reference |
> | **Annotation** | `cascade = CascadeType.REMOVE` | `orphanRemoval = true` |
>
> ```java
> @OneToMany(mappedBy = "department",
>     cascade = CascadeType.ALL,    // REMOVE fires when dept is deleted
>     orphanRemoval = true)          // REMOVE fires when emp removed from list
> private List<Employee> employees;
>
> // CascadeType.REMOVE:
> departmentRepository.delete(dept);    // deletes dept + ALL employees
>
> // orphanRemoval:
> dept.getEmployees().remove(alice);    // alice removed from list
> departmentRepository.save(dept);      // ← alice now deleted from DB
> ```
>
> They are often used together for complete parent-owns-child lifecycle management.

---

**Q12. Why should you never use `CascadeType.ALL` or `CascadeType.REMOVE` on `@ManyToMany`?**

> **Answer:**
> In `@ManyToMany`, both entities are **independent and shared**. One student can be in many courses, and one course has many students.
>
> If `CascadeType.REMOVE` is applied:
> ```java
> @ManyToMany(cascade = CascadeType.ALL)  // ❌ DANGEROUS!
> private List<Course> courses;
>
> // Deleting Alice would also delete Java, Spring Boot, DB Design courses!
> // But Bob, Carol are also enrolled in those courses — data corruption!
> studentRepository.delete(alice);
> ```
>
> **Use only:**
> ```java
> @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
> // PERSIST: new courses saved when student is saved
> // MERGE: course updates propagated when student is updated
> // But REMOVE is excluded — deleting student does NOT delete courses
> ```

---

**Q13. What is a JOIN TABLE in `@ManyToMany`? What does `@JoinTable` configure?**

> **Answer:**
> A **join table** is a separate database table that represents a Many-to-Many relationship. It contains only FK columns pointing to both related entities.
>
> ```
> students table       courses table        student_courses (JOIN TABLE)
> ────────────────     ─────────────────    ──────────────────────────────
> student_id | name    course_id | name     student_id  │  course_id
>      1     | Alice        1   | Java           1      │     1
>      2     | Bob          2   | Spring          1      │     2
>                                                2      │     1
> ```
>
> `@JoinTable` configures:
> ```java
> @JoinTable(
>     name = "student_courses",              // join table name
>     joinColumns = @JoinColumn(name = "student_id"),       // FK to THIS entity
>     inverseJoinColumns = @JoinColumn(name = "course_id")  // FK to OTHER entity
> )
> ```
>
> Without `@JoinTable`, JPA auto-generates ugly table and column names like
> `mtm_uni_student_mtm_uni_course` with columns like `students_student_id`.

---

**Q14. In a bidirectional `@ManyToMany`, how many join tables are created?**

> **Answer:**
> **Only ONE** join table is created, even though both entities have `@ManyToMany`.
>
> The join table is defined on the **owning side** (`@JoinTable`).
> The **inverse side** (`mappedBy`) reads from the same join table — it doesn't create another.
>
> ```java
> // Student — OWNING side — defines the join table
> @ManyToMany
> @JoinTable(name = "student_courses", ...)   // defines the one join table
> private List<Course> courses;
>
> // Course — INVERSE side — reads from Student's join table
> @ManyToMany(mappedBy = "courses")           // no @JoinTable here!
> private List<Student> students;
>
> // Only ONE table in DB: student_courses
> ```
>
> If BOTH sides had `@JoinTable`, JPA would create TWO separate join tables — a bug!

---

## Section 3 — Advanced

---

**Q15. What is the difference between `@OneToMany` unidirectional and bidirectional in terms of SQL efficiency?**

> **Answer:**
>
> **Unidirectional `@OneToMany` with `@JoinColumn`:**
> When saving a Department with 3 new employees, JPA runs:
> ```sql
> INSERT INTO employees (name, city) VALUES (?, ?)    -- no FK yet
> INSERT INTO employees (name, city) VALUES (?, ?)
> INSERT INTO employees (name, city) VALUES (?, ?)
> INSERT INTO departments (dept_name) VALUES (?)
> UPDATE employees SET fk_dept_id = ? WHERE emp_id = ? -- extra UPDATE!
> UPDATE employees SET fk_dept_id = ? WHERE emp_id = ?
> UPDATE employees SET fk_dept_id = ? WHERE emp_id = ?
> ```
> Extra UPDATE per employee because Employee has no `department` field — JPA cannot include FK in INSERT.
>
> **Bidirectional `@OneToMany` (Employee has `@ManyToOne`):**
> ```sql
> INSERT INTO departments (dept_name) VALUES (?)
> INSERT INTO employees (name, city, fk_dept_id) VALUES (?, ?, ?) -- FK included!
> INSERT INTO employees (name, city, fk_dept_id) VALUES (?, ?, ?)
> INSERT INTO employees (name, city, fk_dept_id) VALUES (?, ?, ?)
> ```
> No extra UPDATEs — FK is included directly in the INSERT.
>
> **Conclusion:** Bidirectional `@OneToMany` is more SQL-efficient.

---

**Q16. What is `LazyInitializationException` and how do you prevent it?**

> **Answer:**
> `LazyInitializationException` is thrown when you try to access a LAZY-loaded relationship **after the Hibernate session (transaction) has been closed**.
>
> ```java
> // Transaction ends here
> Employee emp = employeeRepository.findById(1L).orElseThrow();
> // Transaction closed after findById returns
>
> // Trying to access LAZY collection OUTSIDE transaction:
> emp.getDepartment().getDeptName();  // ← LazyInitializationException!
> // Session is closed, Hibernate can't load department
> ```
>
> **Fixes:**
>
> 1. **JOIN FETCH** — load eagerly in the query:
> ```java
> @Query("SELECT e FROM Employee e JOIN FETCH e.department WHERE e.empId = :id")
> Optional<Employee> findByIdWithDept(Long id);
> ```
>
> 2. **`@Transactional`** — keep session open during the method:
> ```java
> @Transactional
> public String getDeptName(Long empId) {
>     Employee emp = employeeRepository.findById(empId).orElseThrow();
>     return emp.getDepartment().getDeptName();  // session still open
> }
> ```
>
> 3. **DTO projection** — fetch only the data you need, no lazy loading needed.

---

**Q17. How does `@Entity(name = "...")` differ from `@Table(name = "...")`?**

> **Answer:**
>
> | Annotation | Controls | Used In |
> |---|---|---|
> | `@Entity(name = "X")` | The **JPA entity name** used in JPQL queries | `"SELECT e FROM X e WHERE ..."` |
> | `@Table(name = "Y")` | The **database table name** | `CREATE TABLE Y (...)` |
>
> ```java
> @Entity(name = "OtmBiEmployee")       // JPQL name: SELECT e FROM OtmBiEmployee e
> @Table(name = "otm_bi_employees")     // DB table:  SELECT * FROM otm_bi_employees
> public class Employee { ... }
> ```
>
> When you have two classes named `Employee` in different packages,
> `@Entity(name = "...")` makes the JPA name unique so JPQL works without ambiguity.

---

**Q18. What is the save order rule and when does it apply?**

> **Answer:**
> The save order rule applies when `cascade = NONE` (the default) on `@ManyToOne` or `@OneToOne`:
>
> > **You must save the referenced (parent) entity FIRST before the referencing (child) entity.**
>
> ```java
> // ❌ WRONG — Department not yet in DB (no ID)
> Employee emp = new Employee("Alice", "Pune", new Department("Eng"));
> employeeRepository.save(emp);
> // Throws: TransientPropertyValueException
>
> // ✅ CORRECT — Save Department first
> Department dept = departmentRepository.save(new Department("Engineering", "Bangalore"));
> // dept now has an ID
> Employee emp = new Employee("Alice", "Pune", dept);
> employeeRepository.save(emp);
> // emp.fk_dept_id = dept.deptId ← correctly set
> ```
>
> **When it doesn't apply:**
> If `cascade = CascadeType.PERSIST` or `ALL` is set, you can save the child and JPA will auto-save the parent.

---

**Q19. What is the problem with using Lombok's `@Data` on bidirectional entity relationships?**

> **Answer:**
> `@Data` generates `equals()`, `hashCode()`, and `toString()` using ALL fields, including relationship fields. In a bidirectional relationship, this causes **infinite recursion** (StackOverflowError):
>
> ```
> Employee.toString() → includes Profile
>   Profile.toString() → includes Employee
>     Employee.toString() → includes Profile
>       ... → StackOverflowError!
> ```
>
> **Fix — use `@Getter @Setter` instead of `@Data`, and manually write `toString()`:**
> ```java
> @Getter @Setter
> @NoArgsConstructor
> public class Employee {
>     ...
>     @OneToOne(mappedBy = "employee")
>     private Profile profile;
>
>     @Override
>     public String toString() {
>         // Do NOT include profile here!
>         return "Employee{empId=" + empId + ", name=" + name + "}";
>     }
> }
> ```
>
> Also affects `equals()` and `hashCode()` — avoid Lombok's auto-generated versions for JPA entities.

---

**Q20. What is the difference between `FetchType.EAGER` and `FetchType.LAZY`? Which is better and why?**

> **Answer:**
>
> | | EAGER | LAZY |
> |---|---|---|
> | When loaded | Immediately with parent query | Only when field is accessed |
> | SQL | JOIN in the main query | Separate query on demand |
> | Performance | ⚠️ Slower (loads unused data) | ✅ Better (load only what's needed) |
> | Risk | Loads too much | `LazyInitializationException` if accessed outside transaction |
>
> ```java
> // EAGER — department loaded with every employee query, whether needed or not
> @ManyToOne(fetch = FetchType.EAGER)  // default!
> private Department department;
> // SQL: SELECT e.*, d.* FROM employees e JOIN departments d ON ...
>
> // LAZY — department only loaded when emp.getDepartment() is called
> @ManyToOne(fetch = FetchType.LAZY)   // recommended
> private Department department;
> // SQL: SELECT * FROM employees WHERE ...
> // (separate SELECT for department only if accessed)
> ```
>
> **LAZY is almost always better** because:
> - Reduces memory usage
> - Prevents N+1 queries (when combined with JOIN FETCH where needed)
> - Gives you control over when data is loaded

---

## Section 4 — Scenario / Trick Questions

---

**Q21. Can two employees point to the same Profile in a `@OneToOne` relationship? How do you prevent it?**

> **Answer:**
> At the Java level, nothing stops this. But at the **database level**, you prevent it using `unique = true` on `@JoinColumn`:
>
> ```java
> @OneToOne
> @JoinColumn(name = "fk_profile_id",
>     unique = true)      // ← DB UNIQUE constraint on this FK column
> private Profile profile;
> ```
>
> This creates a `UNIQUE` constraint on `fk_profile_id` in the employees table.
> If a second employee tries to reference the same profile, the DB throws a constraint violation.
>
> Without `unique = true`, you have a `@ManyToOne`-like relationship, not a true `@OneToOne`.

---

**Q22. If you call `profileRepository.save(profile)` after setting `profile.setEmployee(anotherEmp)` in a bidirectional OneToOne, does the FK change?**

> **Answer:**
> **No.** The FK does NOT change because `profile.employee` is the **inverse side** (`mappedBy`).
>
> Changes to the inverse side are **ignored by JPA** when writing to the database.
>
> To change the FK, you must update the **owning side**:
> ```java
> // ❌ Does NOT change FK — inverse side change is IGNORED
> profile.setEmployee(newEmployee);
> profileRepository.save(profile);
>
> // ✅ DOES change FK — update owning side
> employee.setProfile(newProfile);
> employeeRepository.save(employee);
> ```

---

**Q23. What is the result if you put `@JoinColumn` on BOTH sides of a `@OneToOne` bidirectional?**

> **Answer:**
> JPA treats them as **two separate unidirectional relationships**, not one bidirectional.
>
> ```java
> // ❌ WRONG — @JoinColumn on BOTH sides
> class Employee {
>     @OneToOne
>     @JoinColumn(name = "fk_profile_id")   // FK in employees table
>     private Profile profile;
> }
> class Profile {
>     @OneToOne
>     @JoinColumn(name = "fk_employee_id")  // FK in profiles table (accidental!)
>     private Employee employee;
> }
> ```
>
> Result: **TWO FK columns** — one in each table — which is NOT a standard bidirectional OneToOne.
>
> **Correct way:** Only owning side has `@JoinColumn`. Inverse side has `mappedBy`.

---

**Q24. In a `@ManyToMany` bidirectional, if you add a Student to `course.getStudents()` and save the Course, will the join table entry be created?**

> **Answer:**
> **No.** `course.getStudents()` is the **inverse side** (`mappedBy`). Changes to it are ignored by JPA.
>
> ```java
> course.getStudents().add(alice);     // ❌ inverse side — IGNORED
> courseRepository.save(course);       // No join table entry created!
>
> // Correct way — update the OWNING side (student.courses):
> alice.getCourses().add(course);      // ✅ owning side — will be persisted
> studentRepository.save(alice);       // ✅ join table entry created
>
> // Best practice — use helper method:
> alice.enrollInCourse(course);        // sets BOTH sides
> studentRepository.save(alice);
> ```

---

**Q25. What is the difference between `@OneToMany` unidirectional (with `@JoinColumn`) and `@ManyToOne` unidirectional? Do they produce the same table structure?**

> **Answer:**
> **Yes, the database table structure is the same**, but the entity that declares the relationship is different.
>
> ```
> DB tables (identical):
> departments: dept_id | dept_name
> employees:   emp_id  | name  | fk_dept_id (FK → departments.dept_id)
> ```
>
> | | `@OneToMany` + `@JoinColumn` (on Dept) | `@ManyToOne` + `@JoinColumn` (on Emp) |
> |---|---|---|
> | Where declared | `Department.employees` | `Employee.department` |
> | Navigation | `dept.getEmployees()` | `emp.getDepartment()` |
> | Who controls FK | Department (via list) | Employee (via direct field) |
> | SQL efficiency | ⚠️ Extra UPDATEs | ✅ FK in INSERT directly |
> | Bi-dir option | Add `@ManyToOne` to Employee | Add `@OneToMany(mappedBy)` to Dept |
>
> **Recommendation:** Prefer `@ManyToOne` on Employee. It is more efficient and is the natural mapping.

---

**Q26. Why is `@ManyToOne` called "the most fundamental" JPA relationship?**

> **Answer:**
> `@ManyToOne` is fundamental because all other relationship types are built on top of it:
>
> ```
> @ManyToOne alone
>   = Unidirectional Many-to-One
>
> @ManyToOne + @OneToMany(mappedBy) on the other side
>   = Bidirectional One-to-Many
>
> @ManyToOne on BOTH entities (same entity)
>   = Self-referencing / Tree structure (e.g., Employee has a manager who is also an Employee)
>
> @ManyToMany internally
>   = Two @ManyToOne relationships to a join entity
> ```
>
> Also: `@ManyToOne` ALWAYS owns the FK — no exceptions — which makes reasoning about ownership straightforward.

---

## Section 5 — Quick Fire Questions

---

**Q27.** What is the default cascade type for all JPA relationships?
> **Answer:** `NONE` — no cascade by default.

---

**Q28.** What is the default fetch type for `@ManyToOne`?
> **Answer:** `EAGER` — always change it to `LAZY`.

---

**Q29.** Where does JPA place the FK in a `@ManyToMany` relationship?
> **Answer:** In a **separate join table** — never in either entity's own table.

---

**Q30.** Can you put `mappedBy` on BOTH sides of a bidirectional relationship?
> **Answer:** **No.** If both sides have `mappedBy`, neither side owns the relationship — JPA won't know where to write the FK. Exactly ONE side must own the relationship (no `mappedBy`), and the other uses `mappedBy`.

---

**Q31.** What annotation do you use to customize the FK column name?
> **Answer:** `@JoinColumn(name = "custom_fk_name")` on the owning side.

---

**Q32.** What annotation do you use to customize the join table name in `@ManyToMany`?
> **Answer:** `@JoinTable(name = "custom_table_name", ...)` on the owning side.

---

**Q33.** If you delete an entity from a `@OneToMany` collection and save, will the child be deleted from the DB automatically?
> **Answer:** Only if `orphanRemoval = true` is set. Otherwise, the FK is set to null (if nullable) but the child row remains.

---

**Q34.** In `@OneToMany(mappedBy = "department")`, what does `"department"` refer to?
> **Answer:** The **field name** (not column name) in the owning class — specifically, the field `private Department department` in the `Employee` class.

---

**Q35.** You have a list of 500 employees, all fetched from the DB. Accessing `emp.getDepartment()` in a loop fires 500 extra queries. What is this called and how do you fix it?
> **Answer:** This is the **N+1 query problem**. Fix it with a JOIN FETCH query:
> ```java
> @Query("SELECT e FROM Employee e JOIN FETCH e.department")
> List<Employee> findAllWithDepartment();
> ```

---

*End of Interview Questions*
