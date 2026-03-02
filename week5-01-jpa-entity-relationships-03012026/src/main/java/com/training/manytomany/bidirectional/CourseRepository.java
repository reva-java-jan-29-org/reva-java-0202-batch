package com.training.manytomany.bidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Course (ManyToMany Bidirectional).
 *
 * Course is the INVERSE side (has mappedBy).
 * Course.students is for in-memory navigation only.
 *
 * With bidirectional mapping, you CAN navigate Course -> Students
 * in Java: course.getStudents()
 * And you can query courses by student using JPQL.
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseName(String courseName);

    List<Course> findByInstructor(String instructor);

    // Find courses that a specific student is enrolled in using JPQL
    // Even though Course is the inverse side, we can still join via Student's courses
    @Query("SELECT c FROM MtmBiCourse c JOIN c.students s WHERE s.studentId = :studentId")
    List<Course> findCoursesByStudentId(Long studentId);

    // Join fetch to load course with all enrolled students in one query
    @Query("SELECT DISTINCT c FROM MtmBiCourse c LEFT JOIN FETCH c.students WHERE c.courseId = :courseId")
    Optional<Course> findByIdWithStudents(Long courseId);
}
