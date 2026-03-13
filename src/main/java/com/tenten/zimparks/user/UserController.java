package com.tenten.zimparks.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Manage POS system users.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService service;

    @GetMapping
    @Operation(summary = "List all users.")
    public List<User> list() { return service.findAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID.")
    public ResponseEntity<User> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a user.")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
