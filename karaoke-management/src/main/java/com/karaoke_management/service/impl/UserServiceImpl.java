package com.karaoke_management.service.impl;

import com.karaoke_management.entity.Role;
import com.karaoke_management.entity.User;
import com.karaoke_management.repository.RoleRepository;
import com.karaoke_management.repository.UserRepository;
import com.karaoke_management.service.UserService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User getRequired(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user ID=" + id));
    }

    @Override
    public User createUser(String username,
                           String rawPassword,
                           String fullName,
                           String phone,
                           String roleCode) {

        if (username == null || username.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username không được để trống");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải >= 6 ký tự");
        }
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username đã tồn tại");
        }

        User u = new User();
        u.setUsername(username.trim());
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setFullName(fullName);
        u.setPhone(phone);
        u.setStatus((byte) 1);

        u.setRoles(resolveRoles(roleCode));
        return userRepository.save(u);
    }

    @Override
    public User updateUser(Long id,
                           String fullName,
                           String phone,
                           String roleCode) {
        User u = getRequired(id);
        u.setFullName(fullName);
        u.setPhone(phone);
        u.setRoles(resolveRoles(roleCode));
        return userRepository.save(u);
    }

    @Override
    public void toggleStatus(Long id) {
        User u = getRequired(id);
        u.setStatus(u.isActive() ? (byte) 0 : (byte) 1);
        userRepository.save(u);
    }

    @Override
    public void resetPassword(Long id, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải >= 6 ký tự");
        }
        User u = getRequired(id);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepository.save(u);
    }

    private Set<Role> resolveRoles(String roleCode) {
        Set<Role> roles = new LinkedHashSet<>();
        if (roleCode == null || roleCode.trim().isEmpty()) {
            // Default minimal role if not provided
            Role staff = roleRepository.findByRoleCode("STAFF")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu role STAFF trong bảng roles"));
            roles.add(staff);
            return roles;
        }

        Role r = roleRepository.findByRoleCode(roleCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy role: " + roleCode));
        roles.add(r);
        return roles;
    }
}
