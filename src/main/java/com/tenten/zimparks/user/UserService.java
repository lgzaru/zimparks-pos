package com.tenten.zimparks.user;

import com.tenten.zimparks.station.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


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
        log.debug("User details loaded username={} active={} role={}", u.getUsername(), u.isActive(), u.getRole());
        return new org.springframework.security.core.userdetails.User(
                u.getUsername(), u.getPassword(),
                u.isActive(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))
        );
    }

    public List<User> findAll()           { return repo.findAllByActiveTrue(); }

    public User findById(UUID id) {
        return repo.findByIdAndActiveTrue(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User create(User u) {
        if (repo.existsByUsername(u.getUsername()))
            throw new IllegalArgumentException("Username already exists");
        u.setPassword(encoder.encode(u.getPassword()));
        if (u.getStation() != null && u.getStation().getId() != null) {
            u.setStation(stationRepo.findById(u.getStation().getId())
                    .orElseThrow(() -> new RuntimeException("Station not found")));
        }
        return repo.save(u);
    }

    public User update(UUID id, User patch) {
        User u = repo.findByIdAndActiveTrue(id).orElseThrow(() -> new RuntimeException("User not found"));
        u.setFullName(patch.getFullName());
        u.setUsername(patch.getUsername());
        u.setRole(patch.getRole());
        u.setActive(patch.isActive());

        if (patch.getStation() != null && patch.getStation().getId() != null) {
            u.setStation(stationRepo.findById(patch.getStation().getId())
                    .orElseThrow(() -> new RuntimeException("Station not found")));
        } else {
            u.setStation(null);
        }

        if (patch.getPassword() != null && !patch.getPassword().isBlank())
            u.setPassword(encoder.encode(patch.getPassword()));
        return repo.save(u);
    }

    public void delete(UUID id) {
        User u = repo.findByIdAndActiveTrue(id).orElseThrow(() -> new RuntimeException("User not found"));
        u.setActive(false);
        repo.save(u);
    }
}
