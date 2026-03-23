package com.tenten.zimparks.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtConfig jwtConfig;
    private final UserDetailsService userDetailsService;
    private final com.tenten.zimparks.user.UserRepository userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(req);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = jwtConfig.extractUsername(token);
            if (username != null) {
                UserDetails ud = userDetailsService.loadUserByUsername(username);

                if (jwtConfig.validateToken(token, ud)) {
                    var userOpt = userRepo.findByUsername(username);
                    if (userOpt.isPresent()) {
                        String storedToken = userOpt.get().getCurrentToken();
                        if (token.equals(storedToken)) {
                            var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            log.debug("JWT auth set for user={}", username);
                        } else {
                            log.debug("Token mismatch for user={} — presented token does not match stored currentToken. " +
                                    "storedToken isNull={}", username, storedToken == null);
                        }
                    } else {
                        log.debug("User not found in repository: {}", username);
                    }
                } else {
                    log.debug("Token validation failed for user={}", username);
                }
            } else {
                log.debug("Could not extract username from token");
            }
        }

        chain.doFilter(req, res);
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
