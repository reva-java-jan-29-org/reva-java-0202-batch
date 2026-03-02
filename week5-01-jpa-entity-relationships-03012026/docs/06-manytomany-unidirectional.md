# Many-to-Many Unidirectional Relationship

**Package:** `com.training.manytomany.unidirectional`
**Tables:** `mtm_uni_students`, `mtm_uni_courses`, `mtm_uni_student_courses` (join table)

---

## What Is It?

A **Many-to-Many** relationship means:
- MANY instances of one entity are associated with MANY instances of another entity
- In **unidirectional**, only **one side knows about the other**

```
Student ─────────────► [Course, Course, Course]
  (knows its courses)    (knows nothing about students)
```

---

## Real-World Analogy

> A student can enroll in many courses.
> A course can have many students enrolled.
> But a Course record doesn't know who is enrolled — only the Student record knows which courses it's in.

---

## The Join Table

Unlike all other relationships, `@ManyToMany` does **NOT** place an FK in either entity's table.
Instead, JPA creates a separate **join table** that holds a pair of FK values.

```
mtm_uni_students         mtm_uni_courses         mtm_uni_student_courses
─────────────────────    ──────────────────────   ──────────────────────────
student_id │ name         course_id │ course_name  student_id │ course_id
───────────┼──────        ──────────┼───────────   ───────────┼───────────
    1      │ Alice             1    │ Java Prog.        1     │     1      ← Alice in Java
    2      │ Bob               2    │ Spring Boot       1     │     2      ← Alice in Spring
    3      │ Carol             3    │ DB Design         2     │     1      ← Bob in Java
                                                      2     │     3      ← Bob in DB Design
                                                      3     │     1      ← Carol in Java
```

---

## Two Ways to Define the Join Table

### Option A — Without `@JoinTable` (not recommended)

```java
@ManyToMany
private List<Course> courses;
```

JPA auto-generates the join table:
- Table name: `student_courses` (or `mtm_uni_student_mtm_uni_course`) — auto-generated, often ugly
- Column names: `student_student_id`, `courses_course_id` — verbose and unclear

### Option B — With `@JoinTable` (RECOMMENDED)

```java
@ManyToMany
@JoinTable(
    name = "mtm_uni_student_courses",           // explicit join table name
    joinColumns = @JoinColumn(name = "student_id"),      // FK → THIS entity (Student)
    inverseJoinColumns = @JoinColumn(name = "course_id") // FK → OTHER entity (Course)
)
private List<Course> courses;
```

Always use `@JoinTable` for explicit, clean table and column names.

---

## @JoinTable Attributes Explained

```java
@JoinTable(
    name = "mtm_uni_student_courses",
          ↑ The join table name in the database

    joinColumns = @JoinColumn(name = "student_id"),
                              ↑ FK column pointing to THIS entity's table (Student)

    inverseJoinColumns = @JoinColumn(name = "course_id")
                                     ↑ FK column pointing to the OTHER entity's table (Course)
)
```

---

## Class Structure

```java
// OWNING SIDE — Student has the @JoinTable, has the list
class Student {
    int studentId;
    String name;

    @ManyToMany
    @JoinTable(
        name = "mtm_uni_student_courses",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    List<Course> courses;
}

// NON-REFERENCED SIDE — Course knows nothing about students
class Course {
    int courseId;
    String courseName;
    // No @ManyToMany here — UNIDIRECTIONAL
}
```

---

## Cascade — What's Safe for @ManyToMany

> **NEVER use `CascadeType.REMOVE` or `CascadeType.ALL` on `@ManyToMany`!**

```java
// ❌ DANGEROUS — CascadeType.ALL includes REMOVE!
@ManyToMany(cascade = CascadeType.ALL)
private List<Course> courses;
// If you delete a Student, it would also DELETE all Courses!
// But those courses are shared with other students!

// ✅ SAFE — only PERSIST and MERGE
@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
private List<Course> courses;
```

| Cascade | Safe for @ManyToMany? | Reason |
|---|---|---|
| `PERSIST` | ✅ Yes | Saving student also saves new courses |
| `MERGE` | ✅ Yes | Updating student also updates courses |
| `REMOVE` | ❌ Never | Would delete shared courses! |
| `ALL` | ❌ Never | Includes REMOVE — too dangerous |
| `NONE` | ✅ Safe (but manual) | Must save courses separately |

---

## Fetch Type — Always LAZY for Collections

```java
// ✅ Default for @ManyToMany is LAZY — keep it!
@ManyToMany(fetch = FetchType.LAZY)
private List<Course> courses;

// ❌ NEVER use EAGER for @ManyToMany
@ManyToMany(fetch = FetchType.EAGER)
// If a student has 50 courses, loading ANY student loads all 50 courses!
// In a list of 100 students → up to 5000 course records loaded!
```

---

## Code Examples

### Enroll student in courses (cascade PERSIST saves new courses)

```java
// Courses already saved in DB
Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));
Course spring = courseRepository.save(new Course("Spring Boot", "Prof. Gupta", 6));

// Student enrolls in two courses
Student alice = new Student("Alice", "alice@school.com");
alice.enrollInCourse(java);     // adds to courses list
alice.enrollInCourse(spring);   // adds to courses list

// Save student → cascade saves join table entries
studentRepository.save(alice);

// SQL:
// INSERT INTO mtm_uni_students (name, email) VALUES ('Alice', 'alice@school.com')
// INSERT INTO mtm_uni_student_courses (student_id, course_id) VALUES (1, 1)
// INSERT INTO mtm_uni_student_courses (student_id, course_id) VALUES (1, 2)
```

### Save student with NEW courses (cascade PERSIST saves courses too)

```java
// Courses NOT yet in DB
Course java = new Course("Java Programming", "Prof. Sharma", 8);   // transient
Course spring = new Course("Spring Boot", "Prof. Gupta", 6);       // transient

Student alice = new Student("Alice", "alice@school.com");
alice.enrollInCourse(java);
alice.enrollInCourse(spring);

studentRepository.save(alice);
// CascadeType.PERSIST: saves alice, java, spring, and join entries — all in one save!
```

### Read courses for a student

```java
Student alice = studentRepository.findById(1L).orElseThrow();
List<Course> courses = alice.getCourses();   // triggers LAZY load
courses.forEach(c -> System.out.println(c.getCourseName()));
```

### Find students in a course (JPQL — since Course has no students list)

```java
// In StudentRepository:
@Query("SELECT s FROM MtmUniStudent s JOIN s.courses c WHERE c.courseId = :courseId")
List<Student> findStudentsByCourseId(Long courseId);

// Usage:
List<Student> javaStudents = studentRepository.findStudentsByCourseId(1L);
```

### Drop a course (removes join table entry only — Course NOT deleted)

```java
Student alice = studentRepository.findById(1L).orElseThrow();
Course javaCourse = alice.getCourses().stream()
    .filter(c -> c.getCourseName().equals("Java Programming"))
    .findFirst().orElseThrow();

alice.dropCourse(javaCourse);           // removes from courses list
studentRepository.save(alice);
// SQL: DELETE FROM mtm_uni_student_courses WHERE student_id=1 AND course_id=1
// Course itself (mtm_uni_courses) is NOT touched!
```

---

## Unidirectional Limitation

Since Course has no reference to students, you **cannot navigate** Course → Students:

```java
Course java = courseRepository.findById(1L).orElseThrow();
// java.getStudents()  ← This method does NOT exist! (unidirectional)

// To find students for a course, use JPQL:
List<Student> enrolled = studentRepository.findStudentsByCourseId(java.getCourseId());
```

In **bidirectional** (see `07-manytomany-bidirectional.md`), `course.getStudents()` works directly.

---

## Default Strategy Table

| Aspect | Default | Recommended |
|---|---|---|
| Join Table Name | Auto-generated (ugly) | Use `@JoinTable` with explicit name |
| Join Column Names | Auto-generated | Explicit via `joinColumns` and `inverseJoinColumns` |
| FK Ownership | Declaring side (Student) | Student (has `@JoinTable`) |
| Fetch Type | **LAZY** | Keep LAZY (never use EAGER) |
| Cascade | **NONE** | `{PERSIST, MERGE}` — NEVER `REMOVE` or `ALL` |

---

## What Happens When You Delete?

```
Delete Student → removes student row + all rows in join table for that student
                 ← Courses are NOT deleted (no cascade REMOVE)

Delete Course  → removes course row + all rows in join table for that course
                 ← Students are NOT deleted

Remove enrollment → removes ONE row from join table only
(drop course)       ← Neither Student nor Course is deleted
```

---

## Summary

```
┌──────────────────────────────────────────────────────────────────────┐
│  MANY-TO-MANY UNIDIRECTIONAL — QUICK REFERENCE                       │
├──────────────────────────────┬───────────────────────────────────────┤
│  Direction                   │  Student ──────► [Course, ...]        │
│  Join Table                  │  mtm_uni_student_courses               │
│  Owning Side                 │  Student (has @JoinTable)             │
│  Non-Referenced Side         │  Course (no @ManyToMany)              │
│  Navigate Student → Courses  │  ✅  student.getCourses()             │
│  Navigate Course → Students  │  ❌  Not possible (use JPQL instead)  │
│  Default Fetch               │  LAZY — keep it!                      │
│  Cascade REMOVE              │  ❌ NEVER — courses are shared!       │
│  Drop enrollment             │  Removes join row only                │
│  Delete student              │  Removes student + its join rows      │
└──────────────────────────────┴───────────────────────────────────────┘
```
