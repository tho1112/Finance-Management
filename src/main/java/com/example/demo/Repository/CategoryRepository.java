package com.example.demo.Repository;

import com.example.demo.model.Category;
import com.example.demo.model.JarType;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUser(User user);

    // Lấy cả danh mục do user tạo và danh mục chung của hệ thống (user IS NULL)
    @Query("SELECT c FROM Category c WHERE c.user = :user OR c.user IS NULL")
    List<Category> findByUserOrSystem(@Param("user") User user);

    List<Category> findByType(String type);

    List<Category> findByJarType(JarType jarType);

    boolean existsByName(String name);
}