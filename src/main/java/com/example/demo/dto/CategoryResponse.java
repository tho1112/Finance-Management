package com.example.demo.dto;

import com.example.demo.model.JarType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String type;
    private String iconName;
    private String colorCode;

    // Giữ tương thích ngược
    private String icon;
    private JarType jarType;

    public String getIcon() {
        return icon != null ? icon : iconName;
    }
}