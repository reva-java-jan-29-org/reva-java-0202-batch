package com.training.onetoone.unidirectional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ================================================================
 *  ONE-TO-ONE UNIDIRECTIONAL  -  OWNING SIDE
 * ================================================================
 *
 *  Scenario: Each Employee has exactly ONE Profile.
 *            Navigation is ONE-WAY: Employee ---> Profile
 *            Profile has NO reference back to Employee.
 *
 *  WHAT "OWNING SIDE" MEANS:
 *  --------------------------
 *  The owning side is the entity that:
 *    1. Holds the @JoinColumn annotation
 *    2. Has the actual FK column in its database table
 *    3. Controls FK value - JPA reads FK from THIS side when persisting
 *
 *  In unidirectional @OneToOne, the side that declares the
 *  @OneToOne field IS ALWAYS the owning side.
 *
 *  DEFAULT BEHAVIOR (without @JoinColumn):
 *  -----------------------------------------
 *    FK column name = <referenced_entity_name>_<referenced_pk_column>
 *    e.g., for "Profile" with PK "profile_id":  profile_profile_id
 *
 *  WITH @JoinColumn (RECOMMENDED):
 *  ---------------------------------
 *    Use @JoinColumn to give the FK a meaningful, explicit name.
 *    e.g., @JoinColumn(name = "fk_profile_id")
 *
 *  DATABASE TABLES:
 *  -----------------
 *  oto_uni_employees:                     oto_uni_profiles:
 *  emp_id | name  | fk_profile_id   <FK>  profile_id | bio | linkedin_url
 *    1    | Alice |       1    ----------->     1     | ... | ...
 *    2    | Bob   |       2    ----------->     2     | ... | ...
 *
 *  DEFAULT STRATEGIES SUMMARY (@OneToOne):
 *  ----------------------------------------
 *  Aspect            | Default              | Meaning
 *  ------------------|----------------------|--------------------------------
 *  Mapping Strategy  | FK-based (JoinColumn)| FK column in the owning table
 *  FK Ownership      | Side with @JoinColumn| Only owner writes the FK value
 *  Fetch Type        | EAGER                | Profile loaded with Employee
 *  Cascade           | NONE                 | Must save Profile separately
 *  Optional          | true                 | FK can be null (profile optional)
 *
 *  RECOMMENDATIONS:
 *  -----------------
 *  @OneToOne(
 *      fetch = FetchType.LAZY,         // avoid loading profile unless needed
 *      cascade = CascadeType.ALL       // auto-save/delete profile with employee
 *  )
 *  @JoinColumn(
 *      name = "fk_profile_id",         // explicit FK column name
 *      nullable = false,               // employee must always have a profile
 *      unique = true                   // enforces true one-to-one at DB level
 *  )
 *
 * ================================================================
 */
@Entity(name = "OtoUniEmployee")        // Unique JPA entity name (multiple 'Employee' classes exist in different packages)
@Table(name = "oto_uni_employees")      // Table prefix avoids name collision with other packages
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

    // ============================================================
    //  ONE-TO-ONE MAPPING (UNIDIRECTIONAL - OWNING SIDE)
    // ============================================================
    //
    //  fetch = LAZY  : Profile is NOT loaded until accessed
    //                  (recommended - avoids unnecessary joins)
    //
    //  cascade = ALL : ALL operations (persist, merge, remove,
    //                  refresh, detach) are propagated to Profile.
    //                  Saving Employee also saves Profile.
    //                  Deleting Employee also deletes Profile.
    //
    //  @JoinColumn   : Defines the FK column in oto_uni_employees table
    //  nullable=false: Employee MUST have a Profile
    //  unique=true   : Enforces 1:1 at the database level (DB constraint)
    //
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "fk_profile_id", nullable = false, unique = true)
    private Profile profile;

    // --------------------------------------------------------
    // Convenience constructor (without id - for new entities)
    // --------------------------------------------------------
    public Employee(String name, String city, Profile profile) {
        this.name = name;
        this.city = city;
        this.profile = profile;
    }

    @Override
    public String toString() {
        return "Employee{empId=" + empId + ", name='" + name + "', city='" + city + "'}";
    }
}
