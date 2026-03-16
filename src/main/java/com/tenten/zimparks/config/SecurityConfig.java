package com.tenten.zimparks.config;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final com.tenten.zimparks.activity.ActivityLogFilter activityLogFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow CORS preflight through first
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/by-cell/**").permitAll()
                        .requestMatchers("/api/events/stream/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Admin only
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/stations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasAuthority("product:update-pricing")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/stations/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/stations/*/banks/*").hasAuthority("station:link-banks")
                        .requestMatchers(HttpMethod.DELETE, "/api/stations/*/banks/*").hasAuthority("station:unlink-banks")
                        .requestMatchers(HttpMethod.POST, "/api/stations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/stations/**").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/stations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAuthority("product:add-product")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAuthority("product:update-pricing")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/product-categories/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/product-categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/product-categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/product-categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/product-categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/vat/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/vat/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/vat/**").hasRole("ADMIN")
                        // Supervisor+
                        .requestMatchers(HttpMethod.POST, "/api/shifts/close/**").hasAuthority("shift:close")
                        .requestMatchers("/api/shifts/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR")
                        .requestMatchers("/api/credit-notes/**").hasAnyRole("ADMIN", "SUPERVISOR", "OPERATOR")
                        .requestMatchers("/api/credit-notes/*/approve", "/api/credit-notes/*/reject").hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers("/api/reports/**").hasAnyRole("SUPERVISOR", "ADMIN")
                        // Transactions
                        .requestMatchers("/api/transactions/*/void/**").hasAnyRole("SUPERVISOR", "ADMIN", "OPERATOR")
                        // Authenticated
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(activityLogFilter, JwtFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }
}
