package com.karaoke_management.controller;

import com.karaoke_management.entity.Order;
import com.karaoke_management.entity.OrderItem;
import com.karaoke_management.entity.OrderStatus;
import com.karaoke_management.entity.RoomSession;
import com.karaoke_management.repository.OrderRepository;
import com.karaoke_management.repository.ProductCategoryRepository;
import com.karaoke_management.repository.ProductRepository;
import com.karaoke_management.repository.RoomSessionRepository;
import com.karaoke_management.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/pos")
public class PosOrderController {

    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final RoomSessionRepository roomSessionRepository;
    private final OrderRepository orderRepository;

    /**
     * POS - Gọi món
     * GET /pos/sessions/{sessionId}/order
     */
    @GetMapping("/sessions/{sessionId}/order")
    public String orderScreen(@PathVariable Long sessionId,
                              Model model,
                              Authentication authentication,
                              @ModelAttribute("msg") String msg,
                              @ModelAttribute("warn") String warn,
                              @ModelAttribute("error") String error,
                              @ModelAttribute("success") String success) {

        String username = (authentication != null) ? authentication.getName() : "system";

        RoomSession session = roomSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy session: " + sessionId));

        Order order = orderService.getOrCreateOpenOrder(sessionId, username);

        boolean readOnly = (order.getStatus() == OrderStatus.CLOSED);

        List<OrderItem> items = (order.getItems() == null) ? List.of() : order.getItems();
        items = items.stream()
                .sorted(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        BigDecimal total = items.stream()
                .map(it -> it.getLineAmount() == null ? BigDecimal.ZERO : it.getLineAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // flash messages compatibility
        if (success != null && !success.isBlank() && (msg == null || msg.isBlank())) {
            model.addAttribute("msg", success);
        }
        if (error != null && !error.isBlank() && (warn == null || warn.isBlank())) {
            model.addAttribute("warn", error);
        }
        if (warn != null && !warn.isBlank()) model.addAttribute("warn", warn);
        if (msg != null && !msg.isBlank()) model.addAttribute("msg", msg);

        model.addAttribute("session", session);
        model.addAttribute("order", order);
        model.addAttribute("items", items);
        model.addAttribute("orderTotal", total);
        model.addAttribute("readOnly", readOnly);

        model.addAttribute("categories", productCategoryRepository.findAll());
        model.addAttribute("products", productRepository.findByActiveTrueOrderByNameAsc());

        return "pos/order-screen";
    }

    /**
     * Xem order gần nhất (CLOSED) theo session (readOnly)
     * GET /pos/sessions/{sessionId}/order/view
     */
    @GetMapping("/sessions/{sessionId}/order/view")
    public String viewLastOrder(@PathVariable Long sessionId,
                                Model model,
                                RedirectAttributes ra) {

        RoomSession session = roomSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy session: " + sessionId));

        return orderRepository.findFirstByRoomSession_IdOrderByCreatedAtDesc(sessionId)
                .map(order -> {
                    List<OrderItem> items = (order.getItems() == null) ? List.of() : order.getItems();
                    BigDecimal total = items.stream()
                            .map(it -> it.getLineAmount() == null ? BigDecimal.ZERO : it.getLineAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    model.addAttribute("session", session);
                    model.addAttribute("order", order);
                    model.addAttribute("items", items);
                    model.addAttribute("orderTotal", total);
                    model.addAttribute("readOnly", true);
                    model.addAttribute("categories", productCategoryRepository.findAll());
                    model.addAttribute("products", productRepository.findByActiveTrueOrderByNameAsc());
                    ra.addFlashAttribute("warn", "Bạn đang xem order đã chốt (CLOSED) — chỉ xem lại.");
                    return "pos/order-screen";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("warn", "Chưa có order cho session: " + sessionId);
                    return "redirect:/pos/sessions/" + sessionId + "/order";
                });
    }

    /**
     * Thêm món vào order OPEN của session
     * POST /pos/sessions/{sessionId}/order/items
     */
    @PostMapping("/sessions/{sessionId}/order/items")
    public String addItem(@PathVariable Long sessionId,
                          @RequestParam Long productId,
                          @RequestParam Integer quantity,
                          @RequestParam(required = false) String note,
                          Authentication authentication,
                          RedirectAttributes ra) {

        String username = (authentication != null) ? authentication.getName() : "system";
        try {
            orderService.addItem(sessionId, productId, quantity, note, username);
            ra.addFlashAttribute("msg", "Đã thêm món.");
        } catch (Exception ex) {
            ra.addFlashAttribute("warn", ex.getMessage());
        }
        return "redirect:/pos/sessions/" + sessionId + "/order";
    }

    /**
     * Cập nhật item: quantity + note
     * POST /pos/sessions/{sessionId}/order/items/{itemId}/update
     */
    @PostMapping("/sessions/{sessionId}/order/items/{itemId}/update")
    public String updateItem(@PathVariable Long sessionId,
                             @PathVariable Long itemId,
                             @RequestParam Integer quantity,
                             @RequestParam(required = false) String note,
                             RedirectAttributes ra) {
        try {
            orderService.updateItem(itemId, quantity, note);
            ra.addFlashAttribute("msg", "Đã cập nhật.");
        } catch (Exception ex) {
            ra.addFlashAttribute("warn", ex.getMessage());
        }
        return "redirect:/pos/sessions/" + sessionId + "/order";
    }

    /**
     * Xóa item
     * POST /pos/sessions/{sessionId}/order/items/{itemId}/delete
     */
    @PostMapping("/sessions/{sessionId}/order/items/{itemId}/delete")
    public String deleteItem(@PathVariable Long sessionId,
                             @PathVariable Long itemId,
                             RedirectAttributes ra) {
        try {
            orderService.removeItem(itemId);
            ra.addFlashAttribute("msg", "Đã xóa món.");
        } catch (Exception ex) {
            ra.addFlashAttribute("warn", ex.getMessage());
        }
        return "redirect:/pos/sessions/" + sessionId + "/order";
    }

    /**
     * Chốt order (OPEN -> CLOSED)
     * POST /pos/sessions/{sessionId}/order/close
     */
    @PostMapping("/sessions/{sessionId}/order/close")
    public String closeOrder(@PathVariable Long sessionId,
                             Authentication authentication,
                             RedirectAttributes ra) {
        String username = (authentication != null) ? authentication.getName() : "system";
        try {
            orderService.closeOpenOrder(sessionId, username);
            ra.addFlashAttribute("msg", "Đã chốt order.");
        } catch (Exception ex) {
            ra.addFlashAttribute("warn", ex.getMessage());
        }
        return "redirect:/pos/sessions/" + sessionId + "/order";
    }
}
