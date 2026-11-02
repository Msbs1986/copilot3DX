package com.keonys.copilot3DX.dto;

public class MaturityDto {

    private boolean success;
    private int statusCode;
    private String objectId;
    private String nextState;
    private String response;

    public MaturityDto(
            boolean success,
            int statusCode,
            String objectId,
            String nextState,
            String response) {

        this.success = success;
        this.statusCode = statusCode;
        this.objectId = objectId;
        this.nextState = nextState;
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

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getNextState() {
        return nextState;
    }

    public void setNextState(String nextState) {
        this.nextState = nextState;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}