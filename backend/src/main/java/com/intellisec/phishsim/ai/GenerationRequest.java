package com.intellisec.phishsim.ai;

import lombok.Data;

@Data
public class GenerationRequest {
    private String scenario;        // Ex: "Password expired", "Security alert", "Suspicious login"
    private String department;      // Ex: "IT", "RH", "Direction", "Comptabilité"
    private String language;        // "fr" ou "en"
    private String urgency;         // "low", "medium", "high"
    private String additionalDetails;
}