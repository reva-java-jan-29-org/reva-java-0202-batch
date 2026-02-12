package com.training.campussystem.model.enums;

import java.util.Objects;

public final class Person {
	private final int id;
	private final String name;
	private final Role role;
	private final String department;
	private final String hostelId; // can be null
	private final double finesDue;

	public Person(int id, String name, Role role, String department, String hostelId, double finesDue) {
		this.id = id;
		this.name = Objects.requireNonNull(name);
		this.role = Objects.requireNonNull(role);
		this.department = Objects.requireNonNull(department);
		this.hostelId = hostelId;
		this.finesDue = finesDue;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Role getRole() {
		return role;
	}

	public String getDepartment() {
		return department;
	}

	public String getHostelId() {
		return hostelId;
	}

	public double getFinesDue() {
		return finesDue;
	}
}
