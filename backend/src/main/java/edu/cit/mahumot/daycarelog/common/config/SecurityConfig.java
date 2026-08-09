package edu.cit.mahumot.daycarelog.common.config;

import edu.cit.mahumot.daycarelog.common.security.EmailVerificationFilter;
import edu.cit.mahumot.daycarelog.common.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final EmailVerificationFilter emailVerificationFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, EmailVerificationFilter emailVerificationFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.emailVerificationFilter = emailVerificationFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET,    "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/users/*/role").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/users/*/deactivate").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/users/*/reactivate").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/users/*/reset-password").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/users/*").authenticated()

                .requestMatchers(HttpMethod.GET, "/api/children/mine").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/attendance/mine").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/health-records/mine").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/immunizations/mine").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/immunizations/schedule").authenticated()

                // Same pattern as the /mine endpoints above: broad "authenticated" here,
                // fine-grained PARENT-vs-own-child scoping done in ActivityLogController
                // (reuses GuardianService.isGuardianOfChild).
                .requestMatchers(HttpMethod.GET, "/api/children/*/history").authenticated()

                .requestMatchers(HttpMethod.GET, "/api/activity-logs/recent").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/activity-logs").hasRole("ADMIN")

                // Recycle Bin: admin-only, and must be declared before the broader ADMIN+STAFF
                // rules below -- /permanent in particular would otherwise be caught by the
                // wildcard DELETE "/api/health-records/**" / "/api/immunizations/**" rules,
                // since Spring Security's authorizeHttpRequests uses first-match-wins ordering.
                .requestMatchers(HttpMethod.GET,    "/api/health-records/trash").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/health-records/*/restore").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/health-records/*/permanent").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/immunizations/trash").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/immunizations/*/restore").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/immunizations/*/permanent").hasRole("ADMIN")

                .requestMatchers("/api/children/*/guardians", "/api/children/*/guardians/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/guardians", "/api/guardians/**").hasAnyRole("ADMIN", "STAFF")

                .requestMatchers(HttpMethod.POST,   "/api/children", "/api/children/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PUT,    "/api/children/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/children/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.GET,    "/api/children", "/api/children/*").hasAnyRole("ADMIN", "STAFF")

                .requestMatchers(HttpMethod.POST, "/api/attendance/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.GET,  "/api/attendance", "/api/attendance/range", "/api/attendance/child/**").hasAnyRole("ADMIN", "STAFF")

                .requestMatchers(HttpMethod.POST,   "/api/health-records", "/api/health-records/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/health-records/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.GET,    "/api/health-records", "/api/health-records/child/**").hasAnyRole("ADMIN", "STAFF")

                .requestMatchers(HttpMethod.POST,   "/api/immunizations", "/api/immunizations/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/immunizations/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.GET,    "/api/immunizations", "/api/immunizations/child/**").hasAnyRole("ADMIN", "STAFF")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(emailVerificationFilter, JwtAuthFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
