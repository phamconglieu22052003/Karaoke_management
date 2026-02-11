package com.karaoke_management.service;

import com.karaoke_management.entity.Order;

public interface OrderService {

    /**
     * Lấy order OPEN hiện tại của session. Nếu chưa có thì tạo mới.
     */
    Order getOrCreateOpenOrder(Long roomSessionId, String username);

    /**
     * Thêm món vào order OPEN. Nếu món đã có thì cộng dồn số lượng.
     */
    void addItem(Long roomSessionId, Long productId, Integer quantity, String note, String username);

    /**
     * Update item: cập nhật quantity + note (đồng bộ controller kiểu mới)
     */
    void updateItem(Long itemId, Integer quantity, String note);

    /**
     * Xóa item khỏi order
     */
    void removeItem(Long itemId);

    /**
     * Chốt order OPEN -> CLOSED
     */
    void closeOpenOrder(Long roomSessionId, String username);
}
