package com.keonys.copilot3DX.dto;

import java.util.ArrayList;
import java.util.List;

import com.keonys.copilot3DX.model.DocumentInfo;

public class DocumentDto {

	private boolean success;
	private int statusCode;
	private String searchStr;
	private int items;
	private List<DocumentInfo> documents = new ArrayList<>();

	public DocumentDto() {
	}

	public DocumentDto(boolean success, int statusCode, String searchStr, List<DocumentInfo> documents) {

		this.success = success;
		this.statusCode = statusCode;
		this.searchStr = searchStr;
		setDocuments(documents);
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

	public String getSearchStr() {
		return searchStr;
	}

	public void setSearchStr(String searchStr) {
		this.searchStr = searchStr;
	}

	public int getItems() {
		return items;
	}

	public void setItems(int items) {
		this.items = items;
	}

	public List<DocumentInfo> getDocuments() {
		return documents;
	}

	public void setDocuments(List<DocumentInfo> documents) {
		this.documents = documents == null ? new ArrayList<>() : documents;

		this.items = this.documents.size();
	}
}
