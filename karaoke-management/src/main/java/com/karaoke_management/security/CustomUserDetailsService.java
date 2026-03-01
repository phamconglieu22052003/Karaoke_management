package com.karaoke_management.security;

import com.karaoke_management.entity.Permission;
import com.karaoke_management.entity.Role;
import com.karaoke_management.entity.User;
import com.karaoke_management.repository.UserRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isActive()) {
            // UC-AUTH-01 (E2): tài khoản bị khóa/vô hiệu hóa -> từ chối đăng nhập
            throw new DisabledException("inactive");
        }

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            // UC-AUTH-01 (E2): không có quyền/role -> từ chối đăng nhập
            throw new DisabledException("noperm");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();

        for (Role r : user.getRoles()) {
            // ROLE_* for hasRole(...) / hasAnyRole(...)
            String code = r.getRoleCode();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + code));

            // ===== Role normalization (để tương thích dữ liệu cũ) =====
            // Mục tiêu: phân quyền thật theo actor tài liệu
            // - MANAGER (Quản lý)
            // - CASHIER (Thu ngân)
            // - STOREKEEPER (Nhân viên kho)
            // - WAITER (Nhân viên phục vụ)
            // - TECHNICIAN (Kỹ thuật)
            // Nếu DB còn role cũ (ADMIN/POS/WAREHOUSE/STAFF/TECH) thì map sang role mới.
            if (code != null) {
                switch (code) {
                    case "ADMIN", "MANAGER" -> authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
                    case "POS", "CASHIER" -> authorities.add(new SimpleGrantedAuthority("ROLE_CASHIER"));
                    case "WAREHOUSE", "STOREKEEPER" -> authorities.add(new SimpleGrantedAuthority("ROLE_STOREKEEPER"));
                    case "STAFF", "WAITER" -> authorities.add(new SimpleGrantedAuthority("ROLE_WAITER"));
                    case "TECH", "TECHNICIAN" -> authorities.add(new SimpleGrantedAuthority("ROLE_TECHNICIAN"));
                    default -> {
                        // no-op
                    }
                }
            }
            // Permissions (optional)
            for (Permission p : r.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(p.getPermCode()));
            }
        }

        if (authorities.isEmpty()) {
            // Trường hợp role tồn tại nhưng không sinh ra authority nào (DB lỗi)
            throw new DisabledException("noperm");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
