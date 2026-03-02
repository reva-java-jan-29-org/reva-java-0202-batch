package com.training.manytomany;

import com.training.manytomany.bidirectional.Course;
import com.training.manytomany.bidirectional.CourseRepository;
import com.training.manytomany.bidirectional.Student;
import com.training.manytomany.bidirectional.StudentRepository;
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
 *  TESTS: ManyToMany BIDIRECTIONAL
 * ================================================================
 *
 *  KEY DIFFERENCES FROM UNIDIRECTIONAL:
 *  --------------------------------------
 *  1. Course HAS a students list (course.getStudents() works)
 *  2. Course is the INVERSE side (has mappedBy = "courses")
 *  3. Student is the OWNING side (has @JoinTable)
 *  4. ONLY ONE join table exists (defined on Student side)
 *  5. Must keep both sides in sync using helper methods
 *  6. Inverse side changes (course.students) do NOT write to DB
 *
 *  WHAT THESE TESTS DEMONSTRATE:
 *  -------------------------------
 *  1. Bidirectional navigation: student.getCourses() AND course.getStudents()
 *  2. Owning side controls the join table (Student.courses)
 *  3. Inverse side changes are ignored by JPA
 *  4. Helper methods for sync
 *  5. JPQL queries from both sides
 *
 * ================================================================
 */
@DataJpaTest
@EntityScan("com.training.manytomany.bidirectional")
@EnableJpaRepositories("com.training.manytomany.bidirectional")
@DisplayName("ManyToMany Bidirectional Tests")
class ManyToManyBidirectionalTests {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ============================================================
    //  TEST 1: Save and navigate BOTH directions
    // ============================================================
    @Test
    @DisplayName("Bidirectional navigation - student->courses AND course->students")
    void bidirectionalNavigation_worksBothWays() {
        // Arrange
        Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));
        Course spring = courseRepository.save(new Course("Spring Boot", "Prof. Gupta", 6));

        Student alice = new Student("Alice", "alice@school.com");
        Student bob = new Student("Bob", "bob@school.com");

        // Use helper on Student side (owning side)
        alice.enrollInCourse(java);     // sets: alice.courses.add(java) + java.students.add(alice)
        alice.enrollInCourse(spring);   // sets: alice.courses.add(spring) + spring.students.add(alice)
        bob.enrollInCourse(java);       // sets: bob.courses.add(java) + java.students.add(bob)

        studentRepository.save(alice);
        studentRepository.save(bob);
        entityManager.flush();
        entityManager.clear();

        // Direction 1: Student -> Courses (OWNING side navigation)
        Student fetchedAlice = studentRepository.findById(alice.getStudentId()).orElseThrow();
        assertThat(fetchedAlice.getCourses()).hasSize(2);
        System.out.println("\n[TEST] Alice's courses: " + fetchedAlice.getCourses().size());

        // Direction 2: Course -> Students (INVERSE side navigation)
        Course fetchedJava = courseRepository.findById(java.getCourseId()).orElseThrow();
        assertThat(fetchedJava.getStudents()).hasSize(2);
        System.out.println("[TEST] Students in Java course: " + fetchedJava.getStudents().size());

        assertThat(fetchedJava.getStudents())
                .extracting(Student::getName)
                .containsExactlyInAnyOrder("Alice", "Bob");
    }

    // ============================================================
    //  TEST 2: ONLY ONE join table (defined on owning side)
    // ============================================================
    //
    //  Even though BOTH sides have @ManyToMany annotation,
    //  only ONE join table exists: mtm_bi_student_courses
    //  This is because Course has mappedBy -> reads from Student's @JoinTable
    //
    @Test
    @DisplayName("Single join table - defined by owning side Student")
    void singleJoinTable_ownedByStudentSide() {
        Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));
        Student alice = new Student("Alice", "alice@school.com");
        alice.enrollInCourse(java);
        studentRepository.save(alice);
        entityManager.flush();
        entityManager.clear();

        // Only ONE join table exists: mtm_bi_student_courses
        // Verify relationship is accessible from both sides
        Student fetchedStudent = studentRepository.findById(alice.getStudentId()).orElseThrow();
        assertThat(fetchedStudent.getCourses()).hasSize(1);

        Course fetchedCourse = courseRepository.findById(java.getCourseId()).orElseThrow();
        assertThat(fetchedCourse.getStudents()).hasSize(1);

        System.out.println("\n[TEST] Single join table stores: student_id=" + alice.getStudentId() + ", course_id=" + java.getCourseId());
        System.out.println("[TEST] Both sides read from the SAME join table (mtm_bi_student_courses)");
        System.out.println("[TEST] Student side defined the table via @JoinTable");
        System.out.println("[TEST] Course side reads via mappedBy='courses'");
    }

    // ============================================================
    //  TEST 3: Owning side controls join table writes
    // ============================================================
    //
    //  CRITICAL RULE: Only Student.courses (owning side) writes to join table.
    //  If you only add to course.students (inverse side), it is IGNORED by JPA.
    //
    @Test
    @DisplayName("CRITICAL - Only owning side (Student.courses) writes to join table")
    void owningSideOnly_writesToJoinTable() {
        Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));
        Student alice = studentRepository.save(new Student("Alice", "alice@school.com"));
        entityManager.flush();
        entityManager.clear();

        // Load both
        Course loadedJava = courseRepository.findById(java.getCourseId()).orElseThrow();
        Student loadedAlice = studentRepository.findById(alice.getStudentId()).orElseThrow();

        // WRONG WAY: Only setting INVERSE side (Course.students)
        loadedJava.getStudents().add(loadedAlice);  // <- INVERSE side -> IGNORED by JPA
        courseRepository.save(loadedJava);           // <- This WON'T create join table entry!
        entityManager.flush();
        entityManager.clear();

        // Verify: NO join table entry was created!
        Student check = studentRepository.findById(alice.getStudentId()).orElseThrow();
        assertThat(check.getCourses()).isEmpty();  // No enrollment in DB

        System.out.println("\n[TEST] LESSON: course.students.add(student) is IGNORED by JPA");
        System.out.println("[TEST] Only student.courses.add(course) writes to join table");
        System.out.println("[TEST] Alice's courses (join table): " + check.getCourses().size() + " (expected 0)");

        // CORRECT WAY: Set owning side (Student.courses)
        Student reFetchedAlice = studentRepository.findById(alice.getStudentId()).orElseThrow();
        Course reFetchedJava = courseRepository.findById(java.getCourseId()).orElseThrow();
        reFetchedAlice.enrollInCourse(reFetchedJava);  // helper sets BOTH sides
        studentRepository.save(reFetchedAlice);        // NOW join table entry is created
        entityManager.flush();
        entityManager.clear();

        Student finalCheck = studentRepository.findById(alice.getStudentId()).orElseThrow();
        assertThat(finalCheck.getCourses()).hasSize(1);  // Now enrolled!
        System.out.println("[TEST] After correct way (student.enrollInCourse): " + finalCheck.getCourses().size() + " course(s)");
    }

    // ============================================================
    //  TEST 4: Helper method - keeps both sides in sync
    // ============================================================
    @Test
    @DisplayName("Helper method enrollInCourse - keeps both sides in sync")
    void enrollInCourseHelper_keepsBothSidesSynced() {
        Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));
        Student alice = new Student("Alice", "alice@school.com");

        // Use helper: enrollInCourse sets BOTH sides
        alice.enrollInCourse(java);

        // In-memory check: BOTH sides are set immediately (no DB read needed)
        assertThat(alice.getCourses()).contains(java);        // owning side ✓
        assertThat(java.getStudents()).contains(alice);       // inverse side ✓ (in-memory)

        studentRepository.save(alice);
        entityManager.flush();
        entityManager.clear();

        // DB check: both sides load correctly
        Student fetchedAlice = studentRepository.findById(alice.getStudentId()).orElseThrow();
        Course fetchedJava = courseRepository.findById(java.getCourseId()).orElseThrow();

        assertThat(fetchedAlice.getCourses()).hasSize(1);
        assertThat(fetchedJava.getStudents()).hasSize(1);

        System.out.println("\n[TEST] Helper method kept both sides in sync:");
        System.out.println("[TEST] alice.getCourses() = " + fetchedAlice.getCourses().size());
        System.out.println("[TEST] java.getStudents() = " + fetchedJava.getStudents().size());
    }

    // ============================================================
    //  TEST 5: JPQL queries from both sides
    // ============================================================
    @Test
    @DisplayName("JPQL queries work from both Student and Course sides")
    void jpqlQueries_workFromBothSides() {
        Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));
        Course spring = courseRepository.save(new Course("Spring Boot", "Prof. Gupta", 6));

        Student alice = new Student("Alice", "alice@school.com");
        alice.enrollInCourse(java);
        alice.enrollInCourse(spring);
        studentRepository.save(alice);

        Student bob = new Student("Bob", "bob@school.com");
        bob.enrollInCourse(java);
        studentRepository.save(bob);

        entityManager.flush();
        entityManager.clear();

        // Query from Student side: "Who is enrolled in Java?"
        List<Student> javaStudents = studentRepository.findStudentsByCourseId(java.getCourseId());
        assertThat(javaStudents).hasSize(2);

        // Query from Course side: "What courses is Alice in?"
        List<Course> aliceCourses = courseRepository.findCoursesByStudentId(alice.getStudentId());
        assertThat(aliceCourses).hasSize(2);

        System.out.println("\n[TEST] JPQL from Student side - Students in Java: " + javaStudents.size());
        System.out.println("[TEST] JPQL from Course side - Alice's courses: " + aliceCourses.size());
        System.out.println("[TEST] Bidirectional enables JPQL navigation from both sides!");
    }

    // ============================================================
    //  TEST 6: Delete student - courses remain (no cascade delete)
    // ============================================================
    //
    //  Since cascade does NOT include REMOVE on @ManyToMany:
    //  - Deleting Student removes the student and its join table entries
    //  - Courses are NOT deleted (they are shared, independent entities)
    //
    @Test
    @DisplayName("Delete Student - courses NOT deleted (cascade does not include REMOVE)")
    void deleteStudent_doesNotDeleteCourses() {
        Course java = courseRepository.save(new Course("Java Programming", "Prof. Sharma", 8));
        Course spring = courseRepository.save(new Course("Spring Boot", "Prof. Gupta", 6));

        Student alice = new Student("Alice", "alice@school.com");
        alice.enrollInCourse(java);
        alice.enrollInCourse(spring);
        Student saved = studentRepository.save(alice);
        entityManager.flush();
        entityManager.clear();

        // Delete student (join table entries should be removed, courses preserved)
        // Need to clear courses from the owning side first to avoid constraint violations
        Student toDelete = studentRepository.findById(saved.getStudentId()).orElseThrow();
        toDelete.getCourses().clear();  // remove join table entries first
        studentRepository.save(toDelete);
        studentRepository.delete(toDelete);
        entityManager.flush();

        // Courses still exist!
        assertThat(courseRepository.findById(java.getCourseId())).isPresent();
        assertThat(courseRepository.findById(spring.getCourseId())).isPresent();
        assertThat(studentRepository.findById(saved.getStudentId())).isEmpty();

        System.out.println("\n[TEST] Deleted student Alice");
        System.out.println("[TEST] Java course still exists: " + courseRepository.findById(java.getCourseId()).isPresent());
        System.out.println("[TEST] Spring Boot course still exists: " + courseRepository.findById(spring.getCourseId()).isPresent());
        System.out.println("[TEST] LESSON: Never use cascade=REMOVE on @ManyToMany!");
    }
}
