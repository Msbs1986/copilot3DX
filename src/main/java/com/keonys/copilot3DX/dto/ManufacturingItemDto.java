package com.keonys.copilot3DX.dto;

import java.util.List;

import com.keonys.copilot3DX.model.ManufacturingItemInfo;

public class ManufacturingItemDto {

	private boolean success;
	private int status;
	private String searchStr;
	private int count;
	private List<ManufacturingItemInfo> manufacturingItems;

	public ManufacturingItemDto() {
	}

	public ManufacturingItemDto(boolean success, int status, String searchStr,
			List<ManufacturingItemInfo> manufacturingItems) {

		this.success = success;
		this.status = status;
		this.searchStr = searchStr;
		this.manufacturingItems = manufacturingItems;

		this.count = manufacturingItems == null ? 0 : manufacturingItems.size();
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getSearchStr() {
		return searchStr;
	}

	public void setSearchStr(String searchStr) {
		this.searchStr = searchStr;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public List<ManufacturingItemInfo> getManufacturingItems() {

		return manufacturingItems;
	}

	public void setManufacturingItems(List<ManufacturingItemInfo> manufacturingItems) {

		this.manufacturingItems = manufacturingItems;

		this.count = manufacturingItems == null ? 0 : manufacturingItems.size();
	}
}