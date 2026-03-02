package com.training.manytomany.bidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Student (ManyToMany Bidirectional).
 *
 * Student is the OWNING side (has @JoinTable).
 * Saving Student with courses enrolled writes to the join table.
 *
 * In bidirectional mapping, you can navigate both ways:
 * - student.getCourses() -> List<Course>
 * - course.getStudents() -> List<Student>
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    // Find students enrolled in a specific course using JPQL
    // Works because Student has @ManyToMany (owning side)
    @Query("SELECT s FROM MtmBiStudent s JOIN s.courses c WHERE c.courseId = :courseId")
    List<Student> findStudentsByCourseId(Long courseId);

    // Join fetch to load students with all their courses in one query
    @Query("SELECT DISTINCT s FROM MtmBiStudent s LEFT JOIN FETCH s.courses WHERE s.studentId = :studentId")
    Optional<Student> findByIdWithCourses(Long studentId);
}
