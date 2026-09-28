package com.portal_cm.portal_cm.users.role;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    boolean existsByRoleIgnoreCase(String role);
    boolean existsByRoleIgnoreCaseAndIdNot(String role, Integer id);
}
