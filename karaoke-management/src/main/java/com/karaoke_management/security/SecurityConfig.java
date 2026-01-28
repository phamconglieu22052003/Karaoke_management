package com.karaoke_management.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

                        // Dashboard (all staff roles)
                        .requestMatchers("/", "/dashboard").hasAnyRole("ADMIN", "MANAGER", "CASHIER", "STAFF", "WAREHOUSE", "TECH")

                        // Rooms & sessions (operation)
                        .requestMatchers("/rooms/**", "/room-sessions/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER", "STAFF")

                        // Booking (operation)
                        .requestMatchers("/booking/**", "/bookings/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER", "STAFF")

                        // Invoice/payment (cashier-focused)
                        .requestMatchers("/invoice/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER")

                        // Room type (manager/admin)
                        .requestMatchers("/room-types/**").hasAnyRole("ADMIN", "MANAGER")

                        // QR demo
                        .requestMatchers("/qr").permitAll()

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
