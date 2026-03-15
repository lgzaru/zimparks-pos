package com.tenten.zimparks.user;

import com.tenten.zimparks.station.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository repo;
    private final StationRepository stationRepo;
    private final PasswordEncoder encoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username={}", username);
        User u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        log.debug("User details loaded username={} active={} role={}", u.getUsername(), u.getActive(), u.getRole());

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
        
        // From Role
        authorities.addAll(u.getRole().getPermissions().stream()
                .map(p -> new SimpleGrantedAuthority(p.getPermission()))
                .collect(Collectors.toList()));
        
        // From User (Individual permissions)
        if (u.getPermissions() != null) {
            authorities.addAll(u.getPermissions().stream()
                    .map(p -> new SimpleGrantedAuthority(p.getPermission()))
                    .collect(Collectors.toList()));
        }

        return new org.springframework.security.core.userdetails.User(
                u.getUsername(), u.getPassword(),
                u.getActive() != null && u.getActive(), true, true, true,
                authorities
        );
    }

    public List<User> findAll()           { return repo.findAllByActiveTrue(); }

    public User findById(UUID id) {
        return repo.findByIdAndActiveTrue(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findByCellPhone(String cellPhone) {
        if (cellPhone != null) {
            cellPhone = cellPhone.replaceAll("[\\+\\s]", "");
        }
        return repo.findByCellPhone(cellPhone)
                .orElseThrow(() -> new IllegalArgumentException("the provided number is not linked to any user"));
    }

    public User create(User u) {
        if (u.getUsername() != null) u.setUsername(u.getUsername().toUpperCase());
        if (u.getPassword() != null) u.setPassword(u.getPassword().toUpperCase());

        if (repo.existsByUsername(u.getUsername()))
            throw new IllegalArgumentException("Username already exists");
        u.setPassword(encoder.encode(u.getPassword()));
        if (u.getStation() != null && u.getStation().getId() != null) {
            u.setStation(stationRepo.findById(u.getStation().getId())
                    .orElseThrow(() -> new RuntimeException("Station not found")));
        }
        if (u.getPermissions() == null) {
            u.setPermissions(new java.util.HashSet<>());
        }
        return repo.save(u);
    }

    public User update(UUID id, User patch) {
        User u = repo.findByIdAndActiveTrue(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (patch.getFullName() != null) u.setFullName(patch.getFullName());
        if (patch.getUsername() != null) u.setUsername(patch.getUsername().toUpperCase());
        if (patch.getRole() != null) u.setRole(patch.getRole());
        u.setActive(patch.getActive() != null ? patch.getActive() : u.getActive());
        if (patch.getPermissions() != null) u.setPermissions(patch.getPermissions());

        if (patch.getStation() != null && patch.getStation().getId() != null) {
            u.setStation(stationRepo.findById(patch.getStation().getId())
                    .orElseThrow(() -> new RuntimeException("Station not found")));
        }

        if (patch.getPassword() != null && !patch.getPassword().isBlank())
            u.setPassword(encoder.encode(patch.getPassword().toUpperCase()));
        return repo.save(u);
    }

    public void delete(UUID id) {
        User u = repo.findByIdAndActiveTrue(id).orElseThrow(() -> new RuntimeException("User not found"));
        u.setActive(false);
        repo.save(u);
    }
}
