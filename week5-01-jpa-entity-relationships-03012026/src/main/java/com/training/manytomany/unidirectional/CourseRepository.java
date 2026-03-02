package com.training.manytomany.unidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Course (ManyToMany Unidirectional).
 *
 * Course has NO @ManyToMany annotation (unidirectional).
 * Course does not know which students are enrolled.
 *
 * To find students for a course, use StudentRepository.findStudentsByCourseId()
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByInstructor(String instructor);

    List<Course> findByDurationWeeksLessThanEqual(Integer weeks);
}
