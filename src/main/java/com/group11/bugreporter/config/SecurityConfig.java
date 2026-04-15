package com.group11.bugreporter.config;

import com.group11.bugreporter.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configurare principala pentru securitatea aplicatiei.
 *
 * <p>Aceasta clasa defineste:
 * - regulile de autorizare pentru endpoint-uri;
 * - strategia stateless bazata pe JWT;
 * - integrarea filtrului personalizat de autentificare JWT;
 * - configurarea CORS pentru frontend-ul local;
 * - encoderul de parole folosit la inregistrare/autentificare.</p>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Filtru care extrage token-ul JWT din headerul Authorization
     * si seteaza utilizatorul autentificat in SecurityContext.
     */
    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * Construieste lantul principal de filtre Spring Security.
     *
     * <p>Configurari aplicate:
     * - activeaza CORS cu sursa definita in {@link #corsConfigurationSource()};
     * - dezactiveaza CSRF (potrivit pentru API stateless);
     * - seteaza sesiunea ca STATELESS (fara sesiuni server-side);
     * - permite acces public la autentificare si citirea comentariilor pe bug;
     * - cere autentificare pentru orice alt endpoint;
     * - adauga filtrul JWT inainte de filtrul standard UsernamePasswordAuthenticationFilter.</p>
     *
     * @param http builder-ul de configurare HttpSecurity
     * @return lantul de filtre configurat
     * @throws Exception daca apare o eroare la construirea configurarii de securitate
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .csrf(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/comments/bug/**").permitAll() // Anyone can read comments
                        .requestMatchers("/api/users").permitAll()
                        .requestMatchers("/api/bugs/**").permitAll()
                        .requestMatchers("/api/").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("text/plain;charset=UTF-8");
                            response.getWriter().write("Access denied: you do not have permission to perform this action");
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Defineste encoderul de parole folosit in aplicatie.
     *
     * @return implementare BCrypt pentru hash-uirea parolelor
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defineste configuratia CORS globala pentru API.
     *
     * <p>Permite request-uri doar din frontend-ul local Vite, cu metode HTTP uzuale
     * si headerele necesare pentru autentificare JWT.</p>
     *
     * @return sursa de configurare CORS inregistrata pe toate rutele ("/**")
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }


}
