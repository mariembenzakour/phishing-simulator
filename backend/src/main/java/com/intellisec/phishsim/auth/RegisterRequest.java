package com.intellisec.phishsim.auth;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String role = "VIEWER";  // ✅ Par défaut VIEWER
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate birthDate;
}