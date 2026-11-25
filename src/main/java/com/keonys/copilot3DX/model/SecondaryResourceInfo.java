package com.keonys.copilot3DX.model;

public class SecondaryResourceInfo {

	/*
	 * Information returned by dsprcs:SecondaryCapableResource.
	 */
	private String relationId;
	private String resourceIdentifier;
	private String resourceRelativePath;
	private String resourceType;

	private String dependsOnIdentifier;
	private String dependsOnRelativePath;
	private String dependsOnType;

	private Double resourceTimeConstant;
	private Double resourceTimePerQuantity;
	private String symbolicId;
	private String relationCestamp;

	/*
	 * Detailed information returned by dsrsc:Resource/{identifier}.
	 */
	private String name;
	private String title;
	private String description;
	private String id;
	private String type;
	private String modified;
	private String created;
	private String revision;
	private String state;
	private String owner;
	private String organization;
	private String collabspace;
	private Boolean asset;
	private String detailedResourceType;
	private String cestamp;

	public SecondaryResourceInfo() {
	}

	public String getRelationId() {
		return relationId;
	}

	public void setRelationId(String relationId) {
		this.relationId = relationId;
	}

	public String getResourceIdentifier() {
		return resourceIdentifier;
	}

	public void setResourceIdentifier(String resourceIdentifier) {

		this.resourceIdentifier = resourceIdentifier;
	}

	public String getResourceRelativePath() {
		return resourceRelativePath;
	}

	public void setResourceRelativePath(String resourceRelativePath) {

		this.resourceRelativePath = resourceRelativePath;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getDependsOnIdentifier() {
		return dependsOnIdentifier;
	}

	public void setDependsOnIdentifier(String dependsOnIdentifier) {

		this.dependsOnIdentifier = dependsOnIdentifier;
	}

	public String getDependsOnRelativePath() {
		return dependsOnRelativePath;
	}

	public void setDependsOnRelativePath(String dependsOnRelativePath) {

		this.dependsOnRelativePath = dependsOnRelativePath;
	}

	public String getDependsOnType() {
		return dependsOnType;
	}

	public void setDependsOnType(String dependsOnType) {
		this.dependsOnType = dependsOnType;
	}

	public Double getResourceTimeConstant() {
		return resourceTimeConstant;
	}

	public void setResourceTimeConstant(Double resourceTimeConstant) {

		this.resourceTimeConstant = resourceTimeConstant;
	}

	public Double getResourceTimePerQuantity() {
		return resourceTimePerQuantity;
	}

	public void setResourceTimePerQuantity(Double resourceTimePerQuantity) {

		this.resourceTimePerQuantity = resourceTimePerQuantity;
	}

	public String getSymbolicId() {
		return symbolicId;
	}

	public void setSymbolicId(String symbolicId) {
		this.symbolicId = symbolicId;
	}

	public String getRelationCestamp() {
		return relationCestamp;
	}

	public void setRelationCestamp(String relationCestamp) {

		this.relationCestamp = relationCestamp;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getModified() {
		return modified;
	}

	public void setModified(String modified) {
		this.modified = modified;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getCollabspace() {
		return collabspace;
	}

	public void setCollabspace(String collabspace) {
		this.collabspace = collabspace;
	}

	public Boolean getAsset() {
		return asset;
	}

	public void setAsset(Boolean asset) {
		this.asset = asset;
	}

	public String getDetailedResourceType() {
		return detailedResourceType;
	}

	public void setDetailedResourceType(String detailedResourceType) {

		this.detailedResourceType = detailedResourceType;
	}

	public String getCestamp() {
		return cestamp;
	}

	public void setCestamp(String cestamp) {
		this.cestamp = cestamp;
	}
}