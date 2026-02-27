package com.karaoke_management.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Chuẩn hoá lỗi đăng nhập theo UC-AUTH-01 (E2: tài khoản bị khóa/không có quyền).
 *
 * Trả về:
 *  - /login?error=inactive  : tài khoản bị vô hiệu hoá
 *  - /login?error=noperm    : không có quyền/role
 *  - /login?error=true      : sai username/password hoặc lỗi khác
 */
@Component
public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String redirect = "/login?error=true";

        if (exception instanceof DisabledException) {
            String msg = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
            if (msg.contains("inactive") || msg.contains("disabled")) {
                redirect = "/login?error=inactive";
            } else if (msg.contains("permission") || msg.contains("role") || msg.contains("noperm")) {
                redirect = "/login?error=noperm";
            } else {
                // fallback for DisabledException
                redirect = "/login?error=inactive";
            }
        }

        response.sendRedirect(redirect);
    }
}
