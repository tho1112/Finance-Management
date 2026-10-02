package com.example.demo.controller;

import com.example.demo.dto.ReceiptScanResponse;
import com.example.demo.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ReceiptController {

    private final ReceiptService receiptService;

    @Value("${file.upload-dir:uploads/receipts}")
    private String uploadDir;

    /**
     * 1. Quét hoá đơn từ file ảnh (BFF Gateway forward sang Python OCR Microservice)
     */
    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> scanReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "x-gemini-api-key", required = false) String customApiKey,
            Principal principal) {
        try {
            String username = (principal != null) ? principal.getName() : null;
            ReceiptScanResponse response = receiptService.processReceipt(file, username, customApiKey);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi quét hoá đơn: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi xử lý hoá đơn: " + e.getMessage()));
        }
    }

    /**
     * 2. Phân tích giao dịch từ giọng nói hoặc văn bản tự nhiên bằng AI
     */
    @PostMapping(value = "/parse-text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> parseTextTransaction(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "x-gemini-api-key", required = false) String customApiKey,
            Principal principal) {
        try {
            String text = (payload != null) ? payload.get("text") : null;
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Văn bản/câu nói không được để trống!"));
            }

            String username = (principal != null) ? principal.getName() : null;
            ReceiptScanResponse response = receiptService.processTextTransaction(text, username, customApiKey);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi phân tích câu giao dịch bằng AI: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi xử lý AI: " + e.getMessage()));
        }
    }

    /**
     * 3. Phục vụ hiển thị file ảnh hoá đơn đã lưu
     */
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveReceiptImage(@PathVariable String filename) {
        try {
            Path rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = rootPath.resolve(filename).normalize();

            // Bảo vệ chống Path Traversal (CWE-22)
            if (!filePath.startsWith(rootPath)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Xác định Media Type tương ứng
            String contentType = null;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException ignored) {}

            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
