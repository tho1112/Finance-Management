package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên danh mục không được để trống!")
    @Size(max = 100, message = "Tên danh mục không được dài quá 100 ký tự!")
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 50)
    @Column(length = 50)
    @Builder.Default
    private String type = "EXPENSE"; // EXPENSE, INCOME

    @Size(max = 50)
    @Column(name = "icon_name", length = 50)
    private String iconName;

    @Size(max = 20)
    @Column(name = "color_code", length = 20)
    private String colorCode;

    // Danh mục có thể thuộc về 1 User hoặc là danh mục hệ thống (user_id = null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Transaction> transactions;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Budget> budgets;

    // Giữ enum jarType tương thích nếu cần thiết cho logic phân bổ 6 lọ
    @Enumerated(EnumType.STRING)
    @Column(name = "jar_type", length = 50)
    private JarType jarType;

    // Helper constructor và getter/setter tương thích
    public Category(String name, String iconName, JarType jarType) {
        this.name = name;
        this.iconName = iconName;
        this.jarType = jarType;
        this.type = "EXPENSE";
    }

    public Category(String name, String iconName, String type, String colorCode) {
        this.name = name;
        this.iconName = iconName;
        this.type = type;
        this.colorCode = colorCode;
    }

    public String getIcon() {
        return this.iconName;
    }

    public void setIcon(String icon) {
        this.iconName = icon;
    }
}