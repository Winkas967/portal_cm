package com.portal_cm.portal_cm.users.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByNameIgnoreCase(String name);
    Optional<User> findByName(String name);
    long countByRole_Id(Integer roleId);
    boolean existsByRole_IdAndIsActiveTrue(Integer roleId);
}
