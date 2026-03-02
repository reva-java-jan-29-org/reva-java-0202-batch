package com.training.onetomany.unidirectional;

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
 *  ONE-TO-MANY UNIDIRECTIONAL  -  THE "MANY" SIDE (Employee)
 * ================================================================
 *
 *  In UNIDIRECTIONAL @OneToMany, the Employee entity has
 *  NO knowledge of Department. It is a plain standalone entity.
 *
 *  KEY POINTS:
 *  - No @ManyToOne annotation on this class
 *  - Cannot navigate Employee -> Department
 *  - The FK column (fk_dept_id) is placed on THIS table
 *    because of @JoinColumn on Department's @OneToMany
 *
 *  DATABASE TABLE  (otm_uni_employees):
 *  ---------------------------------------
 *  emp_id  |  name   |  city  |  fk_dept_id
 *    1     |  Alice  |  Pune  |      1
 *    2     |  Bob    |  Mumbai|      1
 *    3     |  Carol  |  Delhi |      2
 *
 *  The fk_dept_id is a FK column added to this table because
 *  of the @JoinColumn annotation on Department.employees field.
 *  Without @JoinColumn on the @OneToMany side, JPA would create
 *  a separate JOIN TABLE instead.
 *
 * ================================================================
 */
@Entity(name = "OtmUniEmployee")        // Unique JPA entity name
@Table(name = "otm_uni_employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    // No @ManyToOne here - this is UNIDIRECTIONAL
    // Department is completely unknown to Employee in this mapping

    public Employee(String name, String city, Double salary) {
        this.name = name;
        this.city = city;
        this.salary = salary;
    }

    @Override
    public String toString() {
        return "Employee{empId=" + empId + ", name='" + name + "', city='" + city + "', salary=" + salary + "}";
    }
}
