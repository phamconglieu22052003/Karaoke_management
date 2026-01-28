package com.karaoke_management.config;

import com.karaoke_management.entity.Permission;
import com.karaoke_management.entity.Role;
import com.karaoke_management.entity.User;
import com.karaoke_management.repository.PermissionRepository;
import com.karaoke_management.repository.RoleRepository;
import com.karaoke_management.repository.UserRepository;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedConfig {

    /**
     * Seed roles/permissions/users for demo.
     * Password default: 123456
     */
    @Bean
    CommandLineRunner seedAuthData(
            PermissionRepository permRepo,
            RoleRepository roleRepo,
            UserRepository userRepo,
            PasswordEncoder encoder
    ) {
        return args -> {
            // --- Permissions (expand later) ---
            Permission roomOpen = upsertPermission(permRepo, "ROOM_OPEN", "Mở phòng", "ROOM");
            Permission roomClose = upsertPermission(permRepo, "ROOM_CLOSE", "Đóng phòng", "ROOM");
            Permission paymentCreate = upsertPermission(permRepo, "PAYMENT_CREATE", "Tạo thanh toán", "INVOICE");
            Permission discountApply = upsertPermission(permRepo, "DISCOUNT_APPLY", "Áp dụng giảm giá", "INVOICE");
            Permission bookingManage = upsertPermission(permRepo, "BOOKING_MANAGE", "Quản lý đặt phòng", "BOOKING");
            Permission roomTypeManage = upsertPermission(permRepo, "ROOMTYPE_MANAGE", "Quản lý loại phòng", "ROOM");
            Permission reportView = upsertPermission(permRepo, "REPORT_VIEW", "Xem báo cáo", "REPORT");

            // --- Roles ---
            Role admin = upsertRole(roleRepo, "ADMIN", "Quản trị", Set.of(
                    roomOpen, roomClose, paymentCreate, discountApply, bookingManage, roomTypeManage, reportView
            ));

            Role manager = upsertRole(roleRepo, "MANAGER", "Quản lý", Set.of(
                    roomOpen, roomClose, paymentCreate, discountApply, bookingManage, roomTypeManage, reportView
            ));

            Role cashier = upsertRole(roleRepo, "CASHIER", "Thu ngân", Set.of(
                    roomOpen, roomClose, paymentCreate, discountApply, bookingManage
            ));

            Role staff = upsertRole(roleRepo, "STAFF", "Nhân viên", Set.of(
                    roomOpen, roomClose, bookingManage
            ));

            Role warehouse = upsertRole(roleRepo, "WAREHOUSE", "Kho", Set.of(reportView));
            Role tech = upsertRole(roleRepo, "TECH", "Kỹ thuật", Set.of(reportView));

            // --- Users ---
            upsertUser(userRepo, encoder, "admin", "Admin", "", admin);
            upsertUser(userRepo, encoder, "manager", "Manager", "", manager);
            upsertUser(userRepo, encoder, "cashier", "Cashier", "", cashier);
            upsertUser(userRepo, encoder, "staff", "Staff", "", staff);
            upsertUser(userRepo, encoder, "warehouse", "Warehouse", "", warehouse);
            upsertUser(userRepo, encoder, "tech", "Tech", "", tech);
        };
    }

    private Permission upsertPermission(PermissionRepository repo, String code, String name, String module) {
        return repo.findByPermCode(code).map(existing -> {
            existing.setPermName(name);
            existing.setModule(module);
            return repo.save(existing);
        }).orElseGet(() -> {
            Permission p = new Permission();
            p.setPermCode(code);
            p.setPermName(name);
            p.setModule(module);
            return repo.save(p);
        });
    }

    private Role upsertRole(RoleRepository repo, String roleCode, String roleName, Set<Permission> permissions) {
        return repo.findByRoleCode(roleCode).map(existing -> {
            existing.setRoleName(roleName);
            existing.setPermissions(permissions);
            return repo.save(existing);
        }).orElseGet(() -> {
            Role r = new Role();
            r.setRoleCode(roleCode);
            r.setRoleName(roleName);
            r.setPermissions(permissions);
            return repo.save(r);
        });
    }

    private void upsertUser(UserRepository userRepo, PasswordEncoder encoder, String username, String fullName, String phone, Role role) {
        userRepo.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setFullName(fullName);
            u.setPhone(phone);
            u.setPasswordHash(encoder.encode("123456"));
            u.setStatus((byte) 1);
            u.setRoles(Set.of(role));
            return userRepo.save(u);
        });
    }
}
