package com.keonys.copilot3DX.dto;

public class ChangeActionDto {

	private boolean success;
	private int statusCode;
	private String searchCriteria;
	private Object response;

	public ChangeActionDto(boolean success, int statusCode, String searchCriteria, Object response) {

		this.success = success;
		this.statusCode = statusCode;
		this.searchCriteria = searchCriteria;
		this.response = response;
	}

	// getters/setters

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

	public Object getResponse() {
		return response;
	}

	public void setResponse(Object response) {
		this.response = response;
	}
}