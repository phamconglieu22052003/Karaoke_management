package com.karaoke_management.security;

import com.karaoke_management.entity.Permission;
import com.karaoke_management.entity.Role;
import com.karaoke_management.entity.User;
import com.karaoke_management.repository.UserRepository;
import java.util.HashSet;
import java.util.Set;
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
            throw new UsernameNotFoundException("User is inactive");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();

        for (Role r : user.getRoles()) {
            // ROLE_* for hasRole(...) / hasAnyRole(...)
            String code = r.getRoleCode();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + code));

            // ===== Role normalization for demo actors (Admin / POS / Lễ tân) =====
            // - DB có thể đang dùng role cũ (MANAGER/CASHIER/STAFF/WAREHOUSE/TECH)
            // - Tại tầng Security, map thêm authority để dùng thống nhất: ADMIN / POS / RECEPTION
            if (code != null) {
                switch (code) {
                    case "ADMIN" -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    case "MANAGER" -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    case "CASHIER" -> authorities.add(new SimpleGrantedAuthority("ROLE_POS"));
                    case "POS" -> authorities.add(new SimpleGrantedAuthority("ROLE_POS"));
                    case "STAFF" -> authorities.add(new SimpleGrantedAuthority("ROLE_RECEPTION"));
                    case "RECEPTION" -> authorities.add(new SimpleGrantedAuthority("ROLE_RECEPTION"));
                    // các role khác (nếu có) coi như admin để demo nhanh
                    case "WAREHOUSE", "TECH" -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
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

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
