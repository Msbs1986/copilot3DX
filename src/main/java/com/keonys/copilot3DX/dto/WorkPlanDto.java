package com.keonys.copilot3DX.dto;

import java.util.ArrayList;
import java.util.List;

import com.keonys.copilot3DX.model.WorkPlanInfo;

public class WorkPlanDto {

	private boolean success;
	private int statusCode;
	private String searchCriteria;
	private int totalItems;
	private List<WorkPlanInfo> workPlans;

	public WorkPlanDto() {
		this.workPlans = new ArrayList<>();
	}

	public WorkPlanDto(boolean success, int statusCode, String searchCriteria, List<WorkPlanInfo> workPlans) {

		this.success = success;
		this.statusCode = statusCode;
		this.searchCriteria = searchCriteria;

		this.workPlans = workPlans != null ? workPlans : new ArrayList<>();

		this.totalItems = this.workPlans.size();
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

	public int getTotalItems() {
		return totalItems;
	}

	public void setTotalItems(int totalItems) {
		this.totalItems = totalItems;
	}

	public List<WorkPlanInfo> getWorkPlans() {
		return workPlans;
	}

	public void setWorkPlans(List<WorkPlanInfo> workPlans) {

		this.workPlans = workPlans != null ? workPlans : new ArrayList<>();

		this.totalItems = this.workPlans.size();
	}
}