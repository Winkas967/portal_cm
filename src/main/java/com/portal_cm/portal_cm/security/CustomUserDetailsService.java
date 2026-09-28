package com.portal_cm.portal_cm.security;

import com.portal_cm.portal_cm.users.role.Role;
import com.portal_cm.portal_cm.users.user.User;
import com.portal_cm.portal_cm.users.user.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        User user = userRepository.findByName(name)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + name));

        Role role = user.getRole();
        List<GrantedAuthority> authorities = (role != null && role.isActive())
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role.getRole().trim().toUpperCase(Locale.ROOT)))
                : List.of();

        return new UserPrincipal(user, authorities);
    }
}
