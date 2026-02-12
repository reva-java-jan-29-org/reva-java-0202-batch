package com.training.employeemgmt.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class Employee {
	private final Long id; // null for new employees
	private final String empCode;
	private final String fullName;
	private final String email;
	private final String department;
	private final BigDecimal salary;
	private final Instant createdAt; // optional in app layer
	private final Instant updatedAt;

	private Employee(Builder b) {
		this.id = b.id;
		this.empCode = Objects.requireNonNull(b.empCode);
		this.fullName = Objects.requireNonNull(b.fullName);
		this.email = Objects.requireNonNull(b.email);
		this.department = Objects.requireNonNull(b.department);
		this.salary = Objects.requireNonNull(b.salary);
		this.createdAt = b.createdAt;
		this.updatedAt = b.updatedAt;
	}

	public Long getId() {
		return id;
	}

	public String getEmpCode() {
		return empCode;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	public String getDepartment() {
		return department;
	}

	public BigDecimal getSalary() {
		return salary;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Builder toBuilder() {
		return new Builder().id(id).empCode(empCode).fullName(fullName).email(email).department(department)
				.salary(salary).createdAt(createdAt).updatedAt(updatedAt);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private Long id;
		private String empCode;
		private String fullName;
		private String email;
		private String department;
		private BigDecimal salary;
		private Instant createdAt;
		private Instant updatedAt;

		public Builder id(Long id) {
			this.id = id;
			return this;
		}

		public Builder empCode(String empCode) {
			this.empCode = empCode;
			return this;
		}

		public Builder fullName(String fullName) {
			this.fullName = fullName;
			return this;
		}

		public Builder email(String email) {
			this.email = email;
			return this;
		}

		public Builder department(String department) {
			this.department = department;
			return this;
		}

		public Builder salary(BigDecimal salary) {
			this.salary = salary;
			return this;
		}

		public Builder createdAt(Instant createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		public Builder updatedAt(Instant updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		public Employee build() {
			return new Employee(this);
			
		}
	}

	@Override
	public String toString() {
		return "Employee{id=" + id + ", empCode='" + empCode + "', fullName='" + fullName + "', email='" + email
				+ "', department='" + department + "', salary=" + salary + "}";
	}
}
