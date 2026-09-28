package com.portal_cm.portal_cm.users.role;


import com.portal_cm.portal_cm.users.role.dto.RoleRequest;
import com.portal_cm.portal_cm.users.role.dto.RoleResponse;
import com.portal_cm.portal_cm.users.role.dto.RoleUpdateRequest;
import com.portal_cm.portal_cm.users.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

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
    public RoleResponse create(RoleRequest request) {
        String roleValue = normalizeRole(request.role());

        if (roleRepository.existsByRoleIgnoreCase(roleValue)) {
            throw new IllegalArgumentException("Já existe uma role cadastrada com essa permissão.");
        }

        Role role = new Role();
        role.setName(request.name().trim());
        role.setRole(roleValue);

        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse update(Integer id, RoleUpdateRequest request) {
        Role role = findRole(id);

        if (!role.isActive()) {
            throw new IllegalArgumentException("Não é possível editar uma role inativa.");
        }

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Preencha o campo de nome.");
            }
            role.setName(name);
        }

        if (request.role() != null) {
            String roleValue = normalizeRole(request.role());
            if (roleValue.isEmpty()) {
                throw new IllegalArgumentException("Preencha o campo de role/permissão.");
            }
            if (!roleValue.equalsIgnoreCase(role.getRole())
                    && roleRepository.existsByRoleIgnoreCaseAndIdNot(roleValue, id)) {
                throw new IllegalArgumentException("Já existe uma role cadastrada com essa permissão.");
            }
            role.setRole(roleValue);
        }

        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse deactivate(Integer id) {
        Role role = findRole(id);

        if (!role.isActive()) {
            throw new IllegalArgumentException("A role já está inativa.");
        }

        if (userRepository.existsByRole_IdAndIsActiveTrue(id)) {
            throw new IllegalArgumentException("Não é possível desativar uma role atribuída a usuários ativos.");
        }

        role.setActive(false);
        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse reactivate(Integer id) {
        Role role = findRole(id);

        if (role.isActive()) {
            throw new IllegalArgumentException("A role já está ativa.");
        }

        role.setActive(true);
        return toResponse(roleRepository.save(role));
    }

    private Role findRole(Integer id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Não foi encontrada role com o ID informado."));
    }

    private String normalizeRole(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getRole(),
                role.isActive(),
                userRepository.countByRole_Id(role.getId())
        );
    }
}
