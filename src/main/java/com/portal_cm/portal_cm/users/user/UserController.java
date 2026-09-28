package com.portal_cm.portal_cm.users.user;


import com.portal_cm.portal_cm.users.user.dto.UserRequest;
import com.portal_cm.portal_cm.users.user.dto.UserResponse;
import com.portal_cm.portal_cm.users.user.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.portal_cm.portal_cm.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Integer id) {
        return userService.findById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> update(@PathVariable Integer id, @Valid @RequestBody UserUpdateRequest request,
                                               @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(userService.update(id, request, currentUser.getId()));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivate(@PathVariable Integer id, @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(userService.deactivate(id, currentUser.getId()));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<UserResponse> reactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.reactivate(id));
    }
}
