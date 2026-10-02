package com.example.demo.dto;

import com.example.demo.model.JarType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống!")
    @Size(max = 100, message = "Tên danh mục không được quá 100 ký tự!")
    private String name;

    private String type; // EXPENSE, INCOME

    private String iconName;

    private String colorCode;

    // Giữ tương thích ngược
    private JarType jarType;
    private String icon;

    public String getIconName() {
        return iconName != null ? iconName : icon;
    }
}