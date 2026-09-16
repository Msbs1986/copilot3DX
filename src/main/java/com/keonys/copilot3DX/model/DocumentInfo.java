package com.keonys.copilot3DX.model;

import java.util.ArrayList;
import java.util.List;

public class DocumentInfo {

    private String id;
    private String type;
    private String identifier;
    private String relativePath;
    private String name;
    private String title;
    private String revision;
    private String state;
    private String stateNLS;
    private String policy;
    private String description;
    private String collabSpace;
    private String collabSpaceTitle;
    private String owner;
    private String ownerFirstName;
    private String ownerLastName;
    private String originator;
    private String originatorFirstName;
    private String originatorLastName;
    private String fileExtension;
    private String originated;
    private String modified;
    private String isLatestRevision;
    private String hasDownloadAccess;
    private String hasReviseAccess;
    private String hasModifyAccess;
    private String hasDeleteAccess;
    private String reservedBy;
    private String codification;
    private List<DocumentFileInfo> files = new ArrayList<>();

    public DocumentInfo() {
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

    public String getStateNLS() {
        return stateNLS;
    }

    public void setStateNLS(String stateNLS) {
        this.stateNLS = stateNLS;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCollabSpace() {
        return collabSpace;
    }

    public void setCollabSpace(String collabSpace) {
        this.collabSpace = collabSpace;
    }

    public String getCollabSpaceTitle() {
        return collabSpaceTitle;
    }

    public void setCollabSpaceTitle(String collabSpaceTitle) {
        this.collabSpaceTitle = collabSpaceTitle;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwnerFirstName() {
        return ownerFirstName;
    }

    public void setOwnerFirstName(String ownerFirstName) {
        this.ownerFirstName = ownerFirstName;
    }

    public String getOwnerLastName() {
        return ownerLastName;
    }

    public void setOwnerLastName(String ownerLastName) {
        this.ownerLastName = ownerLastName;
    }

    public String getOriginator() {
        return originator;
    }

    public void setOriginator(String originator) {
        this.originator = originator;
    }

    public String getOriginatorFirstName() {
        return originatorFirstName;
    }

    public void setOriginatorFirstName(String originatorFirstName) {
        this.originatorFirstName = originatorFirstName;
    }

    public String getOriginatorLastName() {
        return originatorLastName;
    }

    public void setOriginatorLastName(String originatorLastName) {
        this.originatorLastName = originatorLastName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getOriginated() {
        return originated;
    }

    public void setOriginated(String originated) {
        this.originated = originated;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getIsLatestRevision() {
        return isLatestRevision;
    }

    public void setIsLatestRevision(String isLatestRevision) {
        this.isLatestRevision = isLatestRevision;
    }

    public String getHasDownloadAccess() {
        return hasDownloadAccess;
    }

    public void setHasDownloadAccess(String hasDownloadAccess) {
        this.hasDownloadAccess = hasDownloadAccess;
    }

    public String getHasReviseAccess() {
        return hasReviseAccess;
    }

    public void setHasReviseAccess(String hasReviseAccess) {
        this.hasReviseAccess = hasReviseAccess;
    }

    public String getHasModifyAccess() {
        return hasModifyAccess;
    }

    public void setHasModifyAccess(String hasModifyAccess) {
        this.hasModifyAccess = hasModifyAccess;
    }

    public String getHasDeleteAccess() {
        return hasDeleteAccess;
    }

    public void setHasDeleteAccess(String hasDeleteAccess) {
        this.hasDeleteAccess = hasDeleteAccess;
    }

    public String getReservedBy() {
        return reservedBy;
    }

    public void setReservedBy(String reservedBy) {
        this.reservedBy = reservedBy;
    }

    public String getCodification() {
        return codification;
    }

    public void setCodification(String codification) {
        this.codification = codification;
    }

    public List<DocumentFileInfo> getFiles() {
        return files;
    }

    public void setFiles(List<DocumentFileInfo> files) {
        this.files = files == null ? new ArrayList<>() : files;
    }
}