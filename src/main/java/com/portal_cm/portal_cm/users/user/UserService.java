package com.portal_cm.portal_cm.users.user;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.portal_cm.portal_cm.users.role.RoleRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String ENTITY_TIPE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

}
