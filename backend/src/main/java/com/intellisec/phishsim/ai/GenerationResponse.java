package com.intellisec.phishsim.ai;

import lombok.Data;

@Data
public class GenerationResponse {
    private String id;
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private String senderName;
    private String senderDomain;
    private String landingPageHtml;
    private String redFlags;          // JSON string
    private String language;
    private String scenario;
    private String status;
    private boolean approved;
    private String generatedAt;
    private String approvedBy;
}