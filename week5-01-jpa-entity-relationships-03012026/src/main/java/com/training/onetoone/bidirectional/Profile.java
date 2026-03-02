package com.training.onetoone.bidirectional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ================================================================
 *  ONE-TO-ONE BIDIRECTIONAL  -  INVERSE (NON-OWNING) SIDE
 * ================================================================
 *
 *  Scenario: Employee <---> Profile (both know about each other)
 *            Navigation is TWO-WAY: Employee <-> Profile
 *
 *  WHAT "INVERSE / NON-OWNING SIDE" MEANS:
 *  -----------------------------------------
 *  The non-owning side is identified by the "mappedBy" attribute.
 *  It means:
 *    1. This side does NOT have @JoinColumn
 *    2. There is NO FK column in this table
 *    3. JPA IGNORES changes to this field when writing FK values
 *    4. This side is purely for in-memory navigation convenience
 *
 *  mappedBy = "profile"  -->  "The field named 'profile' in the
 *                              Employee class OWNS this relationship."
 *
 *  IMPORTANT - mappedBy refers to:
 *    -> the FIELD NAME in the owning class  (not the column name!)
 *    -> "profile" is the field name in Employee.java
 *
 *  DATABASE TABLE  (oto_bi_profiles):
 *  ------------------------------------
 *  profile_id  |  bio  |  linkedin_url
 *  ------------|-------|---------------
 *       1      | ...   |    ...
 *
 *  NOTE: No FK column here. The FK (fk_profile_id) is in the
 *        oto_bi_employees table on the OWNING side (Employee).
 *
 *  COMMON MISTAKE:
 *  ----------------
 *  If you add @JoinColumn on BOTH sides, JPA creates TWO FK columns
 *  (one in each table) — which is NOT a true bidirectional mapping!
 *  Only the OWNING side should have @JoinColumn.
 *
 * ================================================================
 */
@Entity(name = "OtoBiProfile")          // Unique JPA entity name
@Table(name = "oto_bi_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    // ============================================================
    //  ONE-TO-ONE BIDIRECTIONAL - INVERSE / NON-OWNING SIDE
    // ============================================================
    //
    //  mappedBy = "profile"
    //    -> Tells JPA: "The 'profile' field in Employee owns this relationship"
    //    -> This side has NO @JoinColumn
    //    -> JPA will NOT write any FK from Profile side
    //    -> Only used for in-memory bidirectional navigation
    //
    //  WHY mappedBy IS IMPORTANT:
    //    Without mappedBy, JPA would treat this as TWO separate
    //    relationships and create FK columns in BOTH tables!
    //
    @OneToOne(mappedBy = "profile")
    private Employee employee;

    // --------------------------------------------------------
    // Convenience constructor (without id and back-reference)
    // --------------------------------------------------------
    public Profile(String bio, String linkedinUrl) {
        this.bio = bio;
        this.linkedinUrl = linkedinUrl;
    }

    @Override
    public String toString() {
        // Note: Intentionally NOT printing employee here to avoid infinite recursion
        return "Profile{profileId=" + profileId + ", bio='" + bio + "', linkedinUrl='" + linkedinUrl + "'}";
    }
}
