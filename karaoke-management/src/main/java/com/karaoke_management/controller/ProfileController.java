package com.karaoke_management.controller;

import com.karaoke_management.dto.ChangePasswordDTO;
import com.karaoke_management.service.UserService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/password")
    public String changePasswordForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ChangePasswordDTO());
        }
        return "profile/change-password";
    }

    @PostMapping("/password")
    public String changePasswordSubmit(
            Principal principal,
            @ModelAttribute("form") ChangePasswordDTO form,
            RedirectAttributes ra) {

        try {
            if (form.getNewPassword() == null || form.getConfirmPassword() == null
                    || !form.getNewPassword().equals(form.getConfirmPassword())) {
                throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp.");
            }

            userService.changePassword(principal != null ? principal.getName() : null,
                    form.getOldPassword(),
                    form.getNewPassword());

            ra.addFlashAttribute("success", "Đổi mật khẩu thành công.");
            return "redirect:/profile/password";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage() != null ? ex.getMessage() : "Không thể đổi mật khẩu.");
            ra.addFlashAttribute("form", form);
            return "redirect:/profile/password";
        }
    }
}
