package com.keonys.copilot3DX.dto;

import java.util.ArrayList;
import java.util.List;

import com.keonys.copilot3DX.model.WorkPlanInfo;

public class WorkPlanDto {

	private String manufacturingItemPhysicalId;
	private int totalItems;
	private List<WorkPlanInfo> workPlans;

	public WorkPlanDto() {
		this.workPlans = new ArrayList<>();
	}

	public WorkPlanDto(String manufacturingItemPhysicalId, List<WorkPlanInfo> workPlans) {

		this.manufacturingItemPhysicalId = manufacturingItemPhysicalId;

		this.workPlans = workPlans != null ? workPlans : new ArrayList<>();

		this.totalItems = this.workPlans.size();
	}

	public String getManufacturingItemPhysicalId() {
		return manufacturingItemPhysicalId;
	}

	public void setManufacturingItemPhysicalId(String manufacturingItemPhysicalId) {

		this.manufacturingItemPhysicalId = manufacturingItemPhysicalId;
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