package com.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ================================================================
 *  JPA ENTITY RELATIONSHIPS - COMPREHENSIVE DEMO PROJECT
 * ================================================================
 *
 *  This project covers all four JPA entity relationship types,
 *  each demonstrated with both unidirectional and bidirectional
 *  mappings where applicable.
 *
 *  PACKAGE STRUCTURE:
 *  ------------------
 *  com.training
 *  ├── onetoone
 *  │   ├── unidirectional   -> Employee ---> Profile
 *  │   └── bidirectional    -> Employee <--> Profile
 *  ├── onetomany
 *  │   ├── unidirectional   -> Department ---> [Employee, ...]
 *  │   └── bidirectional    -> Department <--> [Employee, ...]
 *  ├── manytoone
 *  │   └── (unidirectional) -> [Employee, ...] ---> Department
 *  └── manytomany
 *      ├── unidirectional   -> Student ---> [Course, ...]
 *      └── bidirectional    -> Student <--> [Course, ...]
 *
 *  KEY CONCEPTS COVERED:
 *  ----------------------
 *  1. Owning side vs Inverse (non-owning) side
 *  2. FK column placement and naming
 *  3. @JoinColumn customization
 *  4. mappedBy - what it means and where it goes
 *  5. Default fetch types (EAGER vs LAZY)
 *  6. Cascade types (NONE by default, recommendations)
 *  7. Join tables (ManyToMany)
 *  8. Keeping both sides in sync (bidirectional)
 *  9. orphanRemoval vs CascadeType.REMOVE
 *
 *  TESTS:
 *  ------
 *  Tests use @DataJpaTest with H2 in-memory database.
 *  No MySQL required to run tests.
 *  Each test class covers: Create, Read, Update, Delete,
 *  Cascade behavior, and Fetch behavior.
 *
 * ================================================================
 */
@SpringBootApplication
public class JpaEntityRelationshipsApplication {

	public static void main(String[] args) {
		SpringApplication.run(JpaEntityRelationshipsApplication.class, args);
	}

}
