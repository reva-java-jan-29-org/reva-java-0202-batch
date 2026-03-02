package com.training.onetoone.unidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Profile entity (OneToOne Unidirectional).
 *
 * With CascadeType.ALL on Employee.profile, you rarely need to call
 * profileRepository.save() directly — saving Employee auto-saves Profile.
 *
 * However, this repository is useful if you need to:
 * - Query profiles directly
 * - Save a profile independently before linking it to an employee
 */
@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

}
