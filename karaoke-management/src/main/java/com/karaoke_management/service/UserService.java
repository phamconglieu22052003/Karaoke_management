package com.karaoke_management.service;

import com.karaoke_management.entity.User;
import java.util.List;

public interface UserService {

    List<User> findAll();

    User getRequired(Long id);

    User createUser(String username,
                    String rawPassword,
                    String fullName,
                    String phone,
                    String roleCode);

    User updateUser(Long id,
                    String fullName,
                    String phone,
                    String roleCode);

    void toggleStatus(Long id);

    void resetPassword(Long id, String rawPassword);
}
