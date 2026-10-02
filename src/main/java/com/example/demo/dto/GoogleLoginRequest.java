package com.example.demo.dto;

public class GoogleLoginRequest {
    private String credential; // Đây chính là ID Token do Google trả về cho React

    // Getter và Setter
    public String getCredential() { return credential; }
    public void setCredential(String credential) { this.credential = credential; }
}