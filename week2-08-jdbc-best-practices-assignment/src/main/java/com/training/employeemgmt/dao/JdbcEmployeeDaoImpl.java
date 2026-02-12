package com.training.employeemgmt.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.training.employeemgmt.db.DbConnectionFactory;
import com.training.employeemgmt.model.Employee;

public final class JdbcEmployeeDaoImpl implements EmployeeDao {

    private static final String INSERT_SQL =
            "INSERT INTO employees (emp_code, full_name, email, department, salary) VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID_SQL =
            "SELECT id, emp_code, full_name, email, department, salary, created_at, updated_at FROM employees WHERE id=?";

    private static final String SELECT_BY_CODE_SQL =
            "SELECT id, emp_code, full_name, email, department, salary, created_at, updated_at FROM employees WHERE emp_code=?";

    private static final String SELECT_ALL_SQL =
            "SELECT id, emp_code, full_name, email, department, salary, created_at, updated_at FROM employees ORDER BY id LIMIT ? OFFSET ?";

    private static final String SELECT_BY_DEPT_SQL =
            "SELECT id, emp_code, full_name, email, department, salary, created_at, updated_at FROM employees WHERE department=? ORDER BY id LIMIT ? OFFSET ?";

    private static final String UPDATE_SQL =
            "UPDATE employees SET full_name=?, email=?, department=?, salary=? WHERE id=?";

    private static final String DELETE_SQL =
            "DELETE FROM employees WHERE id=?";

    private final DbConnectionFactory connectionFactory;

    public JdbcEmployeeDaoImpl(DbConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Employee create(Employee employee) {
        try (Connection con = connectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, employee.getEmpCode());
            ps.setString(2, employee.getFullName());
            ps.setString(3, employee.getEmail());
            ps.setString(4, employee.getDepartment());
            ps.setBigDecimal(5, employee.getSalary());

            int affected = ps.executeUpdate();
            if (affected != 1) {
                throw new DaoException("Insert failed: expected 1 row, affected=" + affected, null);
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DaoException("Insert failed: no generated key returned", null);
                }
                long id = keys.getLong(1);

                // Fetch the inserted row (ensures timestamps match DB)
                return findById(id).orElseThrow(() -> new DaoException("Inserted row not found for id=" + id, null));
            }

        } catch (SQLException e) {
            throw new DaoException("Error creating employee", e);
        }
    }

    @Override
    public Optional<Employee> findById(long id) {
        try (Connection con = connectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_ID_SQL)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DaoException("Error finding employee by id=" + id, e);
        }
    }

    @Override
    public Optional<Employee> findByEmpCode(String empCode) {
        try (Connection con = connectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_CODE_SQL)) {

            ps.setString(1, empCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DaoException("Error finding employee by empCode=" + empCode, e);
        }
    }

    @Override
    public List<Employee> findAll(int limit, int offset) {
        try (Connection con = connectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL_SQL)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<Employee> out = new ArrayList<>();
                while (rs.next()) out.add(mapRow(rs));
                return out;
            }

        } catch (SQLException e) {
            throw new DaoException("Error listing employees", e);
        }
    }

    @Override
    public List<Employee> findByDepartment(String department, int limit, int offset) {
        try (Connection con = connectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_DEPT_SQL)) {

            ps.setString(1, department);
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<Employee> out = new ArrayList<>();
                while (rs.next()) out.add(mapRow(rs));
                return out;
            }

        } catch (SQLException e) {
            throw new DaoException("Error listing employees by department=" + department, e);
        }
    }

    @Override
    public Employee update(Employee employee) {
        if (employee.getId() == null) {
            throw new DaoException("Update requires employee.id (was null)", null);
        }

        try (Connection con = connectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_SQL)) {

            ps.setString(1, employee.getFullName());
            ps.setString(2, employee.getEmail());
            ps.setString(3, employee.getDepartment());
            ps.setBigDecimal(4, employee.getSalary());
            ps.setLong(5, employee.getId());

            int affected = ps.executeUpdate();
            if (affected != 1) {
                throw new DaoException("Update failed: expected 1 row, affected=" + affected, null);
            }

            return findById(employee.getId())
                    .orElseThrow(() -> new DaoException("Updated row not found for id=" + employee.getId(), null));

        } catch (SQLException e) {
            throw new DaoException("Error updating employee id=" + employee.getId(), e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        try (Connection con = connectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_SQL)) {

            ps.setLong(1, id);
            int affected = ps.executeUpdate();
            return affected == 1;

        } catch (SQLException e) {
            throw new DaoException("Error deleting employee id=" + id, e);
        }
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String empCode = rs.getString("emp_code");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String department = rs.getString("department");
        BigDecimal salary = rs.getBigDecimal("salary");

        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");

        return Employee.builder()
                .id(id)
                .empCode(empCode)
                .fullName(fullName)
                .email(email)
                .department(department)
                .salary(salary)
                .createdAt(created != null ? created.toInstant() : null)
                .updatedAt(updated != null ? updated.toInstant() : null)
                .build();
    }
}

