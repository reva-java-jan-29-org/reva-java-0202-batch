package com.training.manytomany;

import com.training.manytomany.unidirectional.Course;
import com.training.manytomany.unidirectional.CourseRepository;
import com.training.manytomany.unidirectional.Student;
import com.training.manytomany.unidirectional.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ================================================================
 *  TESTS: ManyToMany UNIDIRECTIONAL
 * ================================================================
 *
 *  WHAT THESE TESTS DEMONSTRATE:
 *  -------------------------------
 *  1. Join table auto-created by JPA (@JoinTable config)
 *  2. Student is the owning side (defines join table)
 *  3. Course has no knowledge of students (unidirectional)
 *  4. cascade = PERSIST+MERGE -> saving student saves new courses
 *  5. Dropping a course from student removes join table entry only
 *  6. Course is NOT deleted when student's enrollment is removed
 *
 *  DATABASE STRUCTURE:
 *  --------------------
 *  mtm_uni_students     mtm_uni_courses      mtm_uni_student_courses
 *  (student data)       (course data)        (join table - FK pairs)
 *
 * ================================================================
 */
@DataJpaTest
@EntityScan("com.training.manytomany.unidirectional")
@EnableJpaRepositories("com.training.manytomany.unidirectional")
@DisplayName("ManyToMany Unidirectional Tests")
class ManyToManyUnidirectionalTests {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ============================================================
    //  TEST 1: Enroll Student in multiple Courses (join table entries)
    // ============================================================
    @Test
    @DisplayName("Enroll Student in multiple Courses - join table entries created")
    void enrollStudent_inMultipleCourses_createsJoinTableEntries() {
        // Arrange: create courses first
        Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));
        Course spring = courseRepository.save(new Course("Spring Boot", "Prof. Gupta", 6));
        Course db = courseRepository.save(new Course("Database Design", "Prof. Patel", 4));

        // Create student and enroll in courses
        Student alice = new Student("Alice", "alice@school.com");
        alice.enrollInCourse(java);
        alice.enrollInCourse(spring);
        alice.enrollInCourse(db);

        // Act - cascade = PERSIST saves course entries in join table
        Student saved = studentRepository.save(alice);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Student fetched = studentRepository.findById(saved.getStudentId()).orElseThrow();
        assertThat(fetched.getCourses()).hasSize(3);
        assertThat(fetched.getCourses())
                .extracting(Course::getCourseName)
                .containsExactlyInAnyOrder("Java Programming", "Spring Boot", "Database Design");

        System.out.println("\n[TEST] Student: " + fetched.getName());
        System.out.println("[TEST] Enrolled courses: " + fetched.getCourses().size());
        fetched.getCourses().forEach(c -> System.out.println("  -> " + c));
        System.out.println("[TEST] Join table (mtm_uni_student_courses) has 3 entries for this student");
    }

    // ============================================================
    //  TEST 2: Multiple Students enrolled in same Course
    // ============================================================
    @Test
    @DisplayName("Multiple Students can enroll in the same Course - shared join table entry")
    void multipleStudents_enrollInSameCourse() {
        // Create one shared course
        Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));

        // Multiple students enroll in the same course
        Student alice = new Student("Alice", "alice@school.com");
        alice.enrollInCourse(java);

        Student bob = new Student("Bob", "bob@school.com");
        bob.enrollInCourse(java);

        studentRepository.save(alice);
        studentRepository.save(bob);
        entityManager.flush();
        entityManager.clear();

        // Query students in this course (via JPQL since Course has no students reference)
        List<Student> studentsInJava = studentRepository.findStudentsByCourseId(java.getCourseId());
        assertThat(studentsInJava).hasSize(2);
        assertThat(studentsInJava).extracting(Student::getName)
                .containsExactlyInAnyOrder("Alice", "Bob");

        System.out.println("\n[TEST] " + studentsInJava.size() + " students enrolled in Java Programming:");
        studentsInJava.forEach(s -> System.out.println("  -> " + s.getName()));
        System.out.println("[TEST] Same course shared by multiple students (join table has multiple rows for course)");
    }

    // ============================================================
    //  TEST 3: Drop a course - removes join table entry, NOT the course
    // ============================================================
    //
    //  IMPORTANT: Removing a course from student.courses list:
    //  - Deletes the entry in mtm_uni_student_courses join table
    //  - Does NOT delete the course from mtm_uni_courses table
    //  - Course still exists, can be enrolled in by other students
    //
    //  This is why cascade=ALL on @ManyToMany would be DANGEROUS:
    //  If cascade included REMOVE, dropping a course from one student
    //  would DELETE the course entirely!
    //
    @Test
    @DisplayName("Drop Course - removes join table entry only, Course itself is NOT deleted")
    void dropCourse_removesJoinEntry_courseStillExists() {
        // Setup
        Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));
        Course spring = courseRepository.save(new Course("Spring Boot", "Prof. Gupta", 6));
        Long javaId = java.getCourseId();
        Long springId = spring.getCourseId();

        Student alice = new Student("Alice", "alice@school.com");
        alice.enrollInCourse(java);
        alice.enrollInCourse(spring);
        Student saved = studentRepository.save(alice);
        entityManager.flush();
        entityManager.clear();

        // Drop Java course
        Student loaded = studentRepository.findById(saved.getStudentId()).orElseThrow();
        Course javaCourse = loaded.getCourses().stream()
                .filter(c -> c.getCourseId().equals(javaId))
                .findFirst().orElseThrow();
        loaded.dropCourse(javaCourse);
        studentRepository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        // Assert: student has only 1 course now
        Student reFetched = studentRepository.findById(saved.getStudentId()).orElseThrow();
        assertThat(reFetched.getCourses()).hasSize(1);
        assertThat(reFetched.getCourses().get(0).getCourseName()).isEqualTo("Spring Boot");

        // The Java course itself still exists (NOT deleted!)
        assertThat(courseRepository.findById(javaId)).isPresent();

        System.out.println("\n[TEST] Dropped Java course from Alice's enrollment");
        System.out.println("[TEST] Alice now has " + reFetched.getCourses().size() + " course(s)");
        System.out.println("[TEST] Java course still exists in courses table (join entry deleted only)");
        System.out.println("[TEST] This is why cascade=REMOVE is DANGEROUS on @ManyToMany!");
    }

    // ============================================================
    //  TEST 4: Unidirectional limitation - Course doesn't know students
    // ============================================================
    @Test
    @DisplayName("Unidirectional limitation - Course cannot navigate to Students directly")
    void unidirectional_courseHasNoStudentReference() {
        Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));

        Student alice = new Student("Alice", "alice@school.com");
        Student bob = new Student("Bob", "bob@school.com");
        alice.enrollInCourse(java);
        bob.enrollInCourse(java);
        studentRepository.save(alice);
        studentRepository.save(bob);
        entityManager.flush();
        entityManager.clear();

        // Course has NO students reference (unidirectional)
        Course fetchedCourse = courseRepository.findById(java.getCourseId()).orElseThrow();
        // fetchedCourse.getStudents()  <- This method does NOT exist!

        // To find students for a course, use JPQL query:
        List<Student> enrolled = studentRepository.findStudentsByCourseId(java.getCourseId());
        assertThat(enrolled).hasSize(2);

        System.out.println("\n[TEST] Course class has no students reference (unidirectional)");
        System.out.println("[TEST] Use JPQL to find students enrolled in a course: " + enrolled.size() + " found");
        System.out.println("[TEST] In bidirectional, course.getStudents() would work directly!");
    }

    // ============================================================
    //  TEST 5: Cascade PERSIST - save new courses with student
    // ============================================================
    @Test
    @DisplayName("CascadeType.PERSIST - new Courses saved when Student is saved")
    void cascadePersist_newCoursesPersistedWithStudent() {
        // New courses (not yet saved)
        Course java = new Course("Java Programming", "Prof. Sharma", 8);
        Course spring = new Course("Spring Boot", "Prof. Gupta", 6);

        // Student with unsaved courses (cascade = PERSIST will save them)
        Student alice = new Student("Alice", "alice@school.com");
        alice.enrollInCourse(java);    // java not yet in DB
        alice.enrollInCourse(spring);  // spring not yet in DB

        // Saving student cascades PERSIST to courses (and join table)
        Student saved = studentRepository.save(alice);
        entityManager.flush();
        entityManager.clear();

        // Both courses should now be in DB
        assertThat(courseRepository.count()).isEqualTo(2);
        assertThat(studentRepository.findById(saved.getStudentId()).get().getCourses()).hasSize(2);

        System.out.println("\n[TEST] Saved Student with 2 new courses via CascadeType.PERSIST");
        System.out.println("[TEST] Courses count in DB: " + courseRepository.count());
    }
}
