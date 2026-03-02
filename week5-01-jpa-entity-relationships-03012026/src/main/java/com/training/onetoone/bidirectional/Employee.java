package com.training.onetoone.bidirectional;

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
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ================================================================
 *  ONE-TO-ONE BIDIRECTIONAL  -  OWNING SIDE
 * ================================================================
 *
 *  Scenario: Employee <---> Profile (both know about each other)
 *
 *  OWNING SIDE RULES:
 *  -------------------
 *  1. Has the @JoinColumn annotation
 *  2. Has the actual FK column in the database table
 *  3. JPA writes the FK value using this side's reference
 *  4. When you set employee.setProfile(profile), the FK is saved
 *
 *  HOW TO IDENTIFY THE OWNING SIDE:
 *  ----------------------------------
 *  The side WITHOUT mappedBy = the OWNING side
 *  The side WITH    mappedBy = the INVERSE (non-owning) side
 *
 *  CRITICAL RULE FOR BIDIRECTIONAL RELATIONSHIPS:
 *  -----------------------------------------------
 *  ALWAYS SET BOTH SIDES of a bidirectional relationship:
 *
 *    employee.setProfile(profile);      <- JPA uses this to write FK
 *    profile.setEmployee(employee);     <- required for in-memory sync
 *
 *  If you only set employee.setProfile(profile) but forget
 *  profile.setEmployee(employee), then:
 *    - The database is correct (FK is saved)
 *    - But profile.getEmployee() returns null in the same session
 *      (stale in-memory state)
 *
 *  Use the helper method setProfileWithSync() below to avoid this.
 *
 *  DATABASE TABLES:
 *  -----------------
 *  oto_bi_employees:                    oto_bi_profiles:
 *  emp_id | name  | fk_profile_id  FK   profile_id | bio | linkedin_url
 *    1    | Alice |       1   --------->      1     | ... | ...
 *
 *  FK is in employees table (owning side)
 *  profiles table has NO FK column
 *
 *  DEFAULT STRATEGIES SUMMARY (@OneToOne):
 *  ----------------------------------------
 *  Aspect            | Default              | Recommendation
 *  ------------------|----------------------|--------------------------------
 *  Mapping Strategy  | FK-based (JoinColumn)| Keep as FK-based
 *  FK Ownership      | Side without mappedBy| Employee (this class)
 *  Fetch Type        | EAGER                | Change to LAZY for performance
 *  Cascade           | NONE                 | CascadeType.ALL if profile is
 *                    |                      | always created with employee
 *  Optional          | true                 | Set false if profile is required
 *
 * ================================================================
 */
@Entity(name = "OtoBiEmployee")         // Unique JPA entity name
@Table(name = "oto_bi_employees")
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

    // ============================================================
    //  ONE-TO-ONE BIDIRECTIONAL - OWNING SIDE
    // ============================================================
    //
    //  This side has @JoinColumn -> it is the OWNER
    //  Profile side has mappedBy="profile" -> it is the INVERSE
    //
    //  cascade = ALL : persist, merge, remove, refresh, detach
    //                  are all propagated to Profile automatically
    //
    //  fetch = LAZY  : Profile is loaded only when accessed
    //                  (better performance than default EAGER)
    //
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "fk_profile_id", nullable = false, unique = true)
    private Profile profile;

    // ============================================================
    //  HELPER METHOD - Keep both sides in sync (Best Practice)
    // ============================================================
    //
    //  In bidirectional relationships, always use a helper method
    //  to set both sides together. This prevents stale in-memory state.
    //
    //  ALWAYS use this instead of calling setProfile() directly
    //  when you also want profile.getEmployee() to work correctly.
    //
    public void setProfileWithSync(Profile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setEmployee(this);  // Set the back-reference
        }
    }

    public Employee(String name, String city) {
        this.name = name;
        this.city = city;
    }

    @Override
    public String toString() {
        // Note: Intentionally NOT printing profile here to avoid infinite recursion
        return "Employee{empId=" + empId + ", name='" + name + "', city='" + city + "'}";
    }
}
