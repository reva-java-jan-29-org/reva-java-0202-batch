package com.training.onetoone;

import com.training.onetoone.bidirectional.Employee;
import com.training.onetoone.bidirectional.EmployeeRepository;
import com.training.onetoone.bidirectional.Profile;
import com.training.onetoone.bidirectional.ProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ================================================================
 *  TESTS: OneToOne BIDIRECTIONAL
 * ================================================================
 *
 *  KEY DIFFERENCES FROM UNIDIRECTIONAL:
 *  --------------------------------------
 *  1. BOTH entities have @OneToOne annotation
 *  2. Employee = OWNING side (has @JoinColumn, has FK)
 *  3. Profile  = INVERSE side (has mappedBy, no FK)
 *  4. Must set BOTH sides for in-memory consistency
 *  5. profile.getEmployee() now works (bidirectional navigation)
 *
 *  WHAT THESE TESTS DEMONSTRATE:
 *  -------------------------------
 *  1. Setting both sides via helper method vs manually
 *  2. In-memory stale state when only one side is set
 *  3. Bidirectional navigation Employee <-> Profile
 *  4. mappedBy means Profile's employee field is ignored by JPA
 *
 * ================================================================
 */
@DataJpaTest
@EntityScan("com.training.onetoone.bidirectional")
@EnableJpaRepositories("com.training.onetoone.bidirectional")
@DisplayName("OneToOne Bidirectional Tests")
class OneToOneBidirectionalTests {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ============================================================
    //  TEST 1: Save using helper method (setProfileWithSync)
    // ============================================================
    //
    //  BEST PRACTICE: Always use the helper method to set both sides.
    //  setProfileWithSync() sets:
    //    1. employee.profile = profile     (owning side - writes FK)
    //    2. profile.employee = employee    (inverse side - in-memory navigation)
    //
    @Test
    @DisplayName("Save with helper method - both sides set correctly")
    void saveEmployee_usingHelperMethod_bothSidesSynced() {
        // --- Arrange ---
        Employee employee = new Employee("Alice", "Pune");
        Profile profile = new Profile("Alice's detailed bio", "linkedin.com/alice");

        // Use helper method to set both sides at once
        employee.setProfileWithSync(profile);   // <- Best Practice!

        // --- Act ---
        Employee saved = employeeRepository.save(employee);
        entityManager.flush();
        entityManager.clear();

        // --- Assert ---
        Employee fetchedEmployee = employeeRepository.findById(saved.getEmpId()).orElseThrow();

        // Employee -> Profile navigation (owning side)
        assertThat(fetchedEmployee.getProfile()).isNotNull();
        assertThat(fetchedEmployee.getProfile().getBio()).isEqualTo("Alice's detailed bio");

        // Profile -> Employee navigation (inverse side, in-memory after DB read)
        Profile fetchedProfile = profileRepository.findById(fetchedEmployee.getProfile().getProfileId()).orElseThrow();
        // Note: profile.employee is loaded from DB (mappedBy causes JPA to query back)
        assertThat(fetchedProfile.getEmployee()).isNotNull();
        assertThat(fetchedProfile.getEmployee().getName()).isEqualTo("Alice");

        System.out.println("\n[TEST] Employee -> Profile: " + fetchedEmployee.getProfile());
        System.out.println("[TEST] Profile -> Employee (bidirectional): " + fetchedProfile.getEmployee());
    }

    // ============================================================
    //  TEST 2: Demonstrate IN-MEMORY STALE STATE (IMPORTANT!)
    // ============================================================
    //
    //  If you set only the OWNING side (employee.setProfile(profile))
    //  but NOT the inverse side (profile.setEmployee(employee)),
    //  the database is correct BUT profile.getEmployee() is null IN MEMORY.
    //
    //  After a flush+clear (re-read from DB), both sides will be correct
    //  because JPA loads from the DB, not the stale in-memory state.
    //
    @Test
    @DisplayName("Demonstrate stale in-memory state when only owning side is set")
    void demonstrateStaleState_whenOnlyOwningSideSet() {
        // --- Arrange ---
        Employee employee = new Employee("Bob", "Mumbai");
        Profile profile = new Profile("Bob's bio", "linkedin.com/bob");

        // WRONG WAY: only set owning side, forget inverse side
        employee.setProfile(profile);                   // owning side (writes FK) ✓
        // profile.setEmployee(employee);               // inverse side NOT set! ✗

        // --- Act ---
        Employee saved = employeeRepository.save(employee);

        // IN MEMORY (before flush/clear): profile.getEmployee() is null!
        // because we didn't set profile.setEmployee(employee)
        assertThat(profile.getEmployee()).isNull();     // <- stale state in memory

        entityManager.flush();
        entityManager.clear();                         // Clear cache -> reload from DB

        // AFTER RE-READ FROM DB: JPA correctly loads both sides
        Profile reloadedProfile = profileRepository.findById(saved.getProfile().getProfileId()).orElseThrow();
        assertThat(reloadedProfile.getEmployee()).isNotNull();    // <- now works from DB

        System.out.println("\n[TEST] Before flush: profile.getEmployee() = null (stale state)");
        System.out.println("[TEST] After reload from DB: profile.getEmployee() = " + reloadedProfile.getEmployee());
        System.out.println("[TEST] LESSON: Always use helper methods to set BOTH sides!");
    }

    // ============================================================
    //  TEST 3: Bidirectional navigation Employee <-> Profile
    // ============================================================
    @Test
    @DisplayName("Bidirectional navigation - both Employee->Profile and Profile->Employee work")
    void bidirectionalNavigation_worksInBothDirections() {
        Employee employee = new Employee("Carol", "Delhi");
        Profile profile = new Profile("Carol's bio", "linkedin.com/carol");
        employee.setProfileWithSync(profile);

        Employee saved = employeeRepository.save(employee);
        entityManager.flush();
        entityManager.clear();

        // Navigate Employee -> Profile (owning side)
        Employee fetchedEmp = employeeRepository.findById(saved.getEmpId()).orElseThrow();
        Profile fetchedProfile = fetchedEmp.getProfile();
        assertThat(fetchedProfile).isNotNull();

        // Navigate Profile -> Employee (inverse side)
        Profile directProfile = profileRepository.findById(fetchedProfile.getProfileId()).orElseThrow();
        Employee linkedEmployee = directProfile.getEmployee();
        assertThat(linkedEmployee).isNotNull();
        assertThat(linkedEmployee.getName()).isEqualTo("Carol");

        System.out.println("\n[TEST] Employee->Profile: " + fetchedProfile);
        System.out.println("[TEST] Profile->Employee (INVERSE navigation): " + linkedEmployee);
    }

    // ============================================================
    //  TEST 4: mappedBy field changes are IGNORED by JPA
    // ============================================================
    //
    //  The INVERSE side (profile.employee) is for READ navigation only.
    //  If you try to change the relationship from the INVERSE side,
    //  JPA IGNORES it. Changes must be made from the OWNING side.
    //
    @Test
    @DisplayName("Demonstrate mappedBy - inverse side changes are ignored by JPA")
    void inverseSideChanges_areIgnoredByJPA() {
        // Setup: create two employees with profiles
        Employee emp1 = new Employee("Dave", "Bangalore");
        Profile profile1 = new Profile("Dave's bio", "linkedin.com/dave");
        emp1.setProfileWithSync(profile1);
        employeeRepository.save(emp1);

        Employee emp2 = new Employee("Eve", "Hyderabad");
        Profile profile2 = new Profile("Eve's bio", "linkedin.com/eve");
        emp2.setProfileWithSync(profile2);
        employeeRepository.save(emp2);
        entityManager.flush();
        entityManager.clear();

        // Load both employees
        Employee loadedEmp1 = employeeRepository.findById(emp1.getEmpId()).orElseThrow();
        Employee loadedEmp2 = employeeRepository.findById(emp2.getEmpId()).orElseThrow();

        // Try to change relationship from INVERSE side (Profile.employee)
        // This change will be IGNORED by JPA
        Profile loadedProfile1 = profileRepository.findById(emp1.getProfile().getProfileId()).orElseThrow();
        loadedProfile1.setEmployee(loadedEmp2);     // <- INVERSE SIDE CHANGE -> IGNORED!
        profileRepository.save(loadedProfile1);
        entityManager.flush();
        entityManager.clear();

        // Verify: FK NOT changed because inverse side changes are ignored
        Employee reFetchedEmp1 = employeeRepository.findById(emp1.getEmpId()).orElseThrow();
        assertThat(reFetchedEmp1.getProfile().getProfileId())
                .isEqualTo(emp1.getProfile().getProfileId());   // Still pointing to profile1

        System.out.println("\n[TEST] Tried to change relationship from INVERSE side (Profile.employee)");
        System.out.println("[TEST] JPA IGNORED the change - FK still points to original profile");
        System.out.println("[TEST] LESSON: Only owning side changes (employee.setProfile) are persisted!");
    }

    // ============================================================
    //  TEST 5: Delete - Cascade remove from owning side
    // ============================================================
    @Test
    @DisplayName("Delete Employee - cascade deletes Profile (owning side controls)")
    void deleteEmployee_shouldCascadeDeleteProfile() {
        Employee employee = new Employee("Frank", "Chennai");
        Profile profile = new Profile("Frank's bio", "linkedin.com/frank");
        employee.setProfileWithSync(profile);
        Employee saved = employeeRepository.save(employee);
        Long profileId = saved.getProfile().getProfileId();

        entityManager.flush();
        entityManager.clear();

        // Delete employee (owning side) -> profile also deleted via cascade
        employeeRepository.deleteById(saved.getEmpId());
        entityManager.flush();

        assertThat(employeeRepository.findById(saved.getEmpId())).isEmpty();
        assertThat(profileRepository.findById(profileId)).isEmpty();

        System.out.println("\n[TEST] Deleting Employee also deleted Profile via CascadeType.ALL");
    }
}
