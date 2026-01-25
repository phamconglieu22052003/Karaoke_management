package com.karaoke_management.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Trả ảnh QR PNG từ một chuỗi data (thường là URL).
 * Ví dụ: /qr?data=https://example.com
 */
@RestController
public class QrController {

    @GetMapping("/qr")
    public void qr(@RequestParam("data") String data, HttpServletResponse resp) throws IOException {
        resp.setContentType("image/png");
        try {
            // đảm bảo UTF-8 không bị lỗi khi đi qua URL
            String text = new String(data.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 340, 340);
            MatrixToImageWriter.writeToStream(matrix, "PNG", resp.getOutputStream());
        } catch (WriterException e) {
            resp.sendError(500, "QR generate error");
        }
    }
}
