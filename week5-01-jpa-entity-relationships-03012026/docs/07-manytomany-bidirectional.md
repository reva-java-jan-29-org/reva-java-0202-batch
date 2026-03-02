# Many-to-Many Bidirectional Relationship

**Package:** `com.training.manytomany.bidirectional`
**Tables:** `mtm_bi_students`, `mtm_bi_courses`, `mtm_bi_student_courses` (join table)

---

## What Is It?

A **bidirectional Many-to-Many** means both entities know about each other.
Navigation works in **both directions** from Java code.

```
Student ◄──────────────► Course
  (has List<Course>)       (has List<Student>)
```

---

## Key Difference from Unidirectional

| | Unidirectional | Bidirectional |
|---|---|---|
| `@ManyToMany` on Student | ✅ | ✅ |
| `@ManyToMany` on Course | ❌ | ✅ (with `mappedBy`) |
| `student.getCourses()` | ✅ | ✅ |
| `course.getStudents()` | ❌ | ✅ |
| Number of join tables | 1 | 1 (still just 1!) |
| `@JoinTable` definition | On Student | On Student (owning side) |

> **Important:** Even though BOTH classes have `@ManyToMany`, there is still **only ONE join table**.
> It is defined on the **owning side** (Student).
> The inverse side (Course) reads from that same join table via `mappedBy`.

---

## Class Structure

```java
// OWNING SIDE — Student defines the join table
class Student {
    int studentId;
    String name;

    @ManyToMany
    @JoinTable(
        name = "mtm_bi_student_courses",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    List<Course> courses;                  // defines join table
}

// INVERSE SIDE — Course reads from Student's join table
class Course {
    int courseId;
    String courseName;

    @ManyToMany(mappedBy = "courses")      // "courses" = field name in Student
    List<Student> students;                // reads from join table via mappedBy
}
```

---

## Database Tables

```
mtm_bi_students          mtm_bi_courses           mtm_bi_student_courses
─────────────────────    ──────────────────────    ──────────────────────────
student_id │ name         course_id │ course_name   student_id │ course_id
───────────┼──────        ──────────┼───────────    ───────────┼───────────
    1      │ Alice             1    │ Java Prog.         1     │     1
    2      │ Bob               2    │ Spring Boot        1     │     2
                               3    │ DB Design          2     │     1
                                                        2     │     3

⚠️ No FK columns in either students or courses table
✅ Join table holds all relationship data
⚠️ ONLY ONE join table — both sides read from mtm_bi_student_courses
```

---

## Owning Side vs Inverse Side in @ManyToMany

| | Owning Side (Student) | Inverse Side (Course) |
|---|---|---|
| Has `@JoinTable` | ✅ Yes | ❌ No |
| Has `mappedBy` | ❌ No | ✅ Yes (`mappedBy = "courses"`) |
| Defines join table structure | ✅ Yes | ❌ No (reads from Student's) |
| Writing to join table | ✅ Changes persisted | ❌ Changes IGNORED |
| Reading from join table | ✅ Both work after DB load |  ✅ |

---

## Understanding `mappedBy` in @ManyToMany

```java
// In Course.java:
@ManyToMany(mappedBy = "courses")
                        ↑
          This is the FIELD NAME in Student.java

// In Student.java:
@ManyToMany
@JoinTable(...)
private List<Course> courses;   ← this field is named "courses"
                                   ← mappedBy = "courses" refers to THIS
```

`mappedBy = "courses"` means:
> "The field named `courses` in Student class owns this relationship and defines the join table."

---

## The Helper Method Pattern (Critical for Bidirectional)

Because both sides hold a reference in memory, you must keep them in sync.

```java
// In Student.java
public void enrollInCourse(Course course) {
    this.courses.add(course);         // ✅ Owning side — writes to join table
    course.getStudents().add(this);   // ✅ Inverse side — in-memory navigation
}

public void dropCourse(Course course) {
    this.courses.remove(course);
    course.getStudents().remove(this);
}

// In Course.java
public void addStudent(Student student) {
    this.students.add(student);       // Inverse side (in-memory only)
    student.getCourses().add(this);   // Owning side — writes to join table
}
```

---

## The #1 Rule: Only Owning Side Writes to Join Table

This is the most important concept in bidirectional @ManyToMany:

```java
// Load entities
Course java = courseRepository.findById(1L).orElseThrow();
Student alice = studentRepository.findById(1L).orElseThrow();

// ❌ WRONG — Only setting INVERSE side (Course.students)
java.getStudents().add(alice);        // inverse side → IGNORED by JPA
courseRepository.save(java);          // NO new join table entry!

// ✅ CORRECT — Setting OWNING side (Student.courses)
alice.getCourses().add(java);         // owning side → writes to join table!
studentRepository.save(alice);        // ✅ New entry in mtm_bi_student_courses

// ✅ BEST — Use helper method (sets both sides)
alice.enrollInCourse(java);           // sets owning + inverse sides
studentRepository.save(alice);        // ✅ New entry in join table
```

---

## Step-by-Step: How to Enroll a Student in a Course

```java
// Method 1 — Using Student helper (recommended)
Student alice = studentRepository.findById(1L).orElseThrow();
Course java   = courseRepository.findById(1L).orElseThrow();

alice.enrollInCourse(java);       // Step 1: sets both sides
studentRepository.save(alice);    // Step 2: save owning side → writes join entry

// Method 2 — Manual (ensures owning side is set)
alice.getCourses().add(java);     // owning side ← required!
java.getStudents().add(alice);    // inverse side (optional, for in-memory consistency)
studentRepository.save(alice);    // save owning side

// Method 3 — WRONG (does not persist!)
java.getStudents().add(alice);    // inverse side only
courseRepository.save(java);      // saving inverse side → join entry NOT created!
```

---

## Code Examples

### Save with bidirectional navigation

```java
Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));
Course spring = courseRepository.save(new Course("Spring Boot", "Prof. Gupta", 6));

Student alice = new Student("Alice", "alice@school.com");
Student bob   = new Student("Bob",   "bob@school.com");

alice.enrollInCourse(java);    // alice knows java, java knows alice
alice.enrollInCourse(spring);  // alice knows spring, spring knows alice
bob.enrollInCourse(java);      // bob knows java, java knows alice and bob

studentRepository.save(alice);
studentRepository.save(bob);

// Navigate Student → Courses (owning side)
Student fetchedAlice = studentRepository.findById(alice.getStudentId()).orElseThrow();
System.out.println(fetchedAlice.getCourses().size());   // 2

// Navigate Course → Students (inverse side!)
Course fetchedJava = courseRepository.findById(java.getCourseId()).orElseThrow();
System.out.println(fetchedJava.getStudents().size());   // 2 (Alice and Bob)
```

### JPQL queries from both sides

```java
// From Student side: find all students in a course
@Query("SELECT s FROM MtmBiStudent s JOIN s.courses c WHERE c.courseId = :courseId")
List<Student> findStudentsByCourseId(Long courseId);

// From Course side: find all courses for a student
@Query("SELECT c FROM MtmBiCourse c JOIN c.students s WHERE s.studentId = :studentId")
List<Course> findCoursesByStudentId(Long studentId);

// JOIN FETCH to avoid LazyInitializationException
@Query("SELECT DISTINCT s FROM MtmBiStudent s LEFT JOIN FETCH s.courses WHERE s.studentId = :id")
Optional<Student> findByIdWithCourses(Long id);
```

### Delete student (courses must be cleared first)

```java
Student alice = studentRepository.findById(1L).orElseThrow();

// Clear courses from owning side first
// This removes all entries from join table for alice
alice.getCourses().clear();
studentRepository.save(alice);  // deletes all join table rows for alice

// Now delete alice
studentRepository.delete(alice);

// Courses still exist in DB! (not deleted)
```

---

## Infinite Recursion Warning

With bidirectional `@ManyToMany`, both sides reference each other.
`toString()`, JSON serialization, or Lombok `@Data` can cause infinite loops:

```
student.toString() → calls course.toString() → calls student.toString() → StackOverflow!
```

### Fix — Exclude back-reference in toString()

```java
// Student.java
@Override
public String toString() {
    // Do NOT call courses.toString() here!
    return "Student{id=" + studentId + ", name='" + name + "', courses=" + courses.size() + "}";
}

// Course.java
@Override
public String toString() {
    // Do NOT call students.toString() here!
    return "Course{id=" + courseId + ", name='" + courseName + "', students=" + students.size() + "}";
}
```

> Also avoid using Lombok `@Data` when you have bidirectional relationships — it generates `equals()`, `hashCode()`, and `toString()` that can cause infinite loops. Use `@Getter @Setter` separately.

---

## Delete Behavior Summary

| Operation | What Gets Deleted |
|---|---|
| Delete Student | Student row + all join table rows for that student |
| Delete Course | Course row + all join table rows for that course |
| `student.dropCourse(course)` | ONE join table row (student–course pair) |
| Courses when Student deleted | ❌ NOT deleted (no cascade REMOVE!) |
| Students when Course deleted | ❌ NOT deleted (no cascade REMOVE!) |

---

## Uni vs Bi — Side-by-Side Comparison

| | Unidirectional | Bidirectional |
|---|---|---|
| `@ManyToMany` on Student | ✅ | ✅ |
| `@ManyToMany` on Course | ❌ | ✅ (with `mappedBy`) |
| `student.getCourses()` | ✅ | ✅ |
| `course.getStudents()` | ❌ | ✅ |
| Join tables | 1 | 1 (still only 1!) |
| Must sync both sides | ❌ N/A | ✅ Required |
| JPQL from both sides | ⚠️ Only from Student | ✅ From both |
| Infinite recursion risk | Low | ✅ Must guard against it |

---

## Default Strategy Table

| Aspect | Default | Recommended |
|---|---|---|
| Join Table | Auto-generated name | Use `@JoinTable` with explicit name |
| FK Ownership | Side with `@JoinTable` (Student) | Student |
| Reads join table | Owning side (Student.courses) | — |
| Fetch Type | **LAZY** | Keep LAZY (always!) |
| Cascade | **NONE** | `{PERSIST, MERGE}` only — NEVER `REMOVE` |
| Sync rule | Manual | Use helper methods — `enrollInCourse()` |

---

## Summary

```
┌──────────────────────────────────────────────────────────────────────┐
│  MANY-TO-MANY BIDIRECTIONAL — QUICK REFERENCE                        │
├────────────────────────────────┬─────────────────────────────────────┤
│  Direction                     │  Student ◄───────► [Course, ...]    │
│  Join Table                    │  mtm_bi_student_courses             │
│  Owning Side                   │  Student (has @JoinTable)           │
│  Inverse Side                  │  Course (has mappedBy="courses")    │
│  Navigate Student → Courses    │  ✅  student.getCourses()           │
│  Navigate Course → Students    │  ✅  course.getStudents()           │
│  Who writes join table         │  Only Student (owning side)         │
│  Inverse side changes          │  ❌ IGNORED by JPA                 │
│  Sync both sides               │  ✅ Required — use helper methods   │
│  Cascade REMOVE                │  ❌ NEVER — entities are shared!   │
│  Fetch Type                    │  LAZY — never change to EAGER       │
│  Infinite recursion            │  ✅ Guard in toString() / JSON      │
└────────────────────────────────┴─────────────────────────────────────┘
```
