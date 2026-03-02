package com.training.onetoone.unidirectional;

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
 *  ONE-TO-ONE UNIDIRECTIONAL  -  NON-REFERENCED SIDE
 * ================================================================
 *
 *  In a UNIDIRECTIONAL relationship, Profile has absolutely NO
 *  knowledge of Employee. It is a plain standalone entity.
 *
 *  KEY POINTS:
 *  - No @OneToOne annotation here
 *  - Profile does NOT hold any FK column
 *  - You CANNOT navigate from Profile -> Employee (by design)
 *  - Profile can exist independently (no dependency on Employee)
 *
 *  DATABASE TABLE  (oto_uni_profiles):
 *  ------------------------------------
 *  profile_id  |  bio
 *  ------------|--------------------
 *       1      |  Alice's bio
 *       2      |  Bob's bio
 *
 *  NOTE: No FK column here. The FK lives in the employees table.
 *
 * ================================================================
 */
@Entity(name = "OtoUniProfile")         // Unique JPA entity name (multiple 'Profile' classes exist in different packages)
@Table(name = "oto_uni_profiles")       // Table prefix avoids name collision with other packages
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

    // --------------------------------------------------------
    // Convenience constructor (without id - for new entities)
    // --------------------------------------------------------
    public Profile(String bio, String linkedinUrl) {
        this.bio = bio;
        this.linkedinUrl = linkedinUrl;
    }

    @Override
    public String toString() {
        return "Profile{profileId=" + profileId + ", bio='" + bio + "', linkedinUrl='" + linkedinUrl + "'}";
    }
}
