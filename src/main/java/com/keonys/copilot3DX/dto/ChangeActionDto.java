package com.keonys.copilot3DX.dto;

public class ChangeActionDto {

    private boolean success;
    private int statusCode;
    private String searchCriteria;
    private String response;

    public ChangeActionDto() {
    }

    public ChangeActionDto(
            boolean success,
            int statusCode,
            String searchCriteria,
            String response) {

        this.success = success;
        this.statusCode = statusCode;
        this.searchCriteria = searchCriteria;
        this.response = response;
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

    public String getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}