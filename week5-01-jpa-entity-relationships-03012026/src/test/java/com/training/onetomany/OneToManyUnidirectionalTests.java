package com.training.onetomany;

import com.training.onetomany.unidirectional.Department;
import com.training.onetomany.unidirectional.DepartmentRepository;
import com.training.onetomany.unidirectional.Employee;
import com.training.onetomany.unidirectional.EmployeeRepository;
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
 *  TESTS: OneToMany UNIDIRECTIONAL
 * ================================================================
 *
 *  WHAT THESE TESTS DEMONSTRATE:
 *  -------------------------------
 *  1. Department -> Employees navigation (ONE-WAY only)
 *  2. @JoinColumn puts FK in employees table (no join table)
 *  3. CascadeType.ALL saves employees when department is saved
 *  4. orphanRemoval deletes employees removed from the list
 *  5. Cannot navigate Employee -> Department (unidirectional)
 *
 *  UNIDIRECTIONAL SPECIFIC BEHAVIOR:
 *  ------------------------------------
 *  When saving a department with employees, JPA runs:
 *    1. INSERT INTO otm_uni_employees (no fk_dept_id yet)
 *    2. INSERT INTO otm_uni_departments
 *    3. UPDATE otm_uni_employees SET fk_dept_id = ? (separate UPDATE!)
 *
 *  This extra UPDATE is the "cost" of unidirectional @OneToMany.
 *  Bidirectional avoids this by using a single INSERT with the FK.
 *
 * ================================================================
 */
@DataJpaTest
@EntityScan("com.training.onetomany.unidirectional")
@EnableJpaRepositories("com.training.onetomany.unidirectional")
@DisplayName("OneToMany Unidirectional Tests")
class OneToManyUnidirectionalTests {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ============================================================
    //  TEST 1: Save Department with Employees (cascade)
    // ============================================================
    @Test
    @DisplayName("Save Department with Employees - CascadeType.ALL persists all employees")
    void saveDepartment_withEmployees_shouldCascadePersistAllEmployees() {
        // Arrange
        Department dept = new Department("Engineering", "Bangalore");

        Employee emp1 = new Employee("Alice", "Pune", 60000.0);
        Employee emp2 = new Employee("Bob", "Mumbai", 55000.0);
        Employee emp3 = new Employee("Carol", "Delhi", 70000.0);

        dept.addEmployee(emp1);
        dept.addEmployee(emp2);
        dept.addEmployee(emp3);

        // Act - save department only, employees cascaded
        Department saved = departmentRepository.save(dept);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Department fetched = departmentRepository.findById(saved.getDeptId()).orElseThrow();
        assertThat(fetched.getEmployees()).hasSize(3);
        assertThat(fetched.getEmployees())
                .extracting(Employee::getName)
                .containsExactlyInAnyOrder("Alice", "Bob", "Carol");

        System.out.println("\n[TEST] Saved department: " + fetched.getDeptName());
        System.out.println("[TEST] Employees cascaded: " + fetched.getEmployees().size());
        fetched.getEmployees().forEach(e -> System.out.println("  -> " + e));
        System.out.println("\n[TEST] NOTE: Check SQL output above - you'll see INSERT + UPDATE statements");
        System.out.println("[TEST] Unidirectional @OneToMany issues extra UPDATEs to set FK column");
    }

    // ============================================================
    //  TEST 2: orphanRemoval - remove employee from list deletes it
    // ============================================================
    //
    //  orphanRemoval = true on @OneToMany means:
    //  If an employee is removed from department.employees list
    //  and the department is saved, the employee is deleted from DB.
    //
    @Test
    @DisplayName("orphanRemoval - removing Employee from list deletes it from DB")
    void removeEmployee_fromDeptList_shouldDeleteViaOrphanRemoval() {
        // Setup
        Department dept = new Department("Marketing", "Mumbai");
        Employee emp1 = new Employee("Dave", "Pune", 50000.0);
        Employee emp2 = new Employee("Eve", "Delhi", 55000.0);
        dept.addEmployee(emp1);
        dept.addEmployee(emp2);
        Department saved = departmentRepository.save(dept);
        entityManager.flush();
        entityManager.clear();

        // Get saved employee IDs
        Department fetchedDept = departmentRepository.findById(saved.getDeptId()).orElseThrow();
        Long emp1Id = fetchedDept.getEmployees().get(0).getEmpId();

        // Act: remove one employee from the list
        Employee empToRemove = fetchedDept.getEmployees().get(0);
        fetchedDept.getEmployees().remove(empToRemove);
        departmentRepository.save(fetchedDept);
        entityManager.flush();
        entityManager.clear();

        // Assert: employee should be deleted from DB (orphanRemoval = true)
        Department refetched = departmentRepository.findById(saved.getDeptId()).orElseThrow();
        assertThat(refetched.getEmployees()).hasSize(1);

        // The removed employee should not exist in DB anymore
        Optional<Employee> deletedEmp = employeeRepository.findById(emp1Id);
        assertThat(deletedEmp).isEmpty();

        System.out.println("\n[TEST] Removed employee from dept list -> orphanRemoval deleted it from DB");
        System.out.println("[TEST] Remaining employees: " + refetched.getEmployees().size());
    }

    // ============================================================
    //  TEST 3: Delete Department - cascade deletes all employees
    // ============================================================
    @Test
    @DisplayName("Delete Department - cascade deletes all its Employees")
    void deleteDepartment_shouldCascadeDeleteAllEmployees() {
        // Setup
        Department dept = new Department("Sales", "Hyderabad");
        dept.addEmployee(new Employee("Frank", "Pune", 45000.0));
        dept.addEmployee(new Employee("Grace", "Mumbai", 48000.0));
        Department saved = departmentRepository.save(dept);

        long totalEmployeesBefore = employeeRepository.count();
        entityManager.flush();
        entityManager.clear();

        // Act
        departmentRepository.deleteById(saved.getDeptId());
        entityManager.flush();

        // Assert
        assertThat(departmentRepository.findById(saved.getDeptId())).isEmpty();
        long totalEmployeesAfter = employeeRepository.count();
        assertThat(totalEmployeesAfter).isEqualTo(totalEmployeesBefore - 2);

        System.out.println("\n[TEST] Deleted department 'Sales' -> also deleted 2 employees via cascade");
    }

    // ============================================================
    //  TEST 4: Cannot navigate Employee -> Department (unidirectional)
    // ============================================================
    @Test
    @DisplayName("Employee has no Department reference - demonstrates unidirectional limitation")
    void employee_hasNoDepartmentReference_unidirectionalLimitation() {
        Department dept = new Department("IT", "Bangalore");
        dept.addEmployee(new Employee("Henry", "Chennai", 65000.0));
        departmentRepository.save(dept);
        entityManager.flush();
        entityManager.clear();

        // You CAN access employees via Department
        Department fetchedDept = departmentRepository.findByDeptName("IT").orElseThrow();
        assertThat(fetchedDept.getEmployees()).hasSize(1);
        System.out.println("\n[TEST] Can navigate: Department -> Employees: " + fetchedDept.getEmployees());

        // Employee has no department field (unidirectional)
        Employee emp = fetchedDept.getEmployees().get(0);
        // emp.getDepartment()  <- This method does NOT exist in unidirectional!
        // This is the key limitation of unidirectional @OneToMany

        System.out.println("[TEST] Employee class has no department reference (unidirectional by design)");
        System.out.println("[TEST] Employee: " + emp);
        System.out.println("[TEST] To get employee's dept, use: dept.getEmployees().contains(emp)");
    }

    // ============================================================
    //  TEST 5: Count and verify FK column behavior
    // ============================================================
    @Test
    @DisplayName("Verify FK (fk_dept_id) is stored in employees table")
    void verifyFKIsInEmployeesTable() {
        Department dept1 = new Department("HR", "Pune");
        dept1.addEmployee(new Employee("Iris", "Pune", 42000.0));
        dept1.addEmployee(new Employee("Jack", "Mumbai", 44000.0));
        departmentRepository.save(dept1);

        Department dept2 = new Department("Finance", "Delhi");
        dept2.addEmployee(new Employee("Kate", "Delhi", 58000.0));
        departmentRepository.save(dept2);
        entityManager.flush();
        entityManager.clear();

        // Verify employees count per department
        Department hrDept = departmentRepository.findByDeptName("HR").orElseThrow();
        Department financeDept = departmentRepository.findByDeptName("Finance").orElseThrow();

        assertThat(hrDept.getEmployees()).hasSize(2);
        assertThat(financeDept.getEmployees()).hasSize(1);

        // Total employees
        assertThat(employeeRepository.count()).isEqualTo(3);

        System.out.println("\n[TEST] HR dept employees (FK=1): " + hrDept.getEmployees().size());
        System.out.println("[TEST] Finance dept employees (FK=2): " + financeDept.getEmployees().size());
        System.out.println("[TEST] All FK values stored in otm_uni_employees.fk_dept_id column");
    }
}
