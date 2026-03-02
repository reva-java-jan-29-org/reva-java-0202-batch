package com.training.manytomany.bidirectional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * ================================================================
 *  MANY-TO-MANY BIDIRECTIONAL  -  OWNING SIDE (Student)
 * ================================================================
 *
 *  Scenario: MANY Students enrolled in MANY Courses.
 *            Navigation is TWO-WAY: Student <---> Course
 *            student.getCourses() works AND course.getStudents() works
 *
 *  BIDIRECTIONAL vs UNIDIRECTIONAL @ManyToMany:
 *  ----------------------------------------------
 *  Unidirectional: Student -> Course only
 *    - Cannot navigate Course -> Student in Java
 *    - Can query the DB using JPQL with JOIN
 *
 *  Bidirectional: Student <-> Course
 *    - Can navigate in BOTH directions in Java
 *    - student.getCourses() -> List of courses
 *    - course.getStudents() -> List of students
 *    - Only ONE join table exists (defined on owning side = Student)
 *
 *  OWNING SIDE RULES:
 *  -------------------
 *  Student is the OWNER because it has @JoinTable.
 *  This means:
 *    1. Student defines the join table structure (@JoinTable)
 *    2. JPA reads the join table from Student.courses when saving
 *    3. Course.students (with mappedBy) is for in-memory navigation only
 *    4. Changes to course.students ALONE do NOT write to the join table
 *       -> MUST also update student.courses for DB changes
 *
 *  HOW TO ENROLL A STUDENT IN A COURSE (Bidirectional):
 *  -------------------------------------------------------
 *  Method 1 (Recommended - using Student's helper):
 *    student.enrollInCourse(course);    <- sets both sides
 *    studentRepository.save(student);   <- writes to join table
 *
 *  Method 2 (Manual):
 *    student.getCourses().add(course);   <- owning side (writes to DB)
 *    course.getStudents().add(student);  <- inverse side (in-memory only)
 *    studentRepository.save(student);
 *
 *  WRONG WAY (does NOT write to join table):
 *    course.getStudents().add(student);  <- inverse side only -> NOT persisted!
 *    courseRepository.save(course);      <- still won't write the join entry
 *
 *  DATABASE TABLES:
 *  -----------------
 *  mtm_bi_students:            mtm_bi_courses:         mtm_bi_student_courses:
 *  student_id | name | email   course_id | course_name  student_id | course_id
 *      1      | Alice| ...         1     | Java Prog.       1      |     1
 *      2      | Bob  | ...         2     | Spring Boot      1      |     2
 *                                  3     | DB Design        2      |     1
 *                                                           2      |     3
 *
 *  DEFAULT STRATEGIES SUMMARY (@ManyToMany):
 *  -------------------------------------------
 *  Aspect            | Default              | Recommendation
 *  ------------------|----------------------|---------------------------------
 *  Join Table Name   | entity1_entity2      | Use @JoinTable with explicit name
 *  FK Ownership      | Side with @JoinTable | Student (this class)
 *  Fetch Type        | LAZY                 | Keep LAZY (EAGER is dangerous)
 *  Cascade           | NONE                 | At most PERSIST/MERGE, NOT REMOVE
 *  Sync Rule         | Manual               | Use helper methods always
 *
 * ================================================================
 */
@Entity(name = "MtmBiStudent")          // Unique JPA entity name
@Table(name = "mtm_bi_students")
@Data
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    // ============================================================
    //  MANY-TO-MANY MAPPING (BIDIRECTIONAL - OWNING SIDE)
    // ============================================================
    //
    //  Student is the OWNING side (has @JoinTable)
    //  Course is the INVERSE side (has mappedBy = "courses")
    //
    //  @JoinTable:
    //    name = "mtm_bi_student_courses"   : join table name
    //    joinColumns = student_id          : FK to THIS entity (Student)
    //    inverseJoinColumns = course_id    : FK to related entity (Course)
    //
    //  cascade = {PERSIST, MERGE}:
    //    -> When student is saved, new courses in the list are also saved
    //    -> NOT using REMOVE: deleting a student should NOT delete courses!
    //
    //  fetch = LAZY (default for @ManyToMany):
    //    -> Courses list NOT loaded until student.getCourses() is called
    //    -> Critical for performance (avoid loading all courses with every student)
    //
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
        name = "mtm_bi_student_courses",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses = new ArrayList<>();

    // ============================================================
    //  HELPER METHODS - Keep BOTH sides in sync (Best Practice)
    // ============================================================
    //
    //  enrollInCourse() sets:
    //    1. student.courses list (OWNING side - will write to join table)
    //    2. course.students list (INVERSE side - in-memory navigation)
    //
    //  This ensures both sides are consistent in memory AND persisted to DB.
    //
    public void enrollInCourse(Course course) {
        this.courses.add(course);
        course.getStudents().add(this);    // Keep inverse side in sync!
    }

    public void dropCourse(Course course) {
        this.courses.remove(course);
        course.getStudents().remove(this); // Keep inverse side in sync!
    }

    public Student(String name, String email) {
        this.name = name;
        this.email = email;
    }

    @Override
    public String toString() {
        // Note: Not printing courses here to avoid infinite recursion
        return "Student{studentId=" + studentId + ", name='" + name
                + "', email='" + email + "', courseCount=" + courses.size() + "}";
    }
}
