package com.training.manytomany.unidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Student (ManyToMany Unidirectional).
 *
 * Student is the OWNING side (has @JoinTable).
 * Saving a student with courses set will persist the join table entries.
 *
 * Since the relationship is unidirectional (Student -> Course),
 * you cannot query: "Which students are in a given course?" using JPA navigation.
 * For that, use JPQL query with JOIN.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    List<Student> findByName(String name);

    // Query to find students enrolled in a specific course (using join table)
    @Query("SELECT s FROM MtmUniStudent s JOIN s.courses c WHERE c.courseId = :courseId")
    List<Student> findStudentsByCourseId(Long courseId);
}
