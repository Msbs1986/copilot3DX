package com.keonys.copilot3DX.dto;

import com.keonys.copilot3DX.model.DocumentInfo;

public class DocumentDetailDto {

    private boolean success;
    private int statusCode;
    private String physicalId;
    private DocumentInfo document;

    public DocumentDetailDto() {
    }

    public DocumentDetailDto(
            boolean success,
            int statusCode,
            String physicalId,
            DocumentInfo document) {

        this.success = success;
        this.statusCode = statusCode;
        this.physicalId = physicalId;
        this.document = document;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getPhysicalId() {
        return physicalId;
    }

    public void setPhysicalId(String physicalId) {
        this.physicalId = physicalId;
    }

    public DocumentInfo getDocument() {
        return document;
    }

    public void setDocument(DocumentInfo document) {
        this.document = document;
    }
}