package com.training.onetomany.unidirectional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * ================================================================
 *  ONE-TO-MANY UNIDIRECTIONAL  -  OWNING SIDE (Department)
 * ================================================================
 *
 *  Scenario: One Department has MANY Employees.
 *            Navigation is ONE-WAY: Department ---> [Employee, ...]
 *            Employee has NO reference back to Department.
 *
 *  WHAT IS @OneToMany UNIDIRECTIONAL?
 *  ------------------------------------
 *  Department knows about its employees (has a List<Employee>),
 *  but Employee does NOT know which department it belongs to.
 *
 *  TWO WAYS TO MAP UNIDIRECTIONAL @OneToMany:
 *  -------------------------------------------
 *  Option A: WITHOUT @JoinColumn  (NOT recommended)
 *  --------------------------------------------------
 *    @OneToMany
 *    private List<Employee> employees;
 *
 *    -> JPA creates a separate JOIN TABLE: otm_uni_dept_employees
 *    -> Join table has: dept_id | emp_id
 *    -> Extra table, extra SQL, worse performance
 *    -> Not intuitive for a simple parent-child relationship
 *
 *  Option B: WITH @JoinColumn  (RECOMMENDED for unidirectional)
 *  --------------------------------------------------------------
 *    @OneToMany
 *    @JoinColumn(name = "fk_dept_id")
 *    private List<Employee> employees;
 *
 *    -> No join table! FK column goes into the Employee table
 *    -> More natural - FK is in the "many" side table
 *    -> However: Department is technically NOT the FK owner
 *       (Employee table has the FK, but Employee class doesn't map it)
 *    -> JPA issues extra UPDATE statements to set the FK
 *       (slight performance overhead vs bidirectional)
 *
 *  DATABASE TABLES (with @JoinColumn - Option B):
 *  ------------------------------------------------
 *  otm_uni_departments:          otm_uni_employees:
 *  dept_id | dept_name           emp_id | name | city | fk_dept_id
 *     1    | Engineering            1   | Alice| Pune |     1
 *     2    | Marketing              2   | Bob  |Mumbai|     1
 *                                  3   | Carol| Delhi|     2
 *
 *  FK (fk_dept_id) is in the employees table.
 *
 *  DEFAULT STRATEGIES SUMMARY (@OneToMany):
 *  -----------------------------------------
 *  Aspect            | Default              | Meaning
 *  ------------------|----------------------|--------------------------------
 *  Mapping Strategy  | JOIN TABLE           | Without @JoinColumn -> join table
 *  FK Ownership      | The "many" side table| FK is in employees table
 *  Fetch Type        | LAZY                 | Employees not loaded until accessed
 *  Cascade           | NONE                 | Saving dept won't save employees
 *  orphanRemoval     | false                | Removing employee from list won't
 *                    |                      | delete it from DB automatically
 *
 *  RECOMMENDATIONS:
 *  -----------------
 *  - Prefer BIDIRECTIONAL @OneToMany over unidirectional for better performance
 *  - If using unidirectional, always use @JoinColumn to avoid the join table
 *  - Use cascade = ALL if employees are always managed through Department
 *  - Use orphanRemoval = true to auto-delete employees removed from the list
 *
 * ================================================================
 */
@Entity(name = "OtmUniDepartment")      // Unique JPA entity name
@Table(name = "otm_uni_departments")
@Data
@NoArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "dept_name", nullable = false)
    private String deptName;

    @Column(name = "location")
    private String location;

    // ============================================================
    //  ONE-TO-MANY MAPPING (UNIDIRECTIONAL)
    // ============================================================
    //
    //  @OneToMany: Department can have many Employees
    //
    //  @JoinColumn(name = "fk_dept_id"):
    //    -> Puts the FK in the otm_uni_employees table (not a join table)
    //    -> The column name in the employees table will be "fk_dept_id"
    //    -> This is KEY - without @JoinColumn, a join table would be created
    //
    //  cascade = ALL:
    //    -> Saving department saves all its employees
    //    -> Deleting department deletes all its employees
    //
    //  orphanRemoval = true:
    //    -> When an employee is removed from the list AND saved,
    //       that employee relationship is automatically deleted from the DB
    //    -> Useful for "child is owned by parent" scenarios
    //
    //  fetch = LAZY (default for @OneToMany):
    //    -> Employees are NOT loaded when department is fetched
    //    -> Only fetched when department.getEmployees() is called
    //    -> Prevents loading all employees unnecessarily
    //
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_dept_id")
    private List<Employee> employees = new ArrayList<>();

    // ============================================================
    //  HELPER METHOD - Add employee to department
    // ============================================================
    public void addEmployee(Employee employee) {
        this.employees.add(employee);
    }

    public void removeEmployee(Employee employee) {
        this.employees.remove(employee);
    }

    public Department(String deptName, String location) {
        this.deptName = deptName;
        this.location = location;
    }

    @Override
    public String toString() {
        return "Department{deptId=" + deptId + ", deptName='" + deptName + "', location='" + location
                + "', employeeCount=" + employees.size() + "}";
    }
}
