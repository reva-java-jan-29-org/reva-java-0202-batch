package com.training.onetomany;

import com.training.onetomany.bidirectional.Department;
import com.training.onetomany.bidirectional.DepartmentRepository;
import com.training.onetomany.bidirectional.Employee;
import com.training.onetomany.bidirectional.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ================================================================
 *  TESTS: OneToMany BIDIRECTIONAL
 * ================================================================
 *
 *  KEY DIFFERENCES FROM UNIDIRECTIONAL:
 *  --------------------------------------
 *  1. Employee has @ManyToOne (OWNING SIDE - has FK)
 *  2. Department has @OneToMany(mappedBy) (INVERSE SIDE - no FK)
 *  3. MUST use addEmployee() helper to set both sides
 *  4. Both directions work: dept.getEmployees() AND emp.getDepartment()
 *  5. Better SQL: INSERT with FK directly (no extra UPDATE statements)
 *
 *  SQL COMPARISON:
 *  ----------------
 *  Unidirectional @OneToMany:
 *    INSERT INTO employees (emp_id, name, city, salary)  <- no FK yet
 *    INSERT INTO departments (dept_id, name)
 *    UPDATE employees SET fk_dept_id = ?                 <- extra UPDATE!
 *
 *  Bidirectional @OneToMany (this package):
 *    INSERT INTO departments (dept_id, name)
 *    INSERT INTO employees (emp_id, name, city, salary, fk_dept_id)  <- FK in INSERT!
 *    (No extra UPDATE needed - more efficient!)
 *
 * ================================================================
 */
@DataJpaTest
@EntityScan("com.training.onetomany.bidirectional")
@EnableJpaRepositories("com.training.onetomany.bidirectional")
@DisplayName("OneToMany Bidirectional Tests")
class OneToManyBidirectionalTests {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ============================================================
    //  TEST 1: Save using helper method (addEmployee)
    // ============================================================
    //
    //  dept.addEmployee(emp) sets BOTH:
    //    1. dept.employees.add(emp)     <- inverse side (in-memory navigation)
    //    2. emp.setDepartment(dept)     <- OWNING side (writes FK to DB)
    //
    //  Only emp.setDepartment(dept) actually causes the FK to be saved.
    //  The dept.employees list is for in-memory navigation.
    //
    @Test
    @DisplayName("Save with addEmployee helper - both sides set correctly")
    void saveDepartmentWithEmployees_usingHelper_shouldPersistFKCorrectly() {
        // Arrange
        Department dept = new Department("Engineering", "Bangalore");
        Employee emp1 = new Employee("Alice", "Pune", 60000.0);
        Employee emp2 = new Employee("Bob", "Mumbai", 55000.0);

        // BEST PRACTICE: use addEmployee() which sets BOTH sides
        dept.addEmployee(emp1);     // emp1.department = dept AND dept.employees.add(emp1)
        dept.addEmployee(emp2);     // emp2.department = dept AND dept.employees.add(emp2)

        // Act - cascade saves employees because dept.employees has cascade=ALL
        Department saved = departmentRepository.save(dept);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Department fetchedDept = departmentRepository.findById(saved.getDeptId()).orElseThrow();
        assertThat(fetchedDept.getEmployees()).hasSize(2);

        // Verify bidirectional navigation: emp -> dept
        Employee fetchedEmp = employeeRepository.findById(emp1.getEmpId()).orElseThrow();
        assertThat(fetchedEmp.getDepartment()).isNotNull();
        assertThat(fetchedEmp.getDepartment().getDeptName()).isEqualTo("Engineering");

        System.out.println("\n[TEST] Department saved with employees via helper: " + fetchedDept.getDeptName());
        System.out.println("[TEST] Employee -> Department (OWNING side navigation): " + fetchedEmp.getDepartment());
        System.out.println("[TEST] NOTE: Check SQL - INSERT with FK included (no extra UPDATE!)");
    }

    // ============================================================
    //  TEST 2: Demonstrate WRONG WAY - only setting inverse side
    // ============================================================
    //
    //  If you only add to dept.employees but DON'T set emp.department,
    //  the FK is NOT written to the database!
    //  This is because Department is the INVERSE side (mappedBy).
    //
    @Test
    @DisplayName("Demonstrate wrong way - only setting inverse side does NOT persist FK")
    void wrongWay_onlySettingInverseSide_doesNotPersistFK() {
        Department dept = new Department("HR", "Mumbai");
        Employee emp = new Employee("Charlie", "Delhi", 45000.0);

        // WRONG WAY: only adding to inverse side list, NOT setting owning side
        dept.getEmployees().add(emp);   // <- inverse side only (NO FK written)
        // emp.setDepartment(dept);     // <- owning side NOT set!

        // This throws because Employee.department (nullable=false) was never set.
        // JPA enforces the NOT NULL constraint, proving that only the OWNING side
        // (emp.setDepartment) can write the FK — the inverse side list has no effect.
        assertThrows(Exception.class, () -> {
            employeeRepository.save(emp);
            departmentRepository.save(dept);
            entityManager.flush();
        });

        System.out.println("\n[TEST] LESSON: Only owning side (emp.setDepartment) writes FK to DB");
        System.out.println("[TEST] dept.getEmployees().add(emp) alone does NOT set the FK column");
        System.out.println("[TEST] Always use dept.addEmployee(emp) helper which sets BOTH sides!");
    }

    // ============================================================
    //  TEST 3: Bidirectional navigation - BOTH directions
    // ============================================================
    @Test
    @DisplayName("Bidirectional navigation - dept->employees AND employee->dept")
    void bidirectionalNavigation_worksBothWays() {
        Department dept = new Department("Marketing", "Pune");
        Employee emp1 = new Employee("Dave", "Pune", 52000.0);
        Employee emp2 = new Employee("Eve", "Mumbai", 54000.0);
        dept.addEmployee(emp1);
        dept.addEmployee(emp2);
        Department saved = departmentRepository.save(dept);
        entityManager.flush();
        entityManager.clear();

        // Direction 1: Department -> Employees (INVERSE side navigation)
        Department fetchedDept = departmentRepository.findById(saved.getDeptId()).orElseThrow();
        assertThat(fetchedDept.getEmployees()).hasSize(2);
        System.out.println("\n[TEST] Direction 1 - Dept -> Employees: " + fetchedDept.getEmployees());

        // Direction 2: Employee -> Department (OWNING side navigation)
        List<Employee> employees = employeeRepository.findByDepartmentDeptId(saved.getDeptId());
        assertThat(employees).hasSize(2);
        Employee fetchedEmp = employees.get(0);
        assertThat(fetchedEmp.getDepartment().getDeptName()).isEqualTo("Marketing");
        System.out.println("[TEST] Direction 2 - Employee -> Dept: " + fetchedEmp.getDepartment());
    }

    // ============================================================
    //  TEST 4: Add employee to existing department
    // ============================================================
    @Test
    @DisplayName("Add new Employee to existing Department after initial save")
    void addEmployeeToExistingDept_shouldUpdateRelationship() {
        // Initial state
        Department dept = new Department("Sales", "Hyderabad");
        Employee emp1 = new Employee("Frank", "Pune", 48000.0);
        dept.addEmployee(emp1);
        Department saved = departmentRepository.save(dept);
        entityManager.flush();
        entityManager.clear();

        // Load and add a new employee
        Department loadedDept = departmentRepository.findById(saved.getDeptId()).orElseThrow();
        Employee emp2 = new Employee("Grace", "Mumbai", 51000.0);
        loadedDept.addEmployee(emp2);   // sets both sides via helper
        departmentRepository.save(loadedDept);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Department reFetched = departmentRepository.findById(saved.getDeptId()).orElseThrow();
        assertThat(reFetched.getEmployees()).hasSize(2);

        System.out.println("\n[TEST] Added new employee to existing dept. Total: " + reFetched.getEmployees().size());
    }

    // ============================================================
    //  TEST 5: orphanRemoval - remove employee from dept list
    // ============================================================
    @Test
    @DisplayName("orphanRemoval - removing Employee from dept list deletes from DB")
    void removeEmployee_fromDeptList_deletesViOrphanRemoval() {
        Department dept = new Department("Finance", "Delhi");
        Employee emp1 = new Employee("Henry", "Delhi", 62000.0);
        Employee emp2 = new Employee("Iris", "Noida", 58000.0);
        dept.addEmployee(emp1);
        dept.addEmployee(emp2);
        Department saved = departmentRepository.save(dept);
        entityManager.flush();
        entityManager.clear();

        // Remove employee via helper method (which also clears emp.department)
        Department loadedDept = departmentRepository.findById(saved.getDeptId()).orElseThrow();
        Employee empToRemove = loadedDept.getEmployees().get(0);
        Long removedEmpId = empToRemove.getEmpId();
        loadedDept.removeEmployee(empToRemove);   // sets both sides via helper
        departmentRepository.save(loadedDept);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(departmentRepository.findById(saved.getDeptId()).get().getEmployees()).hasSize(1);
        assertThat(employeeRepository.findById(removedEmpId)).isEmpty();  // deleted via orphanRemoval!

        System.out.println("\n[TEST] Removed employee from dept -> orphanRemoval deleted from DB");
    }

    // ============================================================
    //  TEST 6: Query employee by department (owning side has FK)
    // ============================================================
    @Test
    @DisplayName("Query employees by Department - owning side FK enables this")
    void findEmployeesByDepartment_usesOwningSideFK() {
        Department engDept = new Department("IT", "Bangalore");
        Department hrDept = new Department("HR", "Pune");

        Employee e1 = new Employee("Jack", "Bangalore", 70000.0);
        Employee e2 = new Employee("Kate", "Bangalore", 65000.0);
        Employee e3 = new Employee("Liam", "Pune", 40000.0);

        engDept.addEmployee(e1);
        engDept.addEmployee(e2);
        hrDept.addEmployee(e3);

        departmentRepository.save(engDept);
        departmentRepository.save(hrDept);
        entityManager.flush();
        entityManager.clear();

        // Query using Employee's FK column (owning side)
        Department savedIT = departmentRepository.findByDeptName("IT").orElseThrow();
        List<Employee> itEmployees = employeeRepository.findByDepartment(savedIT);

        assertThat(itEmployees).hasSize(2);
        assertThat(itEmployees).extracting(Employee::getName)
                .containsExactlyInAnyOrder("Jack", "Kate");

        System.out.println("\n[TEST] Queried employees by department (FK-based query): " + itEmployees.size());
    }
}
