package com.karaoke_management.controller;

import com.karaoke_management.entity.Invoice;
import com.karaoke_management.entity.InvoiceLine;
import com.karaoke_management.entity.InvoiceStatus;
import com.karaoke_management.enums.InvoiceLineType;
import com.karaoke_management.repository.InvoiceLineRepository;
import com.karaoke_management.repository.InvoiceRepository;
import com.karaoke_management.repository.RoomRepository;
import com.karaoke_management.repository.ShiftRepository;
import com.karaoke_management.enums.ShiftStatus;
import com.karaoke_management.service.CustomerPayLinkService;
import com.karaoke_management.service.InvoiceService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/invoice")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceService invoiceService;
    private final RoomRepository roomRepository;
    private final ShiftRepository shiftRepository;
    private final CustomerPayLinkService customerPayLinkService;

    // ✅ Format VN: giờ/ngày/tháng/năm
    private static final DateTimeFormatter VN_DTF = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public InvoiceController(InvoiceRepository invoiceRepository,
                             InvoiceLineRepository invoiceLineRepository,
                             InvoiceService invoiceService,
                             RoomRepository roomRepository,
                             ShiftRepository shiftRepository,
                             CustomerPayLinkService customerPayLinkService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.invoiceService = invoiceService;
        this.roomRepository = roomRepository;
        this.shiftRepository = shiftRepository;
        this.customerPayLinkService = customerPayLinkService;
    }

    // /invoice?from=...&to=...&min=...&max=...&roomId=...
    @GetMapping
    public String list(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            @RequestParam(required = false) Long roomId,
            Model model
    ) {
        LocalDateTime fromDt = parseVnDateTimeOrNull(from);
        LocalDateTime toDt = parseVnDateTimeOrNull(to);

        model.addAttribute("invoices",
                invoiceRepository.filterInvoices(fromDt, toDt, min, max, roomId)
        );

        model.addAttribute("from", from == null ? "" : from);
        model.addAttribute("to", to == null ? "" : to);
        model.addAttribute("min", min);
        model.addAttribute("max", max);
        model.addAttribute("roomId", roomId);

        // danh sách phòng cho dropdown filter
        model.addAttribute("rooms", roomRepository.findAll());

        model.addAttribute("dtPattern", "HH:mm dd/MM/yyyy");
        return "invoice/invoice-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        // ✅ Đảm bảo tổng tiền hiển thị đúng (tiền phòng + tiền gọi món) cho invoice chưa thanh toán
        // Tránh trường hợp invoice được tạo từ bản code cũ chỉ tính tiền phòng.
        if ((inv.getStatus() == InvoiceStatus.UNPAID || inv.getStatus() == InvoiceStatus.FAILED)
                && inv.getRoomSession() != null
                && inv.getRoomSession().getId() != null) {
            inv = invoiceService.createOrGetBySession(inv.getRoomSession().getId());
        }
        model.addAttribute("invoice", inv);

        model.addAttribute("hasOpenShift", shiftRepository.existsByStatus(ShiftStatus.OPEN));

        // ✅ Link công khai cho khách hàng (chỉ xem QR, không tự mark PAID)
        model.addAttribute("customerPayUrl", customerPayLinkService.buildCustomerPayUrl(inv.getId()));

        // Snapshot lines + breakdown
        var lines = invoiceLineRepository.findAllByInvoice_IdOrderByIdAsc(inv.getId());
        model.addAttribute("lines", lines);
        model.addAttribute("roomCharge", sumByType(lines, InvoiceLineType.ROOM));
        model.addAttribute("orderCharge", sumByType(lines, InvoiceLineType.ITEM));
        return "invoice/invoice-detail";
    }

    @GetMapping("/{id}/print")
    public String print(@PathVariable Long id, Model model) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        // Với invoice chưa thanh toán, vẫn refresh snapshot để in đúng
        if ((inv.getStatus() == InvoiceStatus.UNPAID || inv.getStatus() == InvoiceStatus.FAILED)
                && inv.getRoomSession() != null
                && inv.getRoomSession().getId() != null) {
            inv = invoiceService.createOrGetBySession(inv.getRoomSession().getId());
        }

        var lines = invoiceLineRepository.findAllByInvoice_IdOrderByIdAsc(inv.getId());
        model.addAttribute("invoice", inv);
        model.addAttribute("lines", lines);
        model.addAttribute("roomCharge", sumByType(lines, InvoiceLineType.ROOM));
        model.addAttribute("orderCharge", sumByType(lines, InvoiceLineType.ITEM));
        return "invoice/invoice-print";
    }

    /**
     * Thanh toán tiền mặt ngay tại trang chi tiết hóa đơn.
     * POST: /invoice/{id}/pay-cash
     */
    @PostMapping("/{id}/pay-cash")
    public String payCash(@PathVariable("id") Long invoiceId, RedirectAttributes ra) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        // ✅ Yêu cầu mở ca trước khi thanh toán
        if (!shiftRepository.existsByStatus(ShiftStatus.OPEN)) {
            ra.addFlashAttribute("error", "Bạn cần mở ca trước khi thanh toán.");
            return "redirect:/shift/open?returnUrl=/invoice/" + invoiceId;
        }

        // Chặn thanh toán lặp
        if (inv.getStatus() == InvoiceStatus.PAID) {
            return "redirect:/invoice/" + invoiceId;
        }

        // Đánh dấu đã thanh toán + trừ kho (idempotent)
        try {
            invoiceService.markPaidAndDeductInventory(invoiceId, "CASH", null);
            ra.addFlashAttribute("success", "Thanh toán tiền mặt thành công.");
        } catch (ResponseStatusException e) {
            ra.addFlashAttribute("error", e.getReason() != null ? e.getReason() : "Thanh toán thất bại");
        }
        return "redirect:/invoice/" + invoiceId;
    }

    private static BigDecimal sumByType(java.util.List<InvoiceLine> lines, InvoiceLineType type) {
        if (lines == null || lines.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (InvoiceLine l : lines) {
            if (l != null && l.getLineType() == type && l.getAmount() != null) {
                sum = sum.add(l.getAmount());
            }
        }
        return sum;
    }

    // =========================
    // EXPORT CSV (giữ nguyên)
    // =========================
    /**
     * Xuất danh sách hóa đơn theo bộ lọc hiện tại ra CSV.
     * URL: /invoice/export?from=...&to=...&min=...&max=...&roomId=...
     */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            @RequestParam(required = false) Long roomId
    ) {
        LocalDateTime fromDt = parseVnDateTimeOrNull(from);
        LocalDateTime toDt = parseVnDateTimeOrNull(to);

        var invoices = invoiceRepository.filterInvoices(fromDt, toDt, min, max, roomId);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM để Excel đọc tiếng Việt đúng
        sb.append('\uFEFF');
        sb.append("ID,Session,Phòng,Tổng tiền,Tạo lúc,Trạng thái,Thanh toán lúc\n");

        for (Invoice inv : invoices) {
            String sessionId = (inv.getRoomSession() != null && inv.getRoomSession().getId() != null)
                    ? String.valueOf(inv.getRoomSession().getId()) : "";
            String roomName = (inv.getRoomSession() != null && inv.getRoomSession().getRoom() != null
                    && inv.getRoomSession().getRoom().getName() != null)
                    ? inv.getRoomSession().getRoom().getName() : "";
            String total = inv.getTotalAmount() != null ? inv.getTotalAmount().toPlainString() : "";
            String created = inv.getCreatedAt() != null ? dtf.format(inv.getCreatedAt()) : "";
            String status = inv.getStatus() != null ? inv.getStatus().name() : "";
            String paid = inv.getPaidAt() != null ? dtf.format(inv.getPaidAt()) : "";

            sb.append(csv(inv.getId() != null ? inv.getId().toString() : ""))
              .append(',').append(csv(sessionId))
              .append(',').append(csv(roomName))
              .append(',').append(csv(total))
              .append(',').append(csv(created))
              .append(',').append(csv(status))
              .append(',').append(csv(paid))
              .append('\n');
        }

        String filename = "invoices-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // =========================
    // EXPORT EXCEL (mới thêm)
    // =========================
    /**
     * Xuất danh sách hóa đơn theo bộ lọc hiện tại ra Excel .xlsx
     * URL: /invoice/export/excel?from=...&to=...&min=...&max=...&roomId=...
     */
    @GetMapping(
            value = "/export/excel",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            @RequestParam(required = false) Long roomId
    ) {
        LocalDateTime fromDt = parseVnDateTimeOrNull(from);
        LocalDateTime toDt = parseVnDateTimeOrNull(to);

        var invoices = invoiceRepository.filterInvoices(fromDt, toDt, min, max, roomId);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Invoices");

            // Styles
            Font headerFont = wb.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);

            CellStyle moneyStyle = wb.createCellStyle();
            DataFormat df = wb.createDataFormat();
            moneyStyle.setDataFormat(df.getFormat("#,##0"));

            // Header row
            Row header = sheet.createRow(0);
            String[] cols = {"ID", "Session", "Phòng", "Tổng tiền", "Tạo lúc", "Trạng thái", "Thanh toán lúc"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

            int r = 1;
            for (Invoice inv : invoices) {
                Row row = sheet.createRow(r++);

                String sessionId = (inv.getRoomSession() != null && inv.getRoomSession().getId() != null)
                        ? String.valueOf(inv.getRoomSession().getId()) : "";
                String roomName = (inv.getRoomSession() != null && inv.getRoomSession().getRoom() != null
                        && inv.getRoomSession().getRoom().getName() != null)
                        ? inv.getRoomSession().getRoom().getName() : "";
                String created = inv.getCreatedAt() != null ? dtf.format(inv.getCreatedAt()) : "";
                String status = inv.getStatus() != null ? inv.getStatus().name() : "";
                String paid = inv.getPaidAt() != null ? dtf.format(inv.getPaidAt()) : "";

                row.createCell(0).setCellValue(inv.getId() != null ? inv.getId() : 0L);
                row.createCell(1).setCellValue(sessionId);
                row.createCell(2).setCellValue(roomName);

                Cell moneyCell = row.createCell(3);
                if (inv.getTotalAmount() != null) {
                    moneyCell.setCellValue(inv.getTotalAmount().doubleValue());
                    moneyCell.setCellStyle(moneyStyle);
                } else {
                    moneyCell.setCellValue("");
                }

                row.createCell(4).setCellValue(created);
                row.createCell(5).setCellValue(status);
                row.createCell(6).setCellValue(paid);
            }

            // Autosize
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);

            String filename = "invoices-" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    ))
                    .body(out.toByteArray());

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Export Excel failed", e);
        }
    }

    /** Escape đơn giản cho CSV */
    private static String csv(String s) {
        if (s == null) return "";
        boolean needQuote = s.contains(",") || s.contains("\n") || s.contains("\r") || s.contains("\"");
        String v = s.replace("\"", "\"\"");
        return needQuote ? ("\"" + v + "\"") : v;
    }

    @GetMapping("/create/{roomSessionId}")
    public String createBySessionGet(@PathVariable Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    @PostMapping("/create/{roomSessionId}")
    public String createBySessionPost(@PathVariable Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    @GetMapping("/create")
    public String createBySessionQueryGet(@RequestParam("roomSessionId") Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    @PostMapping("/create")
    public String createBySessionQueryPost(@RequestParam("roomSessionId") Long roomSessionId) {
        Invoice inv = invoiceService.createOrGetBySession(roomSessionId);
        return "redirect:/invoice/" + inv.getId();
    }

    private LocalDateTime parseVnDateTimeOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.trim(), VN_DTF);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
