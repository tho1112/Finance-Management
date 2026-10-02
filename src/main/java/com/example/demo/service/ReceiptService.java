package com.example.demo.service;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.ReceiptItemDto;
import com.example.demo.dto.ReceiptScanResponse;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RestTemplate restTemplate;

    @Value("${ocr.service.url:http://localhost:8001}")
    private String ocrServiceUrl;

    @Value("${file.upload-dir:uploads/receipts}")
    private String uploadDir;

    /**
     * Xử lý quét hoá đơn từ file ảnh người dùng tải lên
     */
    public ReceiptScanResponse processReceipt(MultipartFile file, String username, String customApiKey) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên không được rỗng!");
        }

        User user = null;
        if (username != null && !username.isBlank()) {
            user = userRepository.findByUsername(username)
                    .or(() -> userRepository.findByEmail(username))
                    .orElse(null);
        }

        // 1. Lưu file ảnh hoá đơn vào ổ đĩa
        String savedImageUrl = saveReceiptImage(file);

        // 2. Gửi ảnh sang OCR Microservice (Python FastAPI)
        Map<String, Object> ocrResult = callOcrMicroservice(file, customApiKey);

        // 3. Xử lý và chuẩn hoá dữ liệu trả về từ Microservice
        return buildScanResponse(ocrResult, user, savedImageUrl);
    }

    /**
     * Lưu ảnh vào thư mục upload trên server
     */
    private String saveReceiptImage(MultipartFile file) {
        try {
            Path rootPath = Paths.get(uploadDir);
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
            }

            String originalName = file.getOriginalFilename();
            String cleanName = (originalName != null) ? originalName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_") : "receipt.jpg";
            String uniqueFilename = UUID.randomUUID().toString().substring(0, 8) + "_" + cleanName;
            Path targetLocation = rootPath.resolve(uniqueFilename);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/api/receipts/images/" + uniqueFilename;
        } catch (IOException e) {
            log.error("Lỗi khi lưu ảnh hoá đơn: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Gọi Microservice OCR qua REST API
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callOcrMicroservice(MultipartFile file, String customApiKey) {
        String targetUrl = ocrServiceUrl.replaceAll("/+$", "") + "/api/v1/scan-receipt";
        log.info("Đang kết nối tới OCR Microservice tại: {}", targetUrl);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (customApiKey != null && !customApiKey.isBlank()) {
                headers.set("x-gemini-api-key", customApiKey.trim());
            }

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt.jpg";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(targetUrl, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
            throw new RuntimeException("Dịch vụ OCR không trả về dữ liệu hợp lệ!");
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.error("Lỗi từ OCR Microservice HTTP {}: {}", ex.getStatusCode(), responseBody);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"detail\"\\s*:\\s*\"([^\"]+)\"").matcher(responseBody);
            if (matcher.find()) {
                throw new IllegalArgumentException(matcher.group(1));
            }
            throw new IllegalArgumentException("Lỗi từ OCR Microservice: " + responseBody);
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            log.error("Không thể kết nối tới OCR Microservice tại {}: {}", targetUrl, e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối tới dịch vụ AI OCR (cổng 8001): " + e.getMessage());
        }
    }

    /**
     * So khớp danh mục phù hợp nhất cho người dùng
     */
    private Category findBestMatchingCategory(User user, String suggestedCategory, String suggestedJar) {
        List<Category> userCategories = categoryRepository.findByUserOrSystem(user);
        if (userCategories == null || userCategories.isEmpty()) {
            return null;
        }

        String targetName = suggestedCategory != null ? suggestedCategory.toLowerCase().trim() : "";

        // 1. Ưu tiên khớp theo tên danh mục chính xác hoặc chứa từ khoá
        for (Category cat : userCategories) {
            String catName = cat.getName().toLowerCase().trim();
            if (catName.equals(targetName) || catName.contains(targetName) || targetName.contains(catName)) {
                return cat;
            }
        }

        // 2. Nếu không khớp tên, khớp theo loại lọ JarType
        if (suggestedJar != null && !suggestedJar.isBlank()) {
            for (Category cat : userCategories) {
                if (cat.getJarType() != null && cat.getJarType().name().equalsIgnoreCase(suggestedJar)) {
                    return cat;
                }
            }
        }

        // 3. Trả về danh mục đầu tiên nếu không khớp
        return userCategories.get(0);
    }

    /**
     * Chuẩn hoá Response trả về cho Frontend
     */
    @SuppressWarnings("unchecked")
    private ReceiptScanResponse buildScanResponse(Map<String, Object> ocrResult, User user, String imageUrl) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (ocrResult.get("total_amount") != null) {
            try {
                totalAmount = new BigDecimal(ocrResult.get("total_amount").toString());
            } catch (Exception ignored) {}
        }

        String merchantName = (String) ocrResult.getOrDefault("merchant_name", "Hoá đơn mua sắm");
        String txDate = (String) ocrResult.get("transaction_date");
        if (txDate == null || txDate.isBlank()) {
            txDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } else if (txDate.length() > 16) {
            txDate = txDate.substring(0, 16);
        }

        String suggestedCategory = (String) ocrResult.getOrDefault("suggested_category", "Ăn uống");
        String suggestedJar = (String) ocrResult.getOrDefault("suggested_jar", "NECESSITIES");
        String note = (String) ocrResult.getOrDefault("note", "");
        String rawText = (String) ocrResult.getOrDefault("raw_text", "");
        String source = (String) ocrResult.getOrDefault("source", "ocr-service");

        Double confidence = 0.9;
        if (ocrResult.get("confidence") != null) {
            try {
                confidence = Double.parseDouble(ocrResult.get("confidence").toString());
            } catch (Exception ignored) {}
        }

        // Parse danh sách items
        List<ReceiptItemDto> items = new ArrayList<>();
        if (ocrResult.get("items") instanceof List) {
            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) ocrResult.get("items");
            for (Map<String, Object> itemMap : rawItems) {
                String name = (String) itemMap.getOrDefault("name", "Món");
                Double qty = 1.0;
                if (itemMap.get("quantity") != null) {
                    try { qty = Double.parseDouble(itemMap.get("quantity").toString()); } catch (Exception ignored) {}
                }
                BigDecimal unitPrice = null;
                if (itemMap.get("unit_price") != null) {
                    try { unitPrice = new BigDecimal(itemMap.get("unit_price").toString()); } catch (Exception ignored) {}
                }
                BigDecimal totalPrice = null;
                if (itemMap.get("total_price") != null) {
                    try { totalPrice = new BigDecimal(itemMap.get("total_price").toString()); } catch (Exception ignored) {}
                }

                items.add(ReceiptItemDto.builder()
                        .name(name)
                        .quantity(qty)
                        .unitPrice(unitPrice)
                        .totalPrice(totalPrice)
                        .build());
            }
        }

        // So khớp danh mục với DB người dùng
        Category matchedCat = findBestMatchingCategory(user, suggestedCategory, suggestedJar);

        String transactionType = (String) ocrResult.getOrDefault("transaction_type", "EXPENSE");

        return ReceiptScanResponse.builder()
                .success(true)
                .message("Trích xuất thông tin hoá đơn thành công!")
                .amount(totalAmount)
                .transactionDate(txDate)
                .merchantName(merchantName)
                .suggestedCategoryId(matchedCat != null ? matchedCat.getId() : null)
                .suggestedCategoryName(matchedCat != null ? matchedCat.getName() : suggestedCategory)
                .suggestedCategoryIcon(matchedCat != null ? matchedCat.getIcon() : "receipt_long")
                .suggestedJar(suggestedJar)
                .transactionType(transactionType)
                .note(note)
                .aiRawText(rawText)
                .receiptImageUrl(imageUrl)
                .items(items)
                .confidence(confidence)
                .source(source)
                .build();
    }

    /**
     * Bóc tách giao dịch từ câu nói hoặc văn bản tự nhiên (AI Voice & Text Assistant)
     */
    public ReceiptScanResponse processTextTransaction(String text, String username, String customApiKey) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Văn bản hoặc câu nói không được để trống!");
        }

        User user = null;
        if (username != null && !username.isBlank()) {
            user = userRepository.findByUsername(username)
                    .or(() -> userRepository.findByEmail(username))
                    .orElse(null);
        }

        List<Category> userCategories = categoryRepository.findByUserOrSystem(user);
        List<String> categoryNames = (userCategories != null)
                ? userCategories.stream().map(Category::getName).filter(Objects::nonNull).toList()
                : Collections.emptyList();

        Map<String, Object> parseResult = null;
        try {
            parseResult = callTextParseMicroservice(text.trim(), categoryNames, customApiKey);
        } catch (Exception ex) {
            log.warn("Microservice AI không khả dụng ({}), sử dụng Java Fallback Parser.", ex.getMessage());
            parseResult = parseTextWithJavaFallback(text.trim(), categoryNames);
        }

        if (parseResult == null) {
            parseResult = parseTextWithJavaFallback(text.trim(), categoryNames);
        }

        return buildTextScanResponse(parseResult, user, text.trim());
    }

    /**
     * Gọi Microservice AI Text Parser qua REST API
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callTextParseMicroservice(String text, List<String> categories, String customApiKey) {
        String targetUrl = ocrServiceUrl.replaceAll("/+$", "") + "/api/v1/parse-text";
        log.info("Gửi câu thoại/văn bản sang AI Microservice: {}", targetUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (customApiKey != null && !customApiKey.isBlank()) {
            headers.set("x-gemini-api-key", customApiKey.trim());
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", text);
        if (categories != null && !categories.isEmpty()) {
            requestBody.put("categories", categories);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(targetUrl, requestEntity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (Map<String, Object>) response.getBody();
        }

        throw new RuntimeException("AI Microservice trả về mã trạng thái " + response.getStatusCode());
    }

    /**
     * Bộ phân tích dự phòng Java thuần (Java NLP Rule Engine) độ trễ 1ms
     */
    private Map<String, Object> parseTextWithJavaFallback(String text, List<String> userCategories) {
        String lower = text.toLowerCase().trim();
        Map<String, Object> result = new HashMap<>();

        // 1. Phân tích số tiền
        BigDecimal amount = BigDecimal.ZERO;
        java.util.regex.Matcher compoundMatcher = java.util.regex.Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*(?:triệu|tr)\\s*(\\d+)(?:\\s*(?:trăm|k|nghìn|ngàn))?").matcher(lower);
        if (compoundMatcher.find()) {
            try {
                double millions = Double.parseDouble(compoundMatcher.group(1).replace(',', '.'));
                String fracStr = compoundMatcher.group(2);
                double frac = 0.0;
                if (fracStr.length() == 1) frac = Double.parseDouble(fracStr) * 100000;
                else if (fracStr.length() == 2) frac = Double.parseDouble(fracStr) * 10000;
                else if (fracStr.length() == 3) frac = Double.parseDouble(fracStr) * 1000;
                else frac = Double.parseDouble(fracStr);
                amount = BigDecimal.valueOf(millions * 1000000 + frac);
            } catch (Exception ignored) {}
        } else if (lower.contains("nửa triệu")) {
            amount = BigDecimal.valueOf(500000);
        } else if (lower.contains("vài chục")) {
            amount = BigDecimal.valueOf(50000);
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            java.util.regex.Matcher mTr = java.util.regex.Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*(?:triệu|tr)\\b").matcher(lower);
            if (mTr.find()) {
                try {
                    double val = Double.parseDouble(mTr.group(1).replace(',', '.'));
                    amount = BigDecimal.valueOf(val * 1000000);
                } catch (Exception ignored) {}
            }
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            java.util.regex.Matcher mK = java.util.regex.Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*(?:nghìn|ngàn|k)\\b").matcher(lower);
            if (mK.find()) {
                try {
                    double val = Double.parseDouble(mK.group(1).replace(',', '.'));
                    amount = BigDecimal.valueOf(val * 1000);
                } catch (Exception ignored) {}
            }
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            java.util.regex.Matcher mNum = java.util.regex.Pattern.compile("(\\d{1,3}(?:[\\.,]\\d{3})+|\\d{4,12})\\s*(?:đ|vnd|dong|đồng)?\\b").matcher(lower);
            if (mNum.find()) {
                try {
                    String clean = mNum.group(1).replaceAll("[^0-9]", "");
                    amount = new BigDecimal(clean);
                } catch (Exception ignored) {}
            }
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            java.util.regex.Matcher mSmall = java.util.regex.Pattern.compile("(?:hết|tốn|mất|cho|là)\\s*(\\d+)\\b").matcher(lower);
            if (mSmall.find()) {
                try {
                    long val = Long.parseLong(mSmall.group(1));
                    amount = BigDecimal.valueOf(val < 1000 ? val * 1000 : val);
                } catch (Exception ignored) {}
            }
        }

        // 2. Phân loại loại giao dịch
        boolean isIncome = lower.contains("lương") || lower.contains("thưởng") || lower.contains("thu nhập")
                || lower.contains("được cho") || lower.contains("lì xì") || lower.contains("tiền về")
                || lower.contains("bán được") || lower.contains("tiền lãi");
        String txType = isIncome ? "INCOME" : "EXPENSE";

        // 3. Phân loại danh mục
        String catName = "Khác";
        String jarName = "NECESSITIES";

        if (containsAny(lower, "trứng", "gà", "thịt", "bò", "cá", "cơm", "phở", "bún", "bánh", "mì", "lẩu", "nướng", "trà sữa", "cafe", "cà phê", "nước", "ăn sáng", "ăn trưa", "ăn tối", "nhà hàng", "quán")) {
            catName = "Ăn uống";
            jarName = "NECESSITIES";
        } else if (containsAny(lower, "xăng", "đổ xăng", "xe", "gửi xe", "rửa xe", "grab", "gojek", "be", "taxi", "vé xe", "xe buýt")) {
            catName = "Đi lại";
            jarName = "NECESSITIES";
        } else if (containsAny(lower, "quần", "áo", "giày", "dép", "mua sắm", "siêu thị", "chợ", "mỹ phẩm", "shopee", "lazada", "tiki", "túi xách")) {
            catName = "Mua sắm";
            jarName = "NECESSITIES";
        } else if (containsAny(lower, "học", "học phí", "sách", "vở", "khóa học", "khoá học", "tiếng anh", "ielts", "toeic", "bút")) {
            catName = "Giáo dục";
            jarName = "EDUCATION";
        } else if (containsAny(lower, "xem phim", "phim", "cgv", "netflix", "spotify", "game", "nạp game", "du lịch", "karaoke", "bar", "pub")) {
            catName = "Giải trí";
            jarName = "PLAY";
        } else if (containsAny(lower, "tiền điện", "tiền nước", "điện", "nước", "internet", "wifi", "tiền nhà", "tiền phòng", "tiền trọ", "gas", "rác")) {
            catName = "Hoá đơn";
            jarName = "NECESSITIES";
        } else if (containsAny(lower, "thuốc", "khám", "bệnh viện", "bác sĩ", "nha khoa", "răng", "hiệu thuốc", "long châu", "an khang")) {
            catName = "Sức khỏe";
            jarName = "NECESSITIES";
        } else if (isIncome) {
            catName = "Thu nhập";
            jarName = "FINANCIAL_FREE";
        }

        // 4. Trích xuất ghi chú
        String note = lower.replaceAll("(?:hết|tốn|mất|khoảng|tầm|khoản|được|giá|cho)?\\s*\\d+[\\.,]?\\d*\\s*(?:triệu|tr|nghìn|ngàn|k|đ|vnd|dong|đồng)?.*$", "").trim();
        note = note.replaceAll("^(?:sáng nay|chiều nay|tối qua|hôm qua|hôm nay|vừa|mới|mình|tôi|em|anh|chị)\\s*", "").trim();
        if (note.isEmpty()) {
            note = (isIncome ? "Khoản thu " : "Chi tiêu ") + catName.toLowerCase();
        } else {
            note = Character.toUpperCase(note.charAt(0)) + note.substring(1);
        }

        result.put("amount", amount);
        result.put("transaction_type", txType);
        result.put("suggested_category", catName);
        result.put("suggested_jar", jarName);
        result.put("note", note);
        result.put("confidence", 0.95);
        result.put("source", "java-fallback-engine");
        result.put("raw_text", text);
        result.put("transaction_date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));

        return result;
    }

    private boolean containsAny(String input, String... keywords) {
        for (String kw : keywords) {
            String pattern = "(?<!\\p{L})" + java.util.regex.Pattern.quote(kw.toLowerCase()) + "(?!\\p{L})";
            if (java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CHARACTER_CLASS).matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Chuẩn hoá Response trả về cho giao dịch văn bản/giọng nói
     */
    private ReceiptScanResponse buildTextScanResponse(Map<String, Object> result, User user, String originalText) {
        BigDecimal amount = BigDecimal.ZERO;
        if (result.get("amount") != null) {
            try {
                amount = new BigDecimal(result.get("amount").toString());
            } catch (Exception ignored) {}
        }

        String txType = (String) result.getOrDefault("transaction_type", "EXPENSE");
        String suggestedCat = (String) result.getOrDefault("suggested_category", "Khác");
        String suggestedJar = (String) result.getOrDefault("suggested_jar", "NECESSITIES");
        String note = (String) result.getOrDefault("note", originalText);
        String txDate = (String) result.get("transaction_date");
        if (txDate == null || txDate.isBlank()) {
            txDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } else if (txDate.length() > 16) {
            txDate = txDate.substring(0, 16);
        }

        Category matchedCat = findBestMatchingCategory(user, suggestedCat, suggestedJar);

        Double confidence = 0.95;
        if (result.get("confidence") != null) {
            try {
                confidence = Double.parseDouble(result.get("confidence").toString());
            } catch (Exception ignored) {}
        }

        return ReceiptScanResponse.builder()
                .success(true)
                .message("Phân tích câu giao dịch bằng AI thành công!")
                .amount(amount)
                .transactionDate(txDate)
                .merchantName((String) result.get("merchant_name"))
                .suggestedCategoryId(matchedCat != null ? matchedCat.getId() : null)
                .suggestedCategoryName(matchedCat != null ? matchedCat.getName() : suggestedCat)
                .suggestedCategoryIcon(matchedCat != null ? matchedCat.getIcon() : "payments")
                .suggestedJar(suggestedJar)
                .transactionType(txType)
                .note(note)
                .aiRawText(originalText)
                .confidence(confidence)
                .source((String) result.getOrDefault("source", "ai-voice-text-engine"))
                .build();
    }
}
