package com.ashutosh.cyberhelper.web;

import com.ashutosh.cyberhelper.dto.user.AssignRolesRequest;
import com.ashutosh.cyberhelper.dto.user.UserResponse;
import com.ashutosh.cyberhelper.dto.user.UserUpdateRequest;
import com.ashutosh.cyberhelper.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(userService.getUserById(id, authentication));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers(Authentication authentication) {
        return ResponseEntity.ok(userService.getAllUsers(authentication));
    }

    @GetMapping("/organization/{orgId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsersByOrganization(@PathVariable Long orgId, Authentication authentication) {
        return ResponseEntity.ok(userService.getUsersByOrganization(orgId, authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateUser(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        userService.deleteUser(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> assignRolesToUser(
            @PathVariable Long id,
            @RequestBody AssignRolesRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.assignRolesToUser(id, request.roleIds(), authentication));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> searchUsersByEmail(
            @RequestParam String email,
            Authentication authentication) {
        return ResponseEntity.ok(userService.searchUsersByEmail(email, authentication));
    }
}

