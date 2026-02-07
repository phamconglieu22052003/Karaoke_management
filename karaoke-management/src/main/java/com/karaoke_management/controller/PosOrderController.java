package com.karaoke_management.controller;

import com.karaoke_management.entity.Order;
import com.karaoke_management.repository.OrderRepository;
import com.karaoke_management.repository.ProductRepository;
import com.karaoke_management.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
@RequestMapping("/pos")
public class PosOrderController {

    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    @GetMapping("/sessions/{sessionId}/order")
    public String orderScreen(@PathVariable Long sessionId, Model model, Authentication auth) {
        String username = (auth != null) ? auth.getName() : "system";

        Order order = orderService.getOrCreateOpenOrder(sessionId, username);

        model.addAttribute("order", order);
        model.addAttribute("products", productRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("readOnly", false);
        return "pos/order-screen";
    }

    @GetMapping("/sessions/{sessionId}/order/view")
    public String viewLastOrder(@PathVariable Long sessionId, Model model, Authentication auth) {
    return orderRepository.findTopByRoomSession_IdOrderByCreatedAtDesc(sessionId)
            .map(order -> {
                model.addAttribute("order", order);
                model.addAttribute("products", productRepository.findByActiveTrueOrderByNameAsc());
                model.addAttribute("readOnly", true);
                return "pos/order-screen";
            })
            // ✅ chưa có order -> chuyển sang màn gọi món (tạo OPEN)
            .orElseGet(() -> "redirect:/pos/sessions/" + sessionId + "/order");
    }

    /**
     * Thêm món vào order OPEN (gộp dòng nếu trùng product)
     */
    @PostMapping("/sessions/{sessionId}/order/items")
    public String addItem(@PathVariable Long sessionId,
                          @RequestParam Long productId,
                          @RequestParam int quantity,
                          @RequestParam(required = false) String note,
                          Authentication auth) {
        String username = (auth != null) ? auth.getName() : "system";

        orderService.addItem(sessionId, productId, quantity, note, username);
        return "redirect:/pos/sessions/" + sessionId + "/order";
    }

    /**
     * Cập nhật số lượng 1 dòng món (chỉ cho order OPEN)
     * - redirect về màn gọi món của session tương ứng
     */
    @PostMapping("/order-items/{itemId}/update")
    public String updateQty(@PathVariable Long itemId,
                            @RequestParam int quantity,
                            @RequestParam Long sessionId) {
        orderService.updateItemQuantity(itemId, quantity);
        return "redirect:/pos/sessions/" + sessionId + "/order";
    }

    /**
     * Xóa 1 dòng món (chỉ cho order OPEN)
     */
    @PostMapping("/order-items/{itemId}/delete")
    public String deleteItem(@PathVariable Long itemId,
                             @RequestParam Long sessionId) {
        orderService.removeItem(itemId);
        return "redirect:/pos/sessions/" + sessionId + "/order";
    }

    /**
     * Chốt order (OPEN -> CLOSED)
     */
    @PostMapping("/sessions/{sessionId}/order/close")
    public String close(@PathVariable Long sessionId) {
        orderService.closeOrder(sessionId);
        return "redirect:/pos/sessions/" + sessionId + "/order/view";
    }
}
