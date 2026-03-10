package com.training.manytomany.bidirectional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * ================================================================
 *  MANY-TO-MANY BIDIRECTIONAL  -  INVERSE (NON-OWNING) SIDE (Course)
 * ================================================================
 *
 *  Scenario: Student <---> Course (both know about each other)
 *            Navigation is TWO-WAY: Student <-> Course
 *
 *  Course is the INVERSE (non-owning) side because it has mappedBy.
 *  Student is the OWNING side because it has @JoinTable.
 *
 *  mappedBy = "courses"  ->  "The 'courses' field in Student class
 *                             owns this relationship (has @JoinTable)"
 *
 *  WHAT mappedBy MEANS IN @ManyToMany:
 *  -------------------------------------
 *  - Course does NOT define the join table (that's Student's job)
 *  - JPA reads the join table definition from the Student.courses field
 *  - Only one side should define the @JoinTable (the owning side = Student)
 *  - If BOTH sides had @JoinTable, JPA would create TWO separate join tables!
 *
 *  RULE: In @ManyToMany bidirectional:
 *    - ONE side has @JoinTable -> OWNING side
 *    - OTHER side has mappedBy -> INVERSE side
 *    - ONLY ONE join table exists in the database
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
 *  KEEPING BOTH SIDES IN SYNC:
 *  ----------------------------
 *  In bidirectional @ManyToMany, always set both sides:
 *    student.getCourses().add(course);      <- Student knows about Course
 *    course.getStudents().add(student);     <- Course knows about Student (in-memory)
 *
 *  Use the helper method addStudent() on Course side (or addCourse() on Student side).
 *
 *  IMPORTANT - NEVER USE CascadeType.REMOVE on @ManyToMany:
 *  ----------------------------------------------------------
 *  If you delete a Course, you don't want to delete all the Students!
 *  Students are independent entities. Only cascade PERSIST/MERGE at most.
 *
 * ================================================================
 */
@Entity(name = "MtmBiCourse")           // Unique JPA entity name
@Table(name = "mtm_bi_courses")
@Data
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;		//default value for ref types is always null, jpa uses null naturally to mean that the "identifer is not assigned yet"

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "instructor")
    private String instructor;

    @Column(name = "duration_weeks")
    private Integer durationWeeks;

    // ============================================================
    //  MANY-TO-MANY MAPPING (BIDIRECTIONAL - INVERSE / NON-OWNING SIDE)
    // ============================================================
    //
    //  mappedBy = "courses":
    //    -> "The 'courses' field in Student class owns this relationship"
    //    -> Course does NOT have @JoinTable
    //    -> Join table is defined by Student.courses field
    //    -> JPA reads join table structure from Student side
    //    -> This field is for in-memory navigation only
    //
    //  No cascade here:
    //    -> Deleting a Course should NOT delete enrolled Students
    //    -> Students are independent entities!
    //
    @ManyToMany(mappedBy = "courses")
    private List<Student> students = new ArrayList<>();

    // ============================================================
    //  HELPER METHOD - Keep both sides in sync
    // ============================================================
    public void addStudent(Student student) {
        this.students.add(student);
        student.getCourses().add(this);    // Keep owning side in sync!
    }

    public void removeStudent(Student student) {
        this.students.remove(student);
        student.getCourses().remove(this); // Keep owning side in sync!
    }

    public Course(String courseName, String instructor, Integer durationWeeks) {
        this.courseName = courseName;
        this.instructor = instructor;
        this.durationWeeks = durationWeeks;
    }

    @Override
    public String toString() {
        // Note: Not printing students here to avoid infinite recursion
        return "Course{courseId=" + courseId + ", courseName='" + courseName
                + "', instructor='" + instructor + "', enrolledStudents=" + students.size() + "}";
    }
}
