package com.training.manytoone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ================================================================
 *  MANY-TO-ONE (UNIDIRECTIONAL)  -  OWNING SIDE (Employee)
 * ================================================================
 *
 *  Scenario: MANY Employees belong to ONE Department.
 *            Navigation is ONE-WAY: Employee ---> Department
 *            Department has NO reference back to Employees.
 *
 *  @ManyToOne: THE FOUNDATIONAL RELATIONSHIP
 *  -------------------------------------------
 *  @ManyToOne is the SIMPLEST and most FUNDAMENTAL JPA relationship.
 *  Understanding @ManyToOne is key to understanding ALL other relationships
 *  because:
 *    - @OneToMany Bidirectional is just @ManyToOne + @OneToMany(mappedBy)
 *    - @ManyToOne always owns the FK (no exceptions!)
 *    - @ManyToOne default fetch is EAGER (change to LAZY!)
 *
 *  KEY RULES FOR @ManyToOne:
 *  --------------------------
 *  1. @ManyToOne ALWAYS owns the FK column
 *  2. @JoinColumn specifies the FK column name
 *  3. Default fetch type is EAGER (loads department with every employee)
 *  4. No cascade by default (must save department separately first)
 *
 *  THE N+1 PROBLEM (Important Performance Concern):
 *  -------------------------------------------------
 *  If you query all employees (e.g., findAll()) and @ManyToOne is EAGER,
 *  Hibernate runs:
 *    1 query  -> SELECT * FROM employees
 *    N queries-> SELECT * FROM departments WHERE dept_id = ?  (for each employee)
 *
 *  This "1+N" (or N+1) queries problem can devastate performance.
 *  FIX: Use FetchType.LAZY or use @Query with JOIN FETCH.
 *
 *  DATABASE TABLES:
 *  -----------------
 *  mto_employees:                           mto_departments:
 *  emp_id | name  | city | salary |fk_dept_id   dept_id | dept_name | location
 *    1    | Alice | Pune | 60000  |    1     ->    1    | Engineer. | Bangalore
 *    2    | Bob   |Mumbai| 55000  |    1     ->    1    | Engineer. | Bangalore
 *    3    | Carol | Delhi| 70000  |    2     ->    2    | Marketing | Mumbai
 *
 *  FK (fk_dept_id) is in the employees table - Employee is the OWNER.
 *
 *  DEFAULT STRATEGIES SUMMARY (@ManyToOne):
 *  -----------------------------------------
 *  Aspect            | Default  | Recommendation
 *  ------------------|----------|-----------------------------------------
 *  Fetch Type        | EAGER    | Change to LAZY (avoid N+1 problem!)
 *  Cascade           | NONE     | Usually NONE - save dept before employee
 *  Optional          | true     | Set false if dept is always required
 *  FK Column Name    | <entity>_<pk>  | Use @JoinColumn for explicit naming
 *
 *  SAVE ORDER (when cascade = NONE):
 *  -----------------------------------
 *  IMPORTANT: When cascade is NONE, you MUST save Department FIRST,
 *  then assign it to Employee, then save Employee.
 *
 *  Department engineering = departmentRepo.save(new Department("Engineering", "Bangalore"));
 *  Employee alice = new Employee("Alice", "Pune", 60000.0, engineering);
 *  employeeRepo.save(alice);    <- Works! dept already has an ID
 *
 *  If you do it the other way (save employee first, then dept), JPA throws:
 *  TransientPropertyValueException: object references an unsaved transient instance
 *
 * ================================================================
 */
@Entity(name = "MtoEmployee")           // Unique JPA entity name
@Table(name = "mto_employees")
@Data
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emp_id")
    private Long empId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "city")
    private String city;

    @Column(name = "salary")
    private Double salary;

    // ============================================================
    //  MANY-TO-ONE MAPPING (UNIDIRECTIONAL - ALWAYS OWNING SIDE)
    // ============================================================
    //
    //  @ManyToOne: MANY employees to ONE department
    //    -> Employee owns the FK (always true for @ManyToOne)
    //    -> fk_dept_id column is created in mto_employees table
    //
    //  fetch = LAZY:
    //    -> Department is NOT loaded when Employee is fetched
    //    -> Loaded only when employee.getDepartment() is called
    //    -> CRITICAL: Change from default EAGER to LAZY!
    //    -> Prevents N+1 query problem
    //
    //  cascade = NONE (default):
    //    -> Department must be saved BEFORE Employee
    //    -> employee.setDepartment(savedDept) then save employee
    //
    //  nullable = false:
    //    -> Every employee must belong to a department
    //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_dept_id", nullable = false)
    private Department department;

    public Employee(String name, String city, Double salary, Department department) {
        this.name = name;
        this.city = city;
        this.salary = salary;
        this.department = department;
    }

    @Override
    public String toString() {
        // Note: Not printing department here to avoid triggering LAZY loading in toString()
        return "Employee{empId=" + empId + ", name='" + name + "', city='" + city + "', salary=" + salary + "}";
    }
}
