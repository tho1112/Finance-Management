package com.example.demo.dto;

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
public class GroupRequest {

    @NotBlank(message = "Tên nhóm không được để trống!")
    @Size(max = 100, message = "Tên nhóm không được dài quá 100 ký tự!")
    private String name;
}
