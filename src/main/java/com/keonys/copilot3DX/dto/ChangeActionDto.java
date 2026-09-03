package com.keonys.copilot3DX.dto;

import java.util.List;

public class ChangeActionDto {

	private boolean success;
	private int statusCode;
	private String searchCriteria;
	private List<ChangeActionInfo> response;
	
	public ChangeActionDto(boolean success, int statusCode, String searchCriteria, List<ChangeActionInfo> response) {

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

	public List<ChangeActionInfo> getResponse() {
		return response;
	}

	public void setResponse(List<ChangeActionInfo> response) {
		this.response = response;
	}
}