package com.training.manytoone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ================================================================
 *  MANY-TO-ONE (UNIDIRECTIONAL)  -  THE "ONE" SIDE (Department)
 * ================================================================
 *
 *  In this unidirectional @ManyToOne mapping:
 *    - Many Employees belong to ONE Department
 *    - Employee ---> Department  (Employee knows about Department)
 *    - Department has NO reference back to its employees
 *
 *  Department is a plain standalone entity here.
 *  It is the "TARGET" of the relationship but does NOT declare it.
 *
 *  @ManyToOne IS THE SIMPLEST JPA RELATIONSHIP BECAUSE:
 *  -------------------------------------------------------
 *  - Only ONE annotation needed (@ManyToOne on Employee side)
 *  - No mappedBy needed (this is unidirectional)
 *  - No join table created
 *  - Always natural: FK is in the "many" side (employees table)
 *
 *  DATABASE TABLE  (mto_departments):
 *  ------------------------------------
 *  dept_id  |  dept_name    |  location
 *    1      | Engineering   |  Bangalore
 *    2      | Marketing     |  Mumbai
 *
 *  No FK column here. FK (fk_dept_id) is in the employees table.
 *
 * ================================================================
 */
@Entity(name = "MtoDepartment")         // Unique JPA entity name
@Table(name = "mto_departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "dept_name", nullable = false, unique = true)
    private String deptName;

    @Column(name = "location")
    private String location;

    public Department(String deptName, String location) {
        this.deptName = deptName;
        this.location = location;
    }

    @Override
    public String toString() {
        return "Department{deptId=" + deptId + ", deptName='" + deptName + "', location='" + location + "'}";
    }
}
