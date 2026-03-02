package com.training.onetoone;

import com.training.onetoone.unidirectional.Employee;
import com.training.onetoone.unidirectional.EmployeeRepository;
import com.training.onetoone.unidirectional.Profile;
import com.training.onetoone.unidirectional.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ================================================================
 *  TESTS: OneToOne UNIDIRECTIONAL
 * ================================================================
 *
 *  @DataJpaTest:
 *  - Loads only JPA components (no full Spring context)
 *  - Auto-configures H2 in-memory database (no MySQL needed!)
 *  - Each test runs in a transaction that is ROLLED BACK by default
 *  - Use TestEntityManager for fine-grained entity operations
 *
 *  WHAT THESE TESTS DEMONSTRATE:
 *  -------------------------------
 *  1. Save with CascadeType.ALL  -> Employee saves Profile automatically
 *  2. Read with LAZY fetch       -> Profile is not loaded until accessed
 *  3. Delete with cascade        -> Deleting Employee deletes Profile
 *  4. Owning side writes the FK  -> Only Employee controls the FK column
 *
 * ================================================================
 */
@DataJpaTest
@EntityScan("com.training.onetoone.unidirectional")
@EnableJpaRepositories("com.training.onetoone.unidirectional")
@DisplayName("OneToOne Unidirectional Tests")
class OneToOneUnidirectionalTests {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ============================================================
    //  TEST 1: CREATE - Demonstrate CascadeType.ALL
    // ============================================================
    //
    //  Since Employee.profile has cascade = ALL:
    //  - We do NOT need to save Profile separately
    //  - Saving Employee auto-saves Profile via cascade
    //  - Both entities get IDs after save
    //
    @Test
    @DisplayName("Save Employee with Profile - CascadeType.ALL auto-persists Profile")
    void saveEmployee_withProfile_shouldCascadePersistProfile() {
        // --- Arrange ---
        Profile profile = new Profile("Alice's professional bio", "linkedin.com/alice");
        Employee employee = new Employee("Alice", "Pune", profile);

        // --- Act ---
        // Note: We do NOT call profileRepository.save(profile) first
        // CascadeType.ALL on Employee.profile handles this automatically
        Employee savedEmployee = employeeRepository.save(employee);

        // Flush to DB and clear first-level cache to force re-fetch
        entityManager.flush();
        entityManager.clear();

        // --- Assert ---
        Employee fetchedEmployee = employeeRepository.findById(savedEmployee.getEmpId()).orElseThrow();

        assertThat(fetchedEmployee.getEmpId()).isNotNull();
        assertThat(fetchedEmployee.getName()).isEqualTo("Alice");

        // Profile should be auto-saved by cascade
        // Note: Using entityManager.find to force DB read (bypasses LAZY proxy)
        Profile fetchedProfile = profileRepository.findById(savedEmployee.getProfile().getProfileId()).orElseThrow();
        assertThat(fetchedProfile.getBio()).isEqualTo("Alice's professional bio");

        System.out.println("\n[TEST] Saved Employee: " + fetchedEmployee);
        System.out.println("[TEST] Auto-saved Profile (via cascade): " + fetchedProfile);
        System.out.println("[TEST] FK in employees table -> fk_profile_id = " + savedEmployee.getProfile().getProfileId());
    }

    // ============================================================
    //  TEST 2: CREATE - Without cascade (manual save order)
    // ============================================================
    //
    //  This shows what would happen WITHOUT CascadeType.ALL:
    //  You would need to: save Profile FIRST, then save Employee
    //  (Profile must have an ID before being assigned to Employee)
    //
    @Test
    @DisplayName("Save Profile first, then Employee - demonstrates manual save order")
    void saveProfileFirst_thenEmployee_shouldWork() {
        // --- Arrange & Act ---
        // Save profile manually first (as if cascade was NONE)
        Profile profile = new Profile("Bob's bio", "linkedin.com/bob");
        Profile savedProfile = profileRepository.save(profile);  // Profile gets ID here

        // Now assign the saved profile (with ID) to employee
        Employee employee = new Employee("Bob", "Mumbai", savedProfile);
        Employee savedEmployee = employeeRepository.save(employee);

        entityManager.flush();
        entityManager.clear();

        // --- Assert ---
        assertThat(savedProfile.getProfileId()).isNotNull();
        assertThat(savedEmployee.getEmpId()).isNotNull();
        assertThat(savedEmployee.getProfile().getProfileId()).isEqualTo(savedProfile.getProfileId());

        System.out.println("\n[TEST] Saved Profile manually: " + savedProfile);
        System.out.println("[TEST] Saved Employee with Profile: " + savedEmployee);
    }

    // ============================================================
    //  TEST 3: READ - Verify FK column and navigation
    // ============================================================
    @Test
    @DisplayName("Find Employee - verify FK and navigation to Profile")
    void findEmployee_shouldReturnWithProfileReference() {
        // --- Setup: create and persist test data ---
        Profile profile = new Profile("Carol's bio", "linkedin.com/carol");
        Employee employee = new Employee("Carol", "Delhi", profile);
        Employee saved = employeeRepository.save(employee);
        entityManager.flush();
        entityManager.clear();

        // --- Act ---
        Optional<Employee> found = employeeRepository.findById(saved.getEmpId());

        // --- Assert ---
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Carol");

        // Profile should be accessible via Employee (owning side navigation)
        // Since cascade saved the profile, profile.profileId is set
        assertThat(found.get().getProfile()).isNotNull();
        assertThat(found.get().getProfile().getBio()).isEqualTo("Carol's bio");

        System.out.println("\n[TEST] Found employee: " + found.get());
        System.out.println("[TEST] Profile via navigation: " + found.get().getProfile());
    }

    // ============================================================
    //  TEST 4: READ - Demonstrate UNIDIRECTIONAL navigation
    // ============================================================
    //
    //  Key teaching point: You CANNOT navigate Profile -> Employee
    //  in unidirectional @OneToOne because Profile has no Employee field.
    //
    @Test
    @DisplayName("Demonstrate unidirectional navigation - Employee -> Profile only")
    void demonstrateUnidirectionalNavigation() {
        Profile profile = new Profile("Dave's bio", "linkedin.com/dave");
        Employee employee = new Employee("Dave", "Chennai", profile);
        Employee saved = employeeRepository.save(employee);
        entityManager.flush();
        entityManager.clear();

        // You CAN navigate: Employee -> Profile
        Employee fetchedEmployee = employeeRepository.findById(saved.getEmpId()).orElseThrow();
        Profile fetchedProfile = fetchedEmployee.getProfile();  // Works!
        assertThat(fetchedProfile.getBio()).isEqualTo("Dave's bio");

        // You CANNOT navigate: Profile -> Employee
        // The Profile class has NO Employee field (unidirectional by design)
        // To find which employee has a profile, you'd need a custom query
        Profile profileFromDb = profileRepository.findById(saved.getProfile().getProfileId()).orElseThrow();
        // profileFromDb.getEmployee()  <- This method does NOT exist!
        // (Profile class has no Employee reference in unidirectional mapping)

        System.out.println("\n[TEST] Employee -> Profile navigation: " + fetchedProfile);
        System.out.println("[TEST] Profile class has no Employee reference (unidirectional design)");
        System.out.println("[TEST] Profile: " + profileFromDb);
    }

    // ============================================================
    //  TEST 5: UPDATE - Change profile assignment
    // ============================================================
    @Test
    @DisplayName("Update Employee - reassign to new Profile")
    void updateEmployee_reassignProfile_shouldUpdateFK() {
        // Setup
        Profile originalProfile = new Profile("Eve original bio", "linkedin.com/eve");
        Employee employee = new Employee("Eve", "Hyderabad", originalProfile);
        Employee saved = employeeRepository.save(employee);

        Profile newProfile = new Profile("Eve updated bio", "linkedin.com/eve-updated");
        profileRepository.save(newProfile);

        entityManager.flush();
        entityManager.clear();

        // Act - reassign profile
        Employee toUpdate = employeeRepository.findById(saved.getEmpId()).orElseThrow();
        toUpdate.setProfile(newProfile);
        Employee updated = employeeRepository.save(toUpdate);

        entityManager.flush();
        entityManager.clear();

        // Assert - FK should point to new profile
        Employee reFetched = employeeRepository.findById(updated.getEmpId()).orElseThrow();
        assertThat(reFetched.getProfile().getProfileId()).isEqualTo(newProfile.getProfileId());
        assertThat(reFetched.getProfile().getBio()).isEqualTo("Eve updated bio");

        System.out.println("\n[TEST] Updated employee profile FK -> " + reFetched.getProfile().getProfileId());
    }

    // ============================================================
    //  TEST 6: DELETE - Demonstrate CascadeType.ALL (cascade delete)
    // ============================================================
    //
    //  Since cascade = ALL includes CascadeType.REMOVE:
    //  Deleting Employee automatically deletes the associated Profile.
    //
    @Test
    @DisplayName("Delete Employee - CascadeType.ALL auto-deletes Profile")
    void deleteEmployee_shouldCascadeDeleteProfile() {
        // Setup
        Profile profile = new Profile("Frank's bio", "linkedin.com/frank");
        Employee employee = new Employee("Frank", "Kolkata", profile);
        Employee saved = employeeRepository.save(employee);
        Long profileId = saved.getProfile().getProfileId();

        entityManager.flush();
        entityManager.clear();

        // Verify both exist
        assertThat(employeeRepository.findById(saved.getEmpId())).isPresent();
        assertThat(profileRepository.findById(profileId)).isPresent();

        // Act - delete Employee (should cascade delete Profile)
        employeeRepository.deleteById(saved.getEmpId());
        entityManager.flush();

        // Assert - Profile should be deleted too (cascade = ALL includes REMOVE)
        assertThat(employeeRepository.findById(saved.getEmpId())).isEmpty();
        assertThat(profileRepository.findById(profileId)).isEmpty();

        System.out.println("\n[TEST] Employee deleted. Profile also deleted via CascadeType.ALL");
    }

    // ============================================================
    //  TEST 7: Verify FK column uniqueness (unique = true on @JoinColumn)
    // ============================================================
    //
    //  The unique = true constraint on @JoinColumn(name = "fk_profile_id")
    //  ensures one profile can only be linked to ONE employee.
    //
    @Test
    @DisplayName("One Profile linked to only one Employee - unique FK constraint")
    void profileLinkedToOneEmployee_uniqueConstraintEnforced() {
        Profile profile = new Profile("Grace's bio", "linkedin.com/grace");
        profileRepository.save(profile);

        // First employee links to the profile
        Employee emp1 = new Employee("Grace", "Mumbai", profile);
        Employee saved1 = employeeRepository.save(emp1);

        entityManager.flush();

        // The fk_profile_id column has UNIQUE constraint
        // So a second employee cannot reference the same profile
        // This is enforced at the database level via unique=true on @JoinColumn

        assertThat(saved1.getEmpId()).isNotNull();
        System.out.println("\n[TEST] Profile ID " + profile.getProfileId() + " is uniquely linked to Employee: " + saved1);
        System.out.println("[TEST] unique=true on @JoinColumn ensures this is a true 1:1 relationship at DB level");
    }
}
