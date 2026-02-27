package com.karaoke_management.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Spring Security 7+ (Spring Boot 4) removed the no-arg constructor.
        // Use the UserDetailsService constructor instead.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // static
                        .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/login").permitAll()

                        // VNPay endpoints MUST be public
                        .requestMatchers(
                                "/payment/vnpay/ipn",
                                "/payment/vnpay/return",
                                "/payment/vnpay/**",
                                "/payment/vnpay/mock/**"
                        ).permitAll()

                        // ===== Chuẩn hoá theo actor trong tài liệu: Admin / POS / Lễ tân =====
                        // Dashboard: ai cũng xem được sau khi login
                        .requestMatchers("/", "/dashboard").hasAnyRole("ADMIN", "POS", "RECEPTION")

                        // Rooms: POS cần xem để mở/đóng phòng; Admin quản trị CRUD
                        .requestMatchers(HttpMethod.GET, "/rooms", "/rooms/").hasAnyRole("ADMIN", "POS")
                        .requestMatchers(HttpMethod.GET, "/rooms/new", "/rooms/edit/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/rooms", "/rooms/update", "/rooms/delete/**").hasRole("ADMIN")
                        // fallback: mọi endpoint khác dưới /rooms/** chỉ cho Admin
                        .requestMatchers("/rooms/**").hasRole("ADMIN")

                        // Room types: Admin quản trị
                        .requestMatchers("/room-types/**").hasRole("ADMIN")

                        // Session open/close + lịch sử: POS
                        .requestMatchers("/room-sessions/**").hasAnyRole("ADMIN", "POS")

                        // POS order
                        .requestMatchers("/pos/**").hasAnyRole("ADMIN", "POS")

                        // Booking: Lễ tân + Admin
                        .requestMatchers("/booking/**", "/bookings/**").hasAnyRole("ADMIN", "RECEPTION")

                        // Invoice: POS + Admin
                        .requestMatchers("/invoice/**").hasAnyRole("ADMIN", "POS")

                        // Shift: POS + Admin
                        .requestMatchers("/shift/**").hasAnyRole("ADMIN", "POS")

                        // Product + category + inventory + users: Admin
                        .requestMatchers("/products/**", "/product-categories/**").hasRole("ADMIN")
                        .requestMatchers("/inventory/**").hasRole("ADMIN")
                        .requestMatchers("/users/**").hasRole("ADMIN")

                        // QR generate: nội bộ (POS/Admin)
                        .requestMatchers("/qr").hasAnyRole("ADMIN", "POS")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll()
                )
                .authenticationProvider(authenticationProvider())
                .build();
    }
}
