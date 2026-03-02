package com.training.onetomany.bidirectional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * ================================================================
 *  ONE-TO-MANY BIDIRECTIONAL  -  INVERSE SIDE (Department / "One" side)
 * ================================================================
 *
 *  Scenario: Department <---> [Employee, ...] (both know each other)
 *
 *  Department is the INVERSE (non-owning) side because it has mappedBy.
 *  Employee is the OWNING side because it has @JoinColumn (has the FK).
 *
 *  RULE: In bidirectional @OneToMany:
 *    - @OneToMany(mappedBy = "department")   -> INVERSE side (Department)
 *    - @ManyToOne + @JoinColumn              -> OWNING side  (Employee)
 *
 *  mappedBy = "department"  ->  "The field named 'department' in Employee
 *                                 owns this relationship."
 *
 *  WHAT mappedBy MEANS HERE:
 *  --------------------------
 *  - Department does NOT have a FK column in its table
 *  - JPA does NOT write FK from this (Department) side
 *  - The list of employees is for in-memory navigation
 *  - Only changes to employee.setDepartment() write the FK to DB
 *
 *  DATABASE TABLES:
 *  -----------------
 *  otm_bi_departments:           otm_bi_employees:
 *  dept_id | dept_name           emp_id | name | city | salary | fk_dept_id
 *     1    | Engineering            1   | Alice| Pune | 60000  |     1
 *     2    | Marketing              2   | Bob  |Mumbai| 55000  |     1
 *                                  3   | Carol| Delhi| 70000  |     2
 *
 *  No FK in departments table. FK (fk_dept_id) is in employees table.
 *
 *  CRITICAL RULE - KEEPING BOTH SIDES IN SYNC:
 *  ---------------------------------------------
 *  Since Department is the INVERSE side, setting dept.getEmployees().add(emp)
 *  alone does NOT save the FK to the database.
 *  You MUST also set emp.setDepartment(dept) to write the FK.
 *
 *  Use the helper method addEmployee() below which sets both sides:
 *    dept.addEmployee(emp);   <- sets dept.employees AND emp.department
 *
 *  BIDIRECTIONAL vs UNIDIRECTIONAL PERFORMANCE:
 *  ----------------------------------------------
 *  Bidirectional @OneToMany is PREFERRED over unidirectional because:
 *  - Unidirectional uses extra UPDATE statements for FK
 *  - Bidirectional uses INSERT with FK directly (better SQL)
 *  - Bidirectional enables navigation in both directions
 *
 *  orphanRemoval vs CascadeType.REMOVE:
 *  --------------------------------------
 *  orphanRemoval = true  -> Removes employee from DB when removed from dept.employees
 *                            (even without explicit delete call)
 *  CascadeType.REMOVE    -> Removes all employees when department is deleted
 *  They can be used together for full lifecycle management.
 *
 * ================================================================
 */
@Entity(name = "OtmBiDepartment")       // Unique JPA entity name
@Table(name = "otm_bi_departments")
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
    //  ONE-TO-MANY MAPPING (BIDIRECTIONAL - INVERSE SIDE)
    // ============================================================
    //
    //  mappedBy = "department":
    //    -> "The 'department' field in Employee class owns this relationship"
    //    -> Department does NOT have @JoinColumn (no FK in dept table)
    //    -> JPA ignores changes to this collection when writing FK values
    //    -> Only employee.setDepartment(dept) writes the FK
    //
    //  cascade = ALL:
    //    -> persist, merge, remove, refresh, detach propagated to employees
    //
    //  orphanRemoval = true:
    //    -> If employee is removed from employees list, it is deleted from DB
    //    -> Useful for parent-child where child can't exist without parent
    //
    //  fetch = LAZY (default for @OneToMany):
    //    -> Employees loaded only when dept.getEmployees() is accessed
    //
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();

    // ============================================================
    //  HELPER METHOD - BEST PRACTICE for Bidirectional Relationships
    // ============================================================
    //
    //  This method sets BOTH sides of the relationship:
    //    1. Adds employee to department's list
    //    2. Sets employee's department reference
    //
    //  ALWAYS use this helper instead of setting sides individually.
    //  This prevents stale in-memory state.
    //
    public void addEmployee(Employee employee) {
        this.employees.add(employee);
        employee.setDepartment(this);    // Set the owning side too!
    }

    public void removeEmployee(Employee employee) {
        this.employees.remove(employee);
        employee.setDepartment(null);    // Clear the owning side too!
    }

    public Department(String deptName, String location) {
        this.deptName = deptName;
        this.location = location;
    }

    @Override
    public String toString() {
        return "Department{deptId=" + deptId + ", deptName='" + deptName
                + "', location='" + location + "', employeeCount=" + employees.size() + "}";
    }
}
