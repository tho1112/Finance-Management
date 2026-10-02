package com.example.demo.controller;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.CategoryRequest;
import com.example.demo.dto.CategoryResponse;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * 1. LẤY DANH SÁCH (Cả danh mục của user và danh mục hệ thống)
     */
    @GetMapping
    public ResponseEntity<?> getAllCategories(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .or(() -> userRepository.findByEmail(principal.getName()))
                .orElseThrow();

        List<Category> categories = categoryRepository.findByUserOrSystem(user);

        // Deduplicate theo tên danh mục (ưu tiên danh mục của user nếu trùng tên với hệ thống)
        Map<String, Category> uniqueCategoryMap = new java.util.LinkedHashMap<>();
        for (Category cat : categories) {
            if (cat.getName() == null) continue;
            String key = cat.getName().trim().toLowerCase();
            if (!uniqueCategoryMap.containsKey(key) || (cat.getUser() != null && uniqueCategoryMap.get(key).getUser() == null)) {
                uniqueCategoryMap.put(key, cat);
            }
        }

        List<CategoryResponse> responseList = uniqueCategoryMap.values().stream()
                .map(cat -> CategoryResponse.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .type(cat.getType())
                        .iconName(cat.getIconName())
                        .icon(cat.getIcon())
                        .colorCode(cat.getColorCode())
                        .jarType(cat.getJarType())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    /**
     * 2. TẠO MỚI DANH MỤC
     */
    @PostMapping
    public ResponseEntity<?> createCategory(
            @Valid @RequestBody CategoryRequest request,
            BindingResult result,
            Principal principal) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow();

            Category category = Category.builder()
                    .name(request.getName().trim())
                    .type(request.getType() != null ? request.getType() : "EXPENSE")
                    .iconName(request.getIconName())
                    .colorCode(request.getColorCode())
                    .jarType(request.getJarType())
                    .user(user)
                    .build();

            Category savedCategory = categoryRepository.save(category);

            CategoryResponse response = CategoryResponse.builder()
                    .id(savedCategory.getId())
                    .name(savedCategory.getName())
                    .type(savedCategory.getType())
                    .iconName(savedCategory.getIconName())
                    .icon(savedCategory.getIcon())
                    .colorCode(savedCategory.getColorCode())
                    .jarType(savedCategory.getJarType())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi tạo danh mục: " + e.getMessage()));
        }
    }

    /**
     * 3. XÓA DANH MỤC
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id, Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow();

            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

            if (category.getUser() == null) {
                return ResponseEntity.status(403).body(Map.of("message", "Không thể xóa danh mục mặc định của hệ thống!"));
            }

            if (!category.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền xóa danh mục của người khác!"));
            }

            categoryRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa danh mục thành công!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể xóa! Có thể danh mục này đang chứa giao dịch."));
        }
    }
}