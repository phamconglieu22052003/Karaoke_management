package com.karaoke_management.controller;

import com.karaoke_management.entity.Role;
import com.karaoke_management.entity.User;
import com.karaoke_management.repository.RoleRepository;
import com.karaoke_management.service.UserService;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    public UserController(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public String list(Model model) {
        List<User> users = userService.findAll().stream()
                .sorted(Comparator.comparing(User::getId, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        model.addAttribute("users", users);
        return "users/user-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new UserForm());
        model.addAttribute("roles", getRoleOptions());
        // Avoid null in Thymeleaf boolean expressions
        model.addAttribute("editMode", false);
        return "users/user-form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("form") UserForm form,
                         RedirectAttributes ra,
                         Model model) {
        try {
            userService.createUser(form.getUsername(), form.getPassword(), form.getFullName(), form.getPhone(), form.getRoleCode());
            ra.addFlashAttribute("success", "Đã tạo user: " + form.getUsername());
            return "redirect:/users";
        } catch (Exception ex) {
            model.addAttribute("roles", getRoleOptions());
            model.addAttribute("editMode", false);
            model.addAttribute("error", ex.getMessage());
            return "users/user-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User u = userService.getRequired(id);
        UserForm form = new UserForm();
        form.setId(u.getId());
        form.setUsername(u.getUsername());
        form.setFullName(u.getFullName());
        form.setPhone(u.getPhone());
        form.setRoleCode(u.getRoles().stream().findFirst().map(Role::getRoleCode).orElse(""));

        model.addAttribute("form", form);
        model.addAttribute("roles", getRoleOptions());
        model.addAttribute("editMode", true);
        return "users/user-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @ModelAttribute("form") UserForm form,
                       RedirectAttributes ra,
                       Model model) {
        try {
            userService.updateUser(id, form.getFullName(), form.getPhone(), form.getRoleCode());
            ra.addFlashAttribute("success", "Đã cập nhật user: " + form.getUsername());
            return "redirect:/users";
        } catch (Exception ex) {
            model.addAttribute("roles", getRoleOptions());
            model.addAttribute("editMode", true);
            model.addAttribute("error", ex.getMessage());
            return "users/user-form";
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        userService.toggleStatus(id);
        ra.addFlashAttribute("success", "Đã đổi trạng thái user ID=" + id);
        return "redirect:/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam("newPassword") String newPassword,
                                RedirectAttributes ra) {
        userService.resetPassword(id, newPassword);
        ra.addFlashAttribute("success", "Đã reset mật khẩu user ID=" + id);
        return "redirect:/users";
    }

    private List<Role> getRoleOptions() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getRoleCode, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    public static class UserForm {
        private Long id;
        private String username;
        private String password;

        private String fullName;
        private String phone;
        private String roleCode;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    }
}
