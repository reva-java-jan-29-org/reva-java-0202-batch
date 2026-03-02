package com.training.manytomany.unidirectional;

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
 *  MANY-TO-MANY UNIDIRECTIONAL  -  OWNING SIDE (Student)
 * ================================================================
 *
 *  Scenario: MANY Students enrolled in MANY Courses.
 *            Navigation is ONE-WAY: Student ---> [Course, ...]
 *            Course has NO reference back to Students.
 *
 *  KEY CONCEPT - JOIN TABLE:
 *  --------------------------
 *  In @ManyToMany, there is NO FK in either entity table.
 *  Instead, JPA creates a separate JOIN TABLE that holds:
 *    - FK to Student table (student_id)
 *    - FK to Course table  (course_id)
 *
 *  The join table represents the relationship between the two entities.
 *
 *  TWO WAYS TO DEFINE THE JOIN TABLE:
 *  ------------------------------------
 *  Option A: WITHOUT @JoinTable (JPA auto-generates table name)
 *  -------------------------------------------------------------
 *    @ManyToMany
 *    private List<Course> courses;
 *
 *    -> JPA creates a join table named: student_courses (or similar)
 *    -> Column names: student_emp_id, courses_course_id (auto-generated, ugly)
 *    -> NOT recommended - use @JoinTable for clarity
 *
 *  Option B: WITH @JoinTable (RECOMMENDED)
 *  -----------------------------------------
 *    @ManyToMany
 *    @JoinTable(
 *        name = "mtm_uni_student_courses",        // explicit join table name
 *        joinColumns = @JoinColumn(name = "student_id"),      // FK to THIS entity
 *        inverseJoinColumns = @JoinColumn(name = "course_id") // FK to OTHER entity
 *    )
 *    private List<Course> courses;
 *
 *  DATABASE TABLES:
 *  -----------------
 *  mtm_uni_students:           mtm_uni_courses:        mtm_uni_student_courses:
 *  student_id | name | email   course_id | course_name  student_id | course_id
 *      1      | Alice| a@x.com     1     | Java Prog.       1      |     1
 *      2      | Bob  | b@x.com     2     | Spring Boot      1      |     2
 *                                  3     | DB Design        2      |     1
 *                                                           2      |     3
 *
 *  DEFAULT STRATEGIES SUMMARY (@ManyToMany):
 *  -------------------------------------------
 *  Aspect            | Default              | Meaning
 *  ------------------|----------------------|--------------------------------
 *  Join Table        | auto-generated name  | Use @JoinTable for control
 *  FK Ownership      | Declaring side       | Student owns (has @JoinTable)
 *  Fetch Type        | LAZY                 | Courses not loaded with Student
 *  Cascade           | NONE                 | Must save courses separately
 *
 *  RECOMMENDATIONS:
 *  -----------------
 *  @ManyToMany(fetch = FetchType.LAZY)     // default, keep as LAZY
 *  @JoinTable(                              // always name the join table explicitly
 *      name = "join_table_name",
 *      joinColumns = @JoinColumn(name = "student_id"),
 *      inverseJoinColumns = @JoinColumn(name = "course_id")
 *  )
 *
 *  IMPORTANT: Do NOT use CascadeType.REMOVE or orphanRemoval in @ManyToMany!
 *  If you delete a Student, you don't want to delete the Courses too!
 *  Cascade is typically NONE or at most PERSIST/MERGE for @ManyToMany.
 *
 * ================================================================
 */
@Entity(name = "MtmUniStudent")         // Unique JPA entity name
@Table(name = "mtm_uni_students")
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
    //  MANY-TO-MANY MAPPING (UNIDIRECTIONAL - OWNING SIDE)
    // ============================================================
    //
    //  @ManyToMany: MANY students enrolled in MANY courses
    //
    //  @JoinTable: Defines the join table structure
    //    name = "mtm_uni_student_courses"  : explicit join table name
    //    joinColumns = student_id          : FK pointing to THIS entity (Student)
    //    inverseJoinColumns = course_id    : FK pointing to Course entity
    //
    //  cascade = PERSIST:
    //    -> If student is saved, newly added courses are also persisted
    //    -> NOT using ALL because deleting a student should NOT delete courses
    //    -> Courses are shared entities - other students may be enrolled too!
    //
    //  fetch = LAZY (default for @ManyToMany):
    //    -> Courses are NOT loaded when Student is fetched
    //    -> Loaded only when student.getCourses() is accessed
    //
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
        name = "mtm_uni_student_courses",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses = new ArrayList<>();

    // Helper methods
    public void enrollInCourse(Course course) {
        this.courses.add(course);
    }

    public void dropCourse(Course course) {
        this.courses.remove(course);
    }

    public Student(String name, String email) {
        this.name = name;
        this.email = email;
    }

    @Override
    public String toString() {
        return "Student{studentId=" + studentId + ", name='" + name + "', email='" + email
                + "', enrolledCourses=" + courses.size() + "}";
    }
}
