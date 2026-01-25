package com.karaoke_management.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // static
                .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                .requestMatchers("/login").permitAll()

                // ✅ VNPay endpoints: MUST be public (VNPay server cannot login)
                // IMPORTANT: sửa path cho đúng với controller VNPay của bạn nếu khác
                .requestMatchers(
                    "/payment/vnpay/ipn",
                    "/payment/vnpay/return",
                    "/payment/vnpay/**"   // nếu bạn có endpoint tạo payment / redirect VNPay
                ).permitAll()

                // pages
                .requestMatchers("/", "/dashboard").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/rooms/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/booking/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/bookings/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/invoice/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/room-types/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/room-sessions/**").hasAnyRole("ADMIN", "STAFF")

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
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    // Tài khoản cố định (demo đồ án)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(admin);
    }
}
