package com.intellisec.phishsim.auth;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OperatorDTO {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;

    private LocalDate birthDate;
    private String role;

    // ✅ NOUVEAU : exposé au front pour afficher la photo de profil
    private String avatar;

    private LocalDateTime createdAt;
}