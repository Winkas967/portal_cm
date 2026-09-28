package com.portal_cm.portal_cm.users.role;


import com.portal_cm.portal_cm.users.role.dto.RoleRequest;
import com.portal_cm.portal_cm.users.role.dto.RoleResponse;
import com.portal_cm.portal_cm.users.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private static final String ENTITY_TYPE = "ROLE";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public RoleRepository create(RoleRequest request) {
        String roleValue = request.role().trim();

        if (roleRepository.existsByRoleIgnoreCase((roleValue)) {
            throw new IllegalArgumentException("Já existe uma role cadastrada com essa permissão");
        }

        Role role = new Role();
        role.setName(request.name().trim());
        role.setRole(roleValue);

        Role saved = roleRepository.save(role);
        auditLogService.record(ENTITY_TYPE, saved.getId(), "CREATE", "Role cadastrado:" + saved.getRole());
        return toResponde(saved);
    }

    @Transactional
    public RoleResponse
}
