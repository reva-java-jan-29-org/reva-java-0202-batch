package com.training.manytomany.unidirectional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ================================================================
 *  MANY-TO-MANY UNIDIRECTIONAL  -  NON-REFERENCED SIDE (Course)
 * ================================================================
 *
 *  In UNIDIRECTIONAL @ManyToMany:
 *    - Student ---> [Course, ...] (Student knows Courses)
 *    - Course has NO reference back to Students
 *
 *  Course is a standalone entity with NO @ManyToMany annotation.
 *  A course can be enrolled in by many students.
 *  A student can enroll in many courses.
 *
 *  DATABASE TABLE  (mtm_uni_courses):
 *  ------------------------------------
 *  course_id  |  course_name          |  instructor
 *      1      |  Java Programming     |  Prof. Sharma
 *      2      |  Spring Boot          |  Prof. Gupta
 *      3      |  Database Design      |  Prof. Patel
 *
 *  JOIN TABLE  (mtm_uni_student_courses):
 *  -----------------------------------------
 *  student_id  |  course_id
 *      1       |      1         <- Student 1 enrolled in Course 1
 *      1       |      2         <- Student 1 enrolled in Course 2
 *      2       |      1         <- Student 2 enrolled in Course 1
 *      2       |      3         <- Student 2 enrolled in Course 3
 *
 * ================================================================
 */
@Entity(name = "MtmUniCourse")          // Unique JPA entity name
@Table(name = "mtm_uni_courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "instructor")
    private String instructor;

    @Column(name = "duration_weeks")
    private Integer durationWeeks;

    // No @ManyToMany here - this is UNIDIRECTIONAL
    // Course has no knowledge of which students are enrolled

    public Course(String courseName, String instructor, Integer durationWeeks) {
        this.courseName = courseName;
        this.instructor = instructor;
        this.durationWeeks = durationWeeks;
    }

    @Override
    public String toString() {
        return "Course{courseId=" + courseId + ", courseName='" + courseName
                + "', instructor='" + instructor + "', durationWeeks=" + durationWeeks + "}";
    }
}
