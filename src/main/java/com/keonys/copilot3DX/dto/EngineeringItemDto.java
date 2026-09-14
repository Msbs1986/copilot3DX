package com.keonys.copilot3DX.dto;

import java.util.List;

import com.keonys.copilot3DX.model.EngineeringItemInfo;

public class EngineeringItemDto {

	private boolean success;
	private int statusCode;
	private String searchCriteria;
	private List<EngineeringItemInfo> engineeringItems;

	public EngineeringItemDto() {
	}

	public EngineeringItemDto(boolean success, int statusCode, String searchCriteria,
			List<EngineeringItemInfo> engineeringItems) {

		this.success = success;
		this.statusCode = statusCode;
		this.searchCriteria = searchCriteria;
		this.engineeringItems = engineeringItems;
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

	public List<EngineeringItemInfo> getEngineeringItems() {
		return engineeringItems;
	}

	public void setEngineeringItems(List<EngineeringItemInfo> engineeringItems) {
		this.engineeringItems = engineeringItems;
	}
}