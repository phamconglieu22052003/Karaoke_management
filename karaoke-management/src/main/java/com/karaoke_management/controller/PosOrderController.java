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

        RoomSession roomSession = roomSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy session: " + sessionId));

        // === FIX UX ===
        // Trước đây: luôn "getOrCreateOpenOrder" => nếu order đã CHỐT (CLOSED) thì vào lại /order
        // sẽ tự tạo order OPEN mới rỗng => người dùng tưởng "mất dữ liệu".
        //
        // Hành vi mới:
        // 1) Nếu có order OPEN => hiển thị để tiếp tục gọi món.
        // 2) Nếu KHÔNG có order OPEN nhưng có order gần nhất (thường là CLOSED) => hiển thị order đó (read-only)
        //    và cung cấp nút "Tạo order mới" nếu muốn gọi thêm.
        // 3) Nếu chưa từng có order nào => tạo order OPEN mới như bình thường.

        Order order;
        boolean canCreateNewOrder = false;
        String autoWarn = null;

        var openOpt = orderRepository.findFirstByRoomSession_IdAndStatusOrderByCreatedAtDesc(sessionId, OrderStatus.OPEN);
        if (openOpt.isPresent()) {
            order = openOpt.get();
        } else {
            var lastOpt = orderRepository.findFirstByRoomSession_IdOrderByCreatedAtDesc(sessionId);
            if (lastOpt.isPresent()) {
                order = lastOpt.get();
                if (order.getStatus() == OrderStatus.CLOSED) {
                    canCreateNewOrder = true;
                    autoWarn = "Order đã chốt. Nếu muốn gọi thêm món, bấm 'Tạo order mới'.";
                }
            } else {
                // chưa có order nào -> tạo OPEN mới
                order = orderService.getOrCreateOpenOrder(sessionId, username);
            }
        }

        boolean readOnly = (order.getStatus() == OrderStatus.CLOSED);

        List<OrderItem> items = (order.getItems() == null) ? List.of() : order.getItems();
        items = items.stream()
                .sorted(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        BigDecimal total = items.stream()
                .map(it -> it.getLineAmount() == null ? BigDecimal.ZERO : it.getLineAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Luôn lấy order CLOSED gần nhất để hiển thị "lịch sử" (kể cả khi đang có OPEN order).
        // Điều này giúp trường hợp user đã chốt order rồi vào lại, thấy OPEN order mới (rỗng) nhưng vẫn xem lại được order đã chốt.
        Order lastClosedOrder = orderRepository
                .findFirstByRoomSession_IdAndStatusOrderByCreatedAtDesc(sessionId, OrderStatus.CLOSED)
                .orElse(null);

        List<OrderItem> lastClosedItems = List.of();
        BigDecimal lastClosedTotal = BigDecimal.ZERO;
        if (lastClosedOrder != null && (order.getId() == null || !lastClosedOrder.getId().equals(order.getId()))) {
            lastClosedItems = (lastClosedOrder.getItems() == null) ? List.of() : lastClosedOrder.getItems();
            lastClosedItems = lastClosedItems.stream()
                    .sorted(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Long::compareTo)))
                    .toList();

            lastClosedTotal = lastClosedItems.stream()
                    .map(it -> it.getLineAmount() == null ? BigDecimal.ZERO : it.getLineAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // flash messages compatibility
        if (success != null && !success.isBlank() && (msg == null || msg.isBlank())) {
            model.addAttribute("msg", success);
        }
        if (error != null && !error.isBlank() && (warn == null || warn.isBlank())) {
            model.addAttribute("warn", error);
        }
        if (warn != null && !warn.isBlank()) model.addAttribute("warn", warn);
        if (msg != null && !msg.isBlank()) model.addAttribute("msg", msg);

        // auto warn nếu đang xem order CLOSED và không có flash warn/error
        if (autoWarn != null && (warn == null || warn.isBlank()) && (error == null || error.isBlank())) {
            model.addAttribute("warn", autoWarn);
        }

        // !!! QUAN TRỌNG: KHÔNG dùng tên "session" vì Thymeleaf đã có biến built-in session (HttpSession)
        model.addAttribute("roomSession", roomSession);

        model.addAttribute("order", order);
        model.addAttribute("items", items);
        model.addAttribute("orderTotal", total);
        model.addAttribute("readOnly", readOnly);
        model.addAttribute("canCreateNewOrder", canCreateNewOrder);

        model.addAttribute("lastClosedOrder", lastClosedOrder);
        model.addAttribute("lastClosedItems", lastClosedItems);
        model.addAttribute("lastClosedTotal", lastClosedTotal);

        model.addAttribute("categories", productCategoryRepository.findAll());
        model.addAttribute("products", productRepository.findByActiveTrueOrderByNameAsc());

        return "pos/order-screen";
    }

    /**
     * (FIX) Nếu ai đó gõ nhầm URL POST endpoint bằng GET:
     * GET /pos/sessions/{sessionId}/order/items  -> redirect về màn order
     *
     * Tránh Whitelabel 400 khi bạn lỡ mở:
     * /pos/sessions//order/items hoặc /pos/sessions/{id}/order/items bằng browser.
     */
    @GetMapping("/sessions/{sessionId}/order/items")
    public String redirectItemsGet(@PathVariable Long sessionId) {
        return "redirect:/pos/sessions/" + sessionId + "/order";
    }

    /**
     * Tạo order mới (OPEN) để gọi tiếp sau khi đã chốt.
     * POST /pos/sessions/{sessionId}/order/new
     */
    @PostMapping("/sessions/{sessionId}/order/new")
    public String createNewOrder(@PathVariable Long sessionId,
                                 Authentication authentication,
                                 RedirectAttributes ra) {
        String username = (authentication != null) ? authentication.getName() : "system";
        try {
            // service sẽ trả về OPEN hiện có nếu đã tồn tại, hoặc tạo mới nếu chưa có
            orderService.getOrCreateOpenOrder(sessionId, username);
            ra.addFlashAttribute("msg", "Đã tạo order mới — bạn có thể gọi thêm món.");
        } catch (Exception ex) {
            ra.addFlashAttribute("warn", ex.getMessage());
        }
        return "redirect:/pos/sessions/" + sessionId + "/order";
    }

    /**
     * Xem order gần nhất theo session (ưu tiên CLOSED) - readOnly
     * GET /pos/sessions/{sessionId}/order/view
     */
    @GetMapping("/sessions/{sessionId}/order/view")
    public String viewLastOrder(@PathVariable Long sessionId,
                                Model model,
                                RedirectAttributes ra) {

        RoomSession roomSession = roomSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy session: " + sessionId));

        // Ưu tiên hiển thị order CLOSED (đã chốt). Nếu không có CLOSED thì fallback về order mới nhất.
        return orderRepository.findFirstByRoomSession_IdAndStatusOrderByCreatedAtDesc(sessionId, OrderStatus.CLOSED)
                .or(() -> orderRepository.findFirstByRoomSession_IdOrderByCreatedAtDesc(sessionId))
                .map(order -> {
                    List<OrderItem> items = (order.getItems() == null) ? List.of() : order.getItems();

                    // sort để hiển thị ổn định
                    items = items.stream()
                            .sorted(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Long::compareTo)))
                            .toList();

                    BigDecimal total = items.stream()
                            .map(it -> it.getLineAmount() == null ? BigDecimal.ZERO : it.getLineAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // !!! QUAN TRỌNG: KHÔNG dùng tên "session"
                    model.addAttribute("roomSession", roomSession);

                    model.addAttribute("order", order);
                    model.addAttribute("items", items);
                    model.addAttribute("orderTotal", total);
                    model.addAttribute("readOnly", true);
                    model.addAttribute("canCreateNewOrder", false);
                    model.addAttribute("lastClosedOrder", null);
                    model.addAttribute("lastClosedItems", List.of());
                    model.addAttribute("lastClosedTotal", BigDecimal.ZERO);
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

        // UX POS: chốt xong thì chuyển sang xem lại (read-only)
        return "redirect:/pos/sessions/" + sessionId + "/order/view";
    }
}
