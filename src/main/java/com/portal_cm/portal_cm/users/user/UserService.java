package com.portal_cm.portal_cm.users.user;


import com.portal_cm.portal_cm.users.role.Role;
import com.portal_cm.portal_cm.users.role.RoleRepository;
import com.portal_cm.portal_cm.users.user.dto.UserRequest;
import com.portal_cm.portal_cm.users.user.dto.UserResponse;
import com.portal_cm.portal_cm.users.user.dto.UserUpdateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    private static final String ENTITY_TYPE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Integer id) {
        return toResponse(findUser(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String name = request.name().trim();

        if (userRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Já existe um usuário com este nome.");
        }

        User user = new User();
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(findActiveRole(request.roleId()));

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Integer id, UserUpdateRequest request, Integer currentUserId) {
        User user = findUser(id);

        if (!user.isActive()) {
            throw new IllegalArgumentException("Não é possível editar um usuário inativo.");
        }

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Preencha o campo de nome.");
            }
            if (!name.equalsIgnoreCase(user.getName())
                    && userRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new IllegalArgumentException("Já existe um usuário com este nome.");
            }
            user.setName(name);
        }

        if (request.password() != null) {
            if (request.password().isBlank()) {
                throw new IllegalArgumentException("Preencha o campo de senha.");
            }
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (request.roleId() != null) {
            Role newRole = findActiveRole(request.roleId());
            if (Objects.equals(id, currentUserId) && !ADMIN_ROLE.equalsIgnoreCase(newRole.getRole())) {
                throw new IllegalArgumentException("Você não pode remover o seu próprio acesso de administrador.");
            }
            user.setRole(newRole);
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse deactivate(Integer id, Integer currentUserId) {
        User user = findUser(id);

        if (!user.isActive()) {
            throw new IllegalArgumentException("O usuário já está inativo.");
        }

        if (Objects.equals(id, currentUserId)) {
            throw new IllegalArgumentException("Você não pode desativar o seu próprio usuário.");
        }

        user.setActive(false);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse reactivate(Integer id) {
        User user = findUser(id);

        if (user.isActive()) {
            throw new IllegalArgumentException("O usuário já está ativo.");
        }

        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    private User findUser(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Não foi encontrado usuário com o ID informado."));
    }

    private Role findActiveRole(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Não foi encontrada role com o ID informado."));

        if (!role.isActive()) {
            throw new IllegalArgumentException("Não é possível atribuir uma role inativa a um usuário.");
        }
        return role;
    }

    private UserResponse toResponse(User user) {
        Role role = user.getRole();
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.isActive(),
                role != null ? role.getId() : null,
                role != null ? role.getName() : null,
                role != null ? role.getRole() : null
        );
    }
}
