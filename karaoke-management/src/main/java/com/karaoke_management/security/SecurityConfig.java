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
    private final CustomAuthFailureHandler authFailureHandler;

    public SecurityConfig(UserDetailsService userDetailsService, CustomAuthFailureHandler authFailureHandler) {
        this.userDetailsService = userDetailsService;
        this.authFailureHandler = authFailureHandler;
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

                        // Public (Khách hàng)
                        .requestMatchers("/customer/**").permitAll()

                        // QR image endpoint can be used in public pages (payment/booking link)
                        .requestMatchers("/qr").permitAll()

                        // VNPay endpoints MUST be public
                        .requestMatchers(
                                "/payment/vnpay/ipn",
                                "/payment/vnpay/return",
                                "/payment/vnpay/**",
                                "/payment/vnpay/mock/**"
                        ).permitAll()

                        // ===== Phân quyền thật theo actor tài liệu =====
                        // Quản lý        -> MANAGER
                        // Thu ngân       -> CASHIER
                        // Nhân viên kho  -> STOREKEEPER
                        // Nhân viên phục vụ -> WAITER
                        // Kỹ thuật       -> TECHNICIAN

                        // Dashboard: tất cả nhân sự nội bộ xem được sau khi login
                        .requestMatchers("/", "/dashboard")
                        .hasAnyRole("MANAGER", "CASHIER", "STOREKEEPER", "WAITER", "TECHNICIAN")

                        // Rooms: phục vụ/thu ngân cần xem để thao tác; quản lý quản trị CRUD
                        .requestMatchers(HttpMethod.GET, "/rooms", "/rooms/")
                        .hasAnyRole("MANAGER", "CASHIER", "WAITER")
                        .requestMatchers(HttpMethod.GET, "/rooms/new", "/rooms/edit/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.POST, "/rooms", "/rooms/update", "/rooms/delete/**").hasRole("MANAGER")
                        // fallback: mọi endpoint khác dưới /rooms/** chỉ cho quản lý
                        .requestMatchers("/rooms/**").hasRole("MANAGER")

                        // Room types: quản lý
                        .requestMatchers("/room-types/**").hasRole("MANAGER")

                        // Session: thu ngân/quản lý mở-đóng; phục vụ xem lịch sử
                        .requestMatchers(HttpMethod.GET, "/room-sessions/**")
                        .hasAnyRole("MANAGER", "CASHIER", "WAITER")
                        .requestMatchers(HttpMethod.POST, "/room-sessions/**")
                        .hasAnyRole("MANAGER", "CASHIER")

                        // POS order: phục vụ + thu ngân + quản lý
                        .requestMatchers("/pos/**").hasAnyRole("MANAGER", "CASHIER", "WAITER")

                        // Booking: thu ngân + quản lý
                        .requestMatchers("/booking/**", "/bookings/**").hasAnyRole("MANAGER", "CASHIER")

                        // Invoice: thu ngân + quản lý
                        .requestMatchers("/invoice/**").hasAnyRole("MANAGER", "CASHIER")

                        // Shift: thu ngân + quản lý
                        .requestMatchers("/shift/**").hasAnyRole("MANAGER", "CASHIER")

                        // Technician module
                        .requestMatchers("/tech/**").hasAnyRole("MANAGER", "TECHNICIAN")

                        // Inventory: nhân viên kho + quản lý
                        // Nghiệp vụ: QUẢN LÝ duyệt/từ chối phiếu; nhân viên kho chỉ tạo/sửa/gửi phiếu
                        .requestMatchers(HttpMethod.POST,
                                "/inventory/receipts/*/approve",
                                "/inventory/receipts/*/reject")
                        .hasRole("MANAGER")

                        // Nhân viên kho chỉ được TẠO/SỬA/GỬI DUYỆT khi phiếu còn DRAFT
                        // (Ràng buộc trạng thái DRAFT được kiểm soát ở service/controller)
                        // Admin = Manager: quản lý phải có full quyền, bao gồm tạo/sửa phiếu kho.
                        .requestMatchers(HttpMethod.GET,
                                "/inventory/receipts/new",
                                "/inventory/receipts/*/edit")
                        .hasAnyRole("MANAGER", "STOREKEEPER")
                        .requestMatchers(HttpMethod.POST,
                                "/inventory/receipts",
                                "/inventory/receipts/*")
                        .hasAnyRole("MANAGER", "STOREKEEPER")

                        .requestMatchers("/inventory/**").hasAnyRole("MANAGER", "STOREKEEPER")

                        // Product + category: quản lý (và nhân viên kho được xem danh sách)
                        .requestMatchers(HttpMethod.GET,
                                "/products", "/products/", "/product-categories", "/product-categories/")
                        .hasAnyRole("MANAGER", "STOREKEEPER")
                        .requestMatchers("/products/**", "/product-categories/**").hasRole("MANAGER")

                        // Users: quản lý
                        .requestMatchers("/users/**").hasRole("MANAGER")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureHandler(authFailureHandler)
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
