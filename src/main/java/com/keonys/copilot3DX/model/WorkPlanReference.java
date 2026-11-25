package com.keonys.copilot3DX.model;

public class WorkPlanReference {

	private String identifier;
	private String relativePath;
	private String type;

	public WorkPlanReference() {
	}

	public WorkPlanReference(String identifier, String relativePath, String type) {

		this.identifier = identifier;
		this.relativePath = relativePath;
		this.type = type;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}