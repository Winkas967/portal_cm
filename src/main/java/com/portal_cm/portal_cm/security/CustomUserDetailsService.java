package com.portal_cm.portal_cm.security;

import com.portal_cm.portal_cm.users.role.Role;
import com.portal_cm.portal_cm.users.user.User;
import com.portal_cm.portal_cm.users.user.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Usado no login. O nome não diferencia maiúsculas de minúsculas. */
    @Override
    @Transactional(readOnly = true)
    public UserPrincipal loadUserByUsername(String name) throws UsernameNotFoundException {
        User user = userRepository.findByNameIgnoreCase(name.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + name));
        return toPrincipal(user);
    }

    /** Usado a cada requisição autenticada, a partir do ID guardado no token. */
    @Transactional(readOnly = true)
    public UserPrincipal loadUserById(Integer id) throws UsernameNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + id));
        return toPrincipal(user);
    }

    private UserPrincipal toPrincipal(User user) {
        Role role = user.getRole();
        List<GrantedAuthority> authorities = (role != null && role.isActive())
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role.getRole().trim().toUpperCase(Locale.ROOT)))
                : List.of();

        return new UserPrincipal(user, authorities);
    }
}
