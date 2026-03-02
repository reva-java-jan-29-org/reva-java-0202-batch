package com.training.manytoone;

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
 *  TESTS: ManyToOne UNIDIRECTIONAL
 * ================================================================
 *
 *  WHAT THESE TESTS DEMONSTRATE:
 *  -------------------------------
 *  1. Employee always owns the FK (always true for @ManyToOne)
 *  2. SAVE ORDER: Department must be saved BEFORE Employee
 *  3. LAZY vs EAGER fetch behavior
 *  4. Query employees by department (FK-based queries)
 *  5. The N+1 query problem and how JOIN FETCH solves it
 *  6. Updating employee's department (FK update)
 *
 *  @ManyToOne IS THE MOST FUNDAMENTAL RELATIONSHIP:
 *  -------------------------------------------------
 *  - Always the owning side (has @JoinColumn, has FK)
 *  - Default fetch: EAGER (change to LAZY for production!)
 *  - No cascade by default (Department must pre-exist)
 *  - @OneToMany Bidirectional is just @ManyToOne + mappedBy
 *
 * ================================================================
 */
@DataJpaTest
@EntityScan("com.training.manytoone")
@EnableJpaRepositories("com.training.manytoone")
@DisplayName("ManyToOne Tests")
class ManyToOneTests {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ============================================================
    //  TEST 1: SAVE ORDER - Department before Employee
    // ============================================================
    //
    //  CRITICAL RULE: When cascade = NONE (default), you MUST save
    //  Department FIRST to give it an ID, then assign it to Employee.
    //
    //  If you try to save Employee with an unsaved (transient) Department,
    //  JPA throws:
    //  TransientPropertyValueException: object references an unsaved transient instance
    //
    @Test
    @DisplayName("Save Order - must save Department first, then Employee")
    void saveOrder_departmentFirst_thenEmployee() {
        // Step 1: Save Department first (gets a DB-generated ID)
        Department engineering = new Department("Engineering", "Bangalore");
        Department savedDept = departmentRepository.save(engineering);

        System.out.println("\n[TEST] Step 1: Department saved, ID = " + savedDept.getDeptId());

        // Step 2: Create Employee and assign the SAVED department (has ID)
        Employee alice = new Employee("Alice", "Pune", 60000.0, savedDept);
        Employee savedEmp = employeeRepository.save(alice);

        System.out.println("[TEST] Step 2: Employee saved, dept FK = " + savedDept.getDeptId());

        entityManager.flush();
        entityManager.clear();

        // Assert
        Employee fetchedEmp = employeeRepository.findById(savedEmp.getEmpId()).orElseThrow();
        assertThat(fetchedEmp.getDepartment()).isNotNull();
        assertThat(fetchedEmp.getDepartment().getDeptId()).isEqualTo(savedDept.getDeptId());
        assertThat(fetchedEmp.getDepartment().getDeptName()).isEqualTo("Engineering");

        System.out.println("[TEST] Verified: Employee FK points to Department: " + fetchedEmp.getDepartment());
    }

    // ============================================================
    //  TEST 2: Many employees in ONE department
    // ============================================================
    @Test
    @DisplayName("Many Employees can belong to the same Department")
    void manyEmployees_sameOneDepartment_singleFK() {
        // One department
        Department marketing = departmentRepository.save(new Department("Marketing", "Mumbai"));

        // Many employees all pointing to same department
        Employee e1 = employeeRepository.save(new Employee("Bob", "Mumbai", 45000.0, marketing));
        Employee e2 = employeeRepository.save(new Employee("Carol", "Pune", 48000.0, marketing));
        Employee e3 = employeeRepository.save(new Employee("Dave", "Delhi", 52000.0, marketing));

        entityManager.flush();
        entityManager.clear();

        // All employees should point to the same department
        List<Employee> mktEmployees = employeeRepository.findByDepartmentDeptId(marketing.getDeptId());
        assertThat(mktEmployees).hasSize(3);
        mktEmployees.forEach(emp ->
                assertThat(emp.getDepartment().getDeptName()).isEqualTo("Marketing")
        );

        System.out.println("\n[TEST] " + mktEmployees.size() + " employees all belong to Marketing dept");
        System.out.println("[TEST] All share the same FK value in fk_dept_id column");
        mktEmployees.forEach(e -> System.out.println("  -> " + e));
    }

    // ============================================================
    //  TEST 3: Query by department name (traverse the relationship)
    // ============================================================
    @Test
    @DisplayName("Find Employees by Department name - Spring Data traverses FK relationship")
    void findEmployeesByDeptName_springDataTraversesRelationship() {
        Department hr = departmentRepository.save(new Department("HR", "Pune"));
        Department it = departmentRepository.save(new Department("IT", "Bangalore"));

        employeeRepository.save(new Employee("Eve", "Pune", 40000.0, hr));
        employeeRepository.save(new Employee("Frank", "Pune", 42000.0, hr));
        employeeRepository.save(new Employee("Grace", "Bangalore", 75000.0, it));

        entityManager.flush();
        entityManager.clear();

        // findByDepartmentDeptName generates:
        //   SELECT * FROM mto_employees e JOIN mto_departments d ON e.fk_dept_id = d.dept_id
        //   WHERE d.dept_name = ?
        List<Employee> hrEmployees = employeeRepository.findByDepartmentDeptName("HR");
        assertThat(hrEmployees).hasSize(2);

        List<Employee> itEmployees = employeeRepository.findByDepartmentDeptName("IT");
        assertThat(itEmployees).hasSize(1);

        System.out.println("\n[TEST] HR employees found by dept name: " + hrEmployees.size());
        System.out.println("[TEST] IT employees found by dept name: " + itEmployees.size());
        System.out.println("[TEST] Spring Data JPA auto-generates the JOIN query!");
    }

    // ============================================================
    //  TEST 4: N+1 Query Problem and JOIN FETCH Solution
    // ============================================================
    //
    //  N+1 Problem with EAGER fetch (default for @ManyToOne):
    //  --------------------------------------------------------
    //  If @ManyToOne were EAGER and you call findAll() on 10 employees:
    //    Query 1: SELECT * FROM mto_employees          (all employees)
    //    Query 2: SELECT * FROM mto_departments WHERE dept_id = 1
    //    Query 3: SELECT * FROM mto_departments WHERE dept_id = 2
    //    ... (N queries for N unique departments)
    //  Total: 1 + N queries = N+1 Problem!
    //
    //  FIX 1: Use FetchType.LAZY (already applied in Employee class)
    //  FIX 2: Use JOIN FETCH in JPQL for bulk operations
    //
    @Test
    @DisplayName("JOIN FETCH query - loads Employee+Department in one SQL query")
    void joinFetch_loadsEmployeeAndDeptInOneQuery() {
        Department fin = departmentRepository.save(new Department("Finance", "Delhi"));
        departmentRepository.save(new Department("Legal", "Chennai"));

        employeeRepository.save(new Employee("Henry", "Delhi", 68000.0, fin));
        employeeRepository.save(new Employee("Iris", "Delhi", 62000.0, fin));

        entityManager.flush();
        entityManager.clear();

        // findAllWithDepartment() uses "SELECT e FROM MtoEmployee e JOIN FETCH e.department"
        // This fetches ALL employees with their departments in ONE SQL query
        // No N+1 problem!
        List<Employee> allWithDept = employeeRepository.findAllWithDepartment();
        assertThat(allWithDept).hasSize(2);
        allWithDept.forEach(e -> assertThat(e.getDepartment()).isNotNull());

        System.out.println("\n[TEST] JOIN FETCH loaded " + allWithDept.size() + " employees with departments");
        System.out.println("[TEST] Only ONE SQL query executed (check above SQL output)");
        System.out.println("[TEST] This solves the N+1 problem for bulk operations");
    }

    // ============================================================
    //  TEST 5: Update Employee's Department (FK change)
    // ============================================================
    @Test
    @DisplayName("Update Employee - transfer to different Department (FK update)")
    void transferEmployee_toDifferentDepartment_updatesFKValue() {
        Department engineering = departmentRepository.save(new Department("Engineering", "Bangalore"));
        Department management = departmentRepository.save(new Department("Management", "Hyderabad"));

        Employee jack = employeeRepository.save(new Employee("Jack", "Bangalore", 80000.0, engineering));
        entityManager.flush();
        entityManager.clear();

        // Transfer Jack from Engineering to Management
        Employee loaded = employeeRepository.findById(jack.getEmpId()).orElseThrow();
        loaded.setDepartment(management);   // Update FK (owning side)
        employeeRepository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        // Verify FK updated
        Employee updated = employeeRepository.findById(jack.getEmpId()).orElseThrow();
        assertThat(updated.getDepartment().getDeptName()).isEqualTo("Management");

        System.out.println("\n[TEST] Employee transferred: Engineering -> Management");
        System.out.println("[TEST] FK (fk_dept_id) updated in employees table");
    }

    // ============================================================
    //  TEST 6: Salary range query within department
    // ============================================================
    @Test
    @DisplayName("Find employees by salary range within department")
    void findEmployees_bySalaryRange() {
        Department dept = departmentRepository.save(new Department("Operations", "Mumbai"));
        employeeRepository.save(new Employee("Kate", "Mumbai", 35000.0, dept));
        employeeRepository.save(new Employee("Liam", "Pune", 55000.0, dept));
        employeeRepository.save(new Employee("Mia", "Delhi", 85000.0, dept));

        entityManager.flush();
        entityManager.clear();

        List<Employee> midRange = employeeRepository.findBySalaryBetween(40000.0, 70000.0);
        assertThat(midRange).hasSize(1);
        assertThat(midRange.get(0).getName()).isEqualTo("Liam");

        System.out.println("\n[TEST] Salary between 40k-70k: " + midRange.get(0).getName());
    }
}
