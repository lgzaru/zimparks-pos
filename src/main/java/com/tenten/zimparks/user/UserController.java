package com.tenten.zimparks.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Manage POS system users.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService service;

    @GetMapping
    @Operation(summary = "List all users, optionally filtered by station or role.")
    public ResponseEntity<Page<User>> list(
            @Parameter(description = "Station ID to filter users by.") @RequestParam(required = false) String stationId,
            @Parameter(description = "Role to filter users by.") @RequestParam(required = false) Role role,
            @PageableDefault(size = 30) Pageable pageable) {
        if (stationId != null && role != null) {
            return ResponseEntity.ok(service.findByStationAndRole(stationId, role, pageable));
        } else if (role != null) {
            return ResponseEntity.ok(service.findByRole(role, pageable));
        }
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID.")
    public ResponseEntity<User> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/by-cell/{cellPhone}")
    @Operation(summary = "Get user details by cell phone number.", security = {})
    public ResponseEntity<User> getUserDetailsByCell(@PathVariable String cellPhone) {
        return ResponseEntity.ok(service.findByCellPhone(cellPhone));
    }

    @GetMapping("/permissions")
    @Operation(summary = "List all available permissions.")
    public Permission[] listPermissions() {
        return Permission.values();
    }

    @PostMapping
    @Operation(summary = "Create a new user.")
    public ResponseEntity<User> create(@RequestBody User u) {
        return ResponseEntity.ok(service.create(u));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user.")
    public ResponseEntity<User> update(@PathVariable UUID id, @RequestBody User u) {
        return ResponseEntity.ok(service.update(id, u));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update an existing user.")
    public ResponseEntity<User> patch(@PathVariable UUID id, @RequestBody User u) {
        return ResponseEntity.ok(service.update(id, u));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a user.")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
