package com.keonys.copilot3DX.model;

public class WorkPlanInfo {

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
	private String reservedby;
	private String createdFrom;
	private String versionComment;
	private String sequencingMode;

	private Integer capacity;
	private Double cycleTime;
	private Integer flowModeIN;
	private Integer flowModeOUT;
	private Double meanTimeBetweenFailure;
	private Double meanTimeToRepair;
	private Integer operationMode;
	private Integer queuingModeIN;
	private Integer queuingModeOUT;
	private Double totalProductionTime;

	private String cestamp;

	public WorkPlanInfo() {
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

	public String getReservedby() {
		return reservedby;
	}

	public void setReservedby(String reservedby) {
		this.reservedby = reservedby;
	}

	public String getCreatedFrom() {
		return createdFrom;
	}

	public void setCreatedFrom(String createdFrom) {
		this.createdFrom = createdFrom;
	}

	public String getVersionComment() {
		return versionComment;
	}

	public void setVersionComment(String versionComment) {
		this.versionComment = versionComment;
	}

	public String getSequencingMode() {
		return sequencingMode;
	}

	public void setSequencingMode(String sequencingMode) {
		this.sequencingMode = sequencingMode;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public void setCapacity(Integer capacity) {
		this.capacity = capacity;
	}

	public Double getCycleTime() {
		return cycleTime;
	}

	public void setCycleTime(Double cycleTime) {
		this.cycleTime = cycleTime;
	}

	public Integer getFlowModeIN() {
		return flowModeIN;
	}

	public void setFlowModeIN(Integer flowModeIN) {
		this.flowModeIN = flowModeIN;
	}

	public Integer getFlowModeOUT() {
		return flowModeOUT;
	}

	public void setFlowModeOUT(Integer flowModeOUT) {
		this.flowModeOUT = flowModeOUT;
	}

	public Double getMeanTimeBetweenFailure() {
		return meanTimeBetweenFailure;
	}

	public void setMeanTimeBetweenFailure(Double meanTimeBetweenFailure) {
		this.meanTimeBetweenFailure = meanTimeBetweenFailure;
	}

	public Double getMeanTimeToRepair() {
		return meanTimeToRepair;
	}

	public void setMeanTimeToRepair(Double meanTimeToRepair) {
		this.meanTimeToRepair = meanTimeToRepair;
	}

	public Integer getOperationMode() {
		return operationMode;
	}

	public void setOperationMode(Integer operationMode) {
		this.operationMode = operationMode;
	}

	public Integer getQueuingModeIN() {
		return queuingModeIN;
	}

	public void setQueuingModeIN(Integer queuingModeIN) {
		this.queuingModeIN = queuingModeIN;
	}

	public Integer getQueuingModeOUT() {
		return queuingModeOUT;
	}

	public void setQueuingModeOUT(Integer queuingModeOUT) {
		this.queuingModeOUT = queuingModeOUT;
	}

	public Double getTotalProductionTime() {
		return totalProductionTime;
	}

	public void setTotalProductionTime(Double totalProductionTime) {
		this.totalProductionTime = totalProductionTime;
	}

	public String getCestamp() {
		return cestamp;
	}

	public void setCestamp(String cestamp) {
		this.cestamp = cestamp;
	}
}