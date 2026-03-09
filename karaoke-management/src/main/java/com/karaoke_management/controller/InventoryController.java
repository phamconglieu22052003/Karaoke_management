package com.karaoke_management.controller;

import com.karaoke_management.entity.InventoryReceipt;
import com.karaoke_management.entity.InventoryStock;
import com.karaoke_management.entity.StockMovement;
import com.karaoke_management.enums.InventoryReceiptType;
import com.karaoke_management.repository.ProductRepository;
import com.karaoke_management.repository.UserRepository;
import com.karaoke_management.service.InventoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public InventoryController(InventoryService inventoryService,
                               ProductRepository productRepository,
                               UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // UC1: Xem tồn kho
    @GetMapping("/stocks")
    public String stocks(Model model) {
        List<InventoryStock> stocks = inventoryService.listStocks();
        model.addAttribute("stocks", stocks);
        return "inventory/stock-list";
    }

    @PostMapping("/stocks/{id}/min")
    public String updateMinQty(@PathVariable("id") Long stockId,
                               @RequestParam("minQty") Integer minQty,
                               RedirectAttributes ra) {
        inventoryService.updateMinQty(stockId, minQty);
        ra.addFlashAttribute("msg", "Đã cập nhật mức tồn tối thiểu");
        return "redirect:/inventory/stocks";
    }

    // UC2 + UC3: Danh sách phiếu
    @GetMapping("/receipts")
    public String receipts(@RequestParam(value = "status", required = false) String status,
                           Model model) {
        model.addAttribute("receipts", inventoryService.listReceipts(status));
        model.addAttribute("status", (status == null || status.isBlank()) ? "ALL" : status.toUpperCase());
        return "inventory/receipt-list";
    }

    // UC2: Tạo phiếu
    // Admin = Manager: Quản lý phải có full quyền thao tác kho giống nhân viên kho
    @PreAuthorize("hasAnyRole('STOREKEEPER','MANAGER')")
    @GetMapping("/receipts/new")
    public String newReceipt(@RequestParam(value = "type", required = false) String type,
                             Model model) {
        InventoryReceiptType t = (type == null || type.isBlank()) ? InventoryReceiptType.IN : InventoryReceiptType.valueOf(type.toUpperCase());
        model.addAttribute("receipt", new InventoryReceipt());
        model.addAttribute("type", t);
        model.addAttribute("products", productRepository.findActiveWithCategory());
        model.addAttribute("editMode", false);
        return "inventory/receipt-form";
    }

    // UC2: Lưu phiếu (draft hoặc submit)
    @PreAuthorize("hasAnyRole('STOREKEEPER','MANAGER')")
    @PostMapping("/receipts")
    public String createReceipt(@RequestParam("type") String type,
                                @RequestParam(value = "note", required = false) String note,
                                @RequestParam(value = "productId", required = false) List<Long> productIds,
                                @RequestParam(value = "qty", required = false) List<Integer> qtys,
                                @RequestParam(value = "unitPrice", required = false) List<BigDecimal> unitPrices,
                                @RequestParam(value = "action", required = false, defaultValue = "save") String action,
                                Authentication auth,
                                RedirectAttributes ra,
                                Model model) {
        try {
            Long uid = userRepository.findByUsername(auth.getName()).orElseThrow().getId();
            boolean submit = "submit".equalsIgnoreCase(action);
            InventoryReceipt saved = inventoryService.createOrUpdateReceipt(null,
                    InventoryReceiptType.valueOf(type.toUpperCase()),
                    note,
                    uid,
                    productIds,
                    qtys,
                    unitPrices,
                    submit);
            ra.addFlashAttribute("msg", submit ? "Đã gửi phiếu để duyệt" : "Đã lưu phiếu (DRAFT)" );
            return "redirect:/inventory/receipts/" + saved.getId();
        } catch (Exception ex) {
            // Render lại form
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("type", InventoryReceiptType.valueOf(type.toUpperCase()));
            model.addAttribute("products", productRepository.findActiveWithCategory());
            model.addAttribute("editMode", false);
            return "inventory/receipt-form";
        }
    }

    // UC2: Sửa phiếu DRAFT
    @PreAuthorize("hasAnyRole('STOREKEEPER','MANAGER')")
    @GetMapping("/receipts/{id}/edit")
    public String editReceipt(@PathVariable("id") Long id, Model model) {
        InventoryReceipt r = inventoryService.getReceiptWithLines(id);
        model.addAttribute("receipt", r);
        model.addAttribute("type", r.getType());
        model.addAttribute("products", productRepository.findActiveWithCategory());
        model.addAttribute("editMode", true);
        return "inventory/receipt-form";
    }

    @PreAuthorize("hasAnyRole('STOREKEEPER','MANAGER')")
    @PostMapping("/receipts/{id}")
    public String updateReceipt(@PathVariable("id") Long id,
                                @RequestParam("type") String type,
                                @RequestParam(value = "note", required = false) String note,
                                @RequestParam(value = "productId", required = false) List<Long> productIds,
                                @RequestParam(value = "qty", required = false) List<Integer> qtys,
                                @RequestParam(value = "unitPrice", required = false) List<BigDecimal> unitPrices,
                                @RequestParam(value = "action", required = false, defaultValue = "save") String action,
                                Authentication auth,
                                RedirectAttributes ra,
                                Model model) {
        try {
            Long uid = userRepository.findByUsername(auth.getName()).orElseThrow().getId();
            boolean submit = "submit".equalsIgnoreCase(action);
            InventoryReceipt saved = inventoryService.createOrUpdateReceipt(id,
                    InventoryReceiptType.valueOf(type.toUpperCase()),
                    note,
                    uid,
                    productIds,
                    qtys,
                    unitPrices,
                    submit);
            ra.addFlashAttribute("msg", submit ? "Đã gửi phiếu để duyệt" : "Đã cập nhật phiếu (DRAFT)" );
            return "redirect:/inventory/receipts/" + saved.getId();
        } catch (Exception ex) {
            InventoryReceipt r = inventoryService.getReceiptWithLines(id);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("receipt", r);
            model.addAttribute("type", InventoryReceiptType.valueOf(type.toUpperCase()));
            model.addAttribute("products", productRepository.findActiveWithCategory());
            model.addAttribute("editMode", true);
            return "inventory/receipt-form";
        }
    }

    // UC2+3: Chi tiết phiếu
    @GetMapping("/receipts/{id}")
    public String receiptDetail(@PathVariable("id") Long id, Model model) {
        InventoryReceipt r = inventoryService.getReceiptWithLines(id);
        model.addAttribute("receipt", r);
        return "inventory/receipt-detail";
    }

    // UC3: Duyệt/Từ chối
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/receipts/{id}/approve")
    public String approve(@PathVariable("id") Long id,
                          Authentication auth,
                          RedirectAttributes ra) {
        Long uid = userRepository.findByUsername(auth.getName()).orElseThrow().getId();
        inventoryService.approveReceipt(id, uid);
        ra.addFlashAttribute("msg", "Đã duyệt phiếu");
        return "redirect:/inventory/receipts/" + id;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/receipts/{id}/reject")
    public String reject(@PathVariable("id") Long id,
                         @RequestParam(value = "reason", required = false) String reason,
                         Authentication auth,
                         RedirectAttributes ra) {
        Long uid = userRepository.findByUsername(auth.getName()).orElseThrow().getId();
        inventoryService.rejectReceipt(id, uid, reason);
        ra.addFlashAttribute("msg", "Đã từ chối phiếu");
        return "redirect:/inventory/receipts/" + id;
    }

    // UC4: Lịch sử xuất/nhập
    @GetMapping("/movements")
    public String movements(@RequestParam(value = "productId", required = false) Long productId,
                            Model model) {
        List<StockMovement> moves = inventoryService.listMovements(productId);
        model.addAttribute("movements", moves);
        model.addAttribute("products", productRepository.findActiveWithCategory());
        model.addAttribute("productId", productId);
        return "inventory/movement-list";
    }
}
