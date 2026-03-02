package com.training.onetoone.bidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Profile entity (OneToOne Bidirectional).
 *
 * With bidirectional mapping, you can navigate from Profile -> Employee
 * directly in Java, but to query from the DB you still use repositories.
 *
 * Since mappedBy is on Profile.employee, Profile does NOT own the FK.
 * To find the employee linked to a profile, you query Employee side.
 */
@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByBioContaining(String keyword);
}
