package com.training.onetomany.bidirectional;

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
 *  ONE-TO-MANY BIDIRECTIONAL  -  OWNING SIDE (Employee / "Many" side)
 * ================================================================
 *
 *  Scenario: Department <---> [Employee, ...] (both know each other)
 *
 *  IN BIDIRECTIONAL @OneToMany, THE @ManyToOne SIDE IS ALWAYS THE OWNER.
 *  -------------------------------------------------------------------------
 *  This is a fundamental JPA rule:
 *    - @ManyToOne side   -> OWNING side   (has @JoinColumn, has the FK)
 *    - @OneToMany side   -> INVERSE side  (has mappedBy, no FK column)
 *
 *  WHY IS THE @ManyToOne SIDE THE OWNER?
 *  ---------------------------------------
 *  Logically, the "many" side (Employee) has a FK to the "one" side (Department).
 *  It makes sense for the entity that HOLDS the FK column to be the owner.
 *  Each Employee row has a fk_dept_id column -> Employee is the owner.
 *
 *  DATABASE TABLE  (otm_bi_employees):
 *  --------------------------------------
 *  emp_id  |  name   |  city  |  salary  |  fk_dept_id
 *    1     |  Alice  |  Pune  |  60000   |      1       <- FK to Engineering
 *    2     |  Bob    |  Mumbai|  55000   |      1       <- FK to Engineering
 *    3     |  Carol  |  Delhi |  70000   |      2       <- FK to Marketing
 *
 *  FK (fk_dept_id) lives in the employees table - Employee is the OWNER.
 *
 *  DEFAULT STRATEGIES SUMMARY (@ManyToOne):
 *  ------------------------------------------
 *  Aspect            | Default              | Meaning
 *  ------------------|----------------------|--------------------------------
 *  Fetch Type        | EAGER                | Department loaded WITH Employee!
 *  Cascade           | NONE                 | Saving employee won't save dept
 *  Optional          | true                 | Employee can have null department
 *
 *  RECOMMENDATIONS:
 *  -----------------
 *  @ManyToOne(fetch = FetchType.LAZY)   // avoid loading dept with every emp query
 *  @JoinColumn(name = "fk_dept_id", nullable = false)
 *
 * ================================================================
 */
@Entity(name = "OtmBiEmployee")         // Unique JPA entity name
@Table(name = "otm_bi_employees")
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
    //  MANY-TO-ONE MAPPING - OWNING SIDE of the bidirectional relationship
    // ============================================================
    //
    //  Many employees belong to ONE department.
    //  This side OWNS the relationship (has the FK column).
    //
    //  @JoinColumn(name = "fk_dept_id"):
    //    -> Defines the FK column name in otm_bi_employees table
    //    -> nullable = false: employee must always belong to a department
    //
    //  fetch = LAZY (RECOMMENDED):
    //    -> By default @ManyToOne is EAGER (loads dept with every emp fetch)
    //    -> LAZY means department is only loaded when you call emp.getDepartment()
    //    -> IMPORTANT: ALWAYS change @ManyToOne to LAZY for production code!
    //      (Eager @ManyToOne can cause N+1 query problem in collections)
    //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_dept_id", nullable = false)
    private Department department;

    public Employee(String name, String city, Double salary) {
        this.name = name;
        this.city = city;
        this.salary = salary;
    }

    @Override
    public String toString() {
        // Note: Intentionally NOT printing department to avoid infinite recursion
        return "Employee{empId=" + empId + ", name='" + name + "', city='" + city + "', salary=" + salary + "}";
    }
}
