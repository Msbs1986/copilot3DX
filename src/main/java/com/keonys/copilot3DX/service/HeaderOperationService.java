package com.keonys.copilot3DX.service;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.keonys.copilot3DX.config.HttpRequestService;
import com.keonys.copilot3DX.model.PrimaryResourceInfo;
import com.keonys.copilot3DX.model.ResourceInfo;
import com.keonys.copilot3DX.model.SecondaryResourceInfo;

@Service
public class HeaderOperationService {

	private static final String HEADER_OPERATION_ENDPOINT = "/resources/v1/modeler/dsprcs/dsprcs:MfgOperation/";

	private static final String PRIMARY_CAPABLE_RESOURCE_PATH = "/dsprcs:PrimaryCapableResource";

	private static final String SECONDARY_CAPABLE_RESOURCE_PATH = "/dsprcs:SecondaryCapableResource";

	private static final String RESOURCE_ENDPOINT = "/resources/v1/modeler/dsrsc/dsrsc:Resource/";

	@Value("${threedx.space-url}")
	private String space3dsUrlStr;

	@Value("${threedx.security-context}")
	private String secContext;

	private final Service3DXConnexion service3DXConnexion;
	private final HttpRequestService httpRequestService;

	public HeaderOperationService(Service3DXConnexion service3DXConnexion, HttpRequestService httpRequestService) {

		this.service3DXConnexion = service3DXConnexion;
		this.httpRequestService = httpRequestService;
	}

	/**
	 * Get all Primary Capable Resources attached to one Header Operation.
	 *
	 * Processing: 1. Call dsprcs:PrimaryCapableResource. 2. Extract the resource
	 * identifier, type and quantity. 3. Retrieve the detailed information for every
	 * Resource. 4. Return the enriched Primary Resources.
	 */
	public List<PrimaryResourceInfo> getPrimaryResources(String headerOperationPhysicalId) throws Exception {

		validatePhysicalId(headerOperationPhysicalId, "Header Operation");

		String effectivePhysicalId = headerOperationPhysicalId.trim();

		prepareConnection();

		Map<String, String> headers = createSecurityHeaders();

		HttpResponse<String> response = loadCapableResources(effectivePhysicalId, PRIMARY_CAPABLE_RESOURCE_PATH,
				"Primary", headers);

		List<PrimaryResourceInfo> primaryResources = extractPrimaryResources(response.body());

		System.out.println("Primary Capable Resources found for Header Operation " + effectivePhysicalId + ": "
				+ primaryResources.size());

		for (PrimaryResourceInfo primaryResource : primaryResources) {

			String resourceIdentifier = primaryResource.getResourceIdentifier();

			if (resourceIdentifier == null || resourceIdentifier.isBlank()) {

				System.err.println("Primary Resource details skipped because " + "resource.identifier is empty.");

				continue;
			}

			try {

				JSONObject resourceJson = loadResourceDetails(resourceIdentifier, headers);

				mapPrimaryResourceDetails(resourceJson, primaryResource);

			} catch (Exception e) {

				/*
				 * Keep the relation information even when the detailed Resource retrieval
				 * fails.
				 */
				System.err.println("Unable to retrieve details for Primary Resource " + resourceIdentifier + ": "
						+ e.getMessage());
			}
		}

		return primaryResources;
	}

	/**
	 * Get all Secondary Capable Resources attached to one Header Operation.
	 *
	 * Processing: 1. Call dsprcs:SecondaryCapableResource. 2. Extract the resource
	 * and dependsOn information. 3. Retrieve the detailed information for every
	 * Resource. 4. Return the enriched Secondary Resources.
	 */
	public List<SecondaryResourceInfo> getSecondaryResources(String headerOperationPhysicalId) throws Exception {

		validatePhysicalId(headerOperationPhysicalId, "Header Operation");

		String effectivePhysicalId = headerOperationPhysicalId.trim();

		prepareConnection();

		Map<String, String> headers = createSecurityHeaders();

		HttpResponse<String> response = loadCapableResources(effectivePhysicalId, SECONDARY_CAPABLE_RESOURCE_PATH,
				"Secondary", headers);

		List<SecondaryResourceInfo> secondaryResources = extractSecondaryResources(response.body());

		System.out.println("Secondary Capable Resources found for Header Operation " + effectivePhysicalId + ": "
				+ secondaryResources.size());

		for (SecondaryResourceInfo secondaryResource : secondaryResources) {

			String resourceIdentifier = secondaryResource.getResourceIdentifier();

			if (resourceIdentifier == null || resourceIdentifier.isBlank()) {

				System.err.println("Secondary Resource details skipped because " + "resource.identifier is empty.");

				continue;
			}

			try {

				JSONObject resourceJson = loadResourceDetails(resourceIdentifier, headers);

				mapSecondaryResourceDetails(resourceJson, secondaryResource);

			} catch (Exception e) {

				/*
				 * Keep the relation information even when the detailed Resource retrieval
				 * fails.
				 */
				System.err.println("Unable to retrieve details for Secondary Resource " + resourceIdentifier + ": "
						+ e.getMessage());
			}
		}

		return secondaryResources;
	}

	/**
	 * Public method used by the REST controller and MCP tool to retrieve detailed
	 * information for one Resource.
	 *
	 * Endpoint: GET /resources/v1/modeler/dsrsc/dsrsc:Resource/{physicalId}
	 */
	public ResourceInfo getResourceDetails(String resourcePhysicalId) throws Exception {

		validatePhysicalId(resourcePhysicalId, "Resource");

		String effectivePhysicalId = resourcePhysicalId.trim();

		prepareConnection();

		Map<String, String> headers = createSecurityHeaders();

		JSONObject resourceJson = loadResourceDetails(effectivePhysicalId, headers);

		return mapResourceInfo(resourceJson);
	}

	/**
	 * Call PrimaryCapableResource or SecondaryCapableResource for one Header
	 * Operation.
	 */
	private HttpResponse<String> loadCapableResources(String headerOperationPhysicalId, String capableResourcePath,
			String resourceCategory, Map<String, String> headers) throws Exception {

		String encodedPhysicalId = URLEncoder.encode(headerOperationPhysicalId, StandardCharsets.UTF_8);

		String url = normalizeBaseUrl(space3dsUrlStr) + HEADER_OPERATION_ENDPOINT + encodedPhysicalId
				+ capableResourcePath;

		System.out.println("Header Operation " + resourceCategory + " Capable Resources URL: " + url);

		HttpResponse<String> response = httpRequestService.loadUrl("GET", "", "", url, headers);

		if (response.statusCode() != 200) {

			throw new RuntimeException("3DEXPERIENCE " + resourceCategory + " Capable Resource retrieval failed. "
					+ "HeaderOperationId=" + headerOperationPhysicalId + " Status=" + response.statusCode()
					+ " Response=" + response.body());
		}

		return response;
	}

	/**
	 * Retrieve the detailed information for one Resource.
	 *
	 * This private method is shared by: - getResourceDetails; -
	 * getPrimaryResources; - getSecondaryResources.
	 */
	private JSONObject loadResourceDetails(String resourceIdentifier, Map<String, String> headers) throws Exception {

		validatePhysicalId(resourceIdentifier, "Resource");

		String effectiveIdentifier = resourceIdentifier.trim();

		String encodedIdentifier = URLEncoder.encode(effectiveIdentifier, StandardCharsets.UTF_8);

		String url = normalizeBaseUrl(space3dsUrlStr) + RESOURCE_ENDPOINT + encodedIdentifier;

		System.out.println("Loading Resource details: " + url);

		HttpResponse<String> response = httpRequestService.loadUrl("GET", "", "", url, headers);

		if (response.statusCode() != 200) {

			throw new RuntimeException("3DEXPERIENCE Resource retrieval failed. " + "ResourceId=" + effectiveIdentifier
					+ " Status=" + response.statusCode() + " Response=" + response.body());
		}

		JSONObject rootJson = parseJsonObject(response.body());

		JSONArray members = getJsonArray(rootJson, "member");

		if (members == null || members.isEmpty()) {

			throw new RuntimeException("No Resource details returned for physical ID: " + effectiveIdentifier);
		}

		Object memberObject = members.get(0);

		if (!(memberObject instanceof JSONObject)) {

			throw new RuntimeException("Invalid Resource response for physical ID: " + effectiveIdentifier);
		}

		return (JSONObject) memberObject;
	}

	/**
	 * Extract Primary Capable Resources from the 3DEXPERIENCE relation response.
	 */
	private List<PrimaryResourceInfo> extractPrimaryResources(String responseBody) throws Exception {

		JSONObject rootJson = parseJsonObject(responseBody);

		JSONArray members = getJsonArray(rootJson, "member");

		List<PrimaryResourceInfo> primaryResources = new ArrayList<>();

		if (members == null || members.isEmpty()) {
			return primaryResources;
		}

		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject memberJson = (JSONObject) memberObject;

			JSONObject resourceJson = getJsonObject(memberJson, "resource");

			if (resourceJson.isEmpty()) {

				System.err.println("Primary Capable Resource relation ignored " + "because resource is empty.");

				continue;
			}

			String resourceIdentifier = getString(resourceJson, "identifier");

			if (resourceIdentifier.isBlank()) {

				System.err.println(
						"Primary Capable Resource relation ignored " + "because resource.identifier is empty.");

				continue;
			}

			PrimaryResourceInfo primaryResource = new PrimaryResourceInfo();

			primaryResource.setRelationId(getString(memberJson, "id"));

			primaryResource.setResourceIdentifier(resourceIdentifier);

			primaryResource.setResourceRelativePath(getString(resourceJson, "relativePath"));

			primaryResource.setResourceType(getString(resourceJson, "type"));

			primaryResource.setResourcesQuantity(getDouble(memberJson, "resourcesQuantity"));

			primaryResource.setPreferred(getBoolean(memberJson, "isPreferred"));

			primaryResource.setManuallyScheduled(getBoolean(memberJson, "manuallyScheduled"));

			primaryResource.setPriority(getInteger(memberJson, "priority"));

			primaryResource.setToolChangeoverTime(getDouble(memberJson, "toolChangeoverTime"));

			primaryResource.setReservedFromFirstStep(getBoolean(memberJson, "isReservedFromFirstStep"));

			primaryResource.setCapableResourceGroupIndex(getInteger(memberJson, "capableRscGroupIndex"));

			primaryResources.add(primaryResource);
		}

		return primaryResources;
	}

	/**
	 * Extract Secondary Capable Resources from the 3DEXPERIENCE relation response.
	 */
	private List<SecondaryResourceInfo> extractSecondaryResources(String responseBody) throws Exception {

		JSONObject rootJson = parseJsonObject(responseBody);

		JSONArray members = getJsonArray(rootJson, "member");

		List<SecondaryResourceInfo> secondaryResources = new ArrayList<>();

		if (members == null || members.isEmpty()) {
			return secondaryResources;
		}

		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject memberJson = (JSONObject) memberObject;

			JSONObject resourceJson = getJsonObject(memberJson, "resource");

			if (resourceJson.isEmpty()) {

				System.err.println("Secondary Capable Resource relation ignored " + "because resource is empty.");

				continue;
			}

			String resourceIdentifier = getString(resourceJson, "identifier");

			if (resourceIdentifier.isBlank()) {

				System.err.println(
						"Secondary Capable Resource relation ignored " + "because resource.identifier is empty.");

				continue;
			}

			JSONObject dependsOnJson = getJsonObject(memberJson, "dependsOn");

			SecondaryResourceInfo secondaryResource = new SecondaryResourceInfo();

			secondaryResource.setRelationId(getString(memberJson, "id"));

			secondaryResource.setResourceIdentifier(resourceIdentifier);

			secondaryResource.setResourceRelativePath(getString(resourceJson, "relativePath"));

			secondaryResource.setResourceType(getString(resourceJson, "type"));

			secondaryResource.setDependsOnIdentifier(getString(dependsOnJson, "identifier"));

			secondaryResource.setDependsOnRelativePath(getString(dependsOnJson, "relativePath"));

			secondaryResource.setDependsOnType(getString(dependsOnJson, "type"));

			secondaryResource.setResourceTimeConstant(getDouble(memberJson, "rscTimeConst"));

			secondaryResource.setResourceTimePerQuantity(getDouble(memberJson, "rscTimePerQtyToProducedPerTime"));

			secondaryResource.setSymbolicId(getString(memberJson, "symbolicID"));

			secondaryResource.setRelationCestamp(getString(memberJson, "cestamp"));

			secondaryResources.add(secondaryResource);
		}

		return secondaryResources;
	}

	/**
	 * Map a detailed Resource JSON object to ResourceInfo.
	 */
	private ResourceInfo mapResourceInfo(JSONObject resourceJson) {

		ResourceInfo resourceInfo = new ResourceInfo();

		resourceInfo.setName(getString(resourceJson, "name"));

		resourceInfo.setTitle(getString(resourceJson, "title"));

		resourceInfo.setDescription(getString(resourceJson, "description"));

		resourceInfo.setId(getString(resourceJson, "id"));

		resourceInfo.setType(getString(resourceJson, "type"));

		resourceInfo.setModified(getString(resourceJson, "modified"));

		resourceInfo.setCreated(getString(resourceJson, "created"));

		resourceInfo.setRevision(getString(resourceJson, "revision"));

		resourceInfo.setState(getString(resourceJson, "state"));

		resourceInfo.setOwner(getString(resourceJson, "owner"));

		resourceInfo.setOrganization(getString(resourceJson, "organization"));

		resourceInfo.setCollabspace(getString(resourceJson, "collabspace"));

		resourceInfo.setAsset(getBoolean(resourceJson, "isAsset"));

		resourceInfo.setResourceType(getString(resourceJson, "resourceType"));

		resourceInfo.setCestamp(getString(resourceJson, "cestamp"));

		return resourceInfo;
	}

	/**
	 * Add detailed Resource information to PrimaryResourceInfo.
	 */
	private void mapPrimaryResourceDetails(JSONObject resourceJson, PrimaryResourceInfo primaryResource) {

		primaryResource.setName(getString(resourceJson, "name"));

		primaryResource.setTitle(getString(resourceJson, "title"));

		primaryResource.setDescription(getString(resourceJson, "description"));

		primaryResource.setId(getString(resourceJson, "id"));

		primaryResource.setType(getString(resourceJson, "type"));

		primaryResource.setModified(getString(resourceJson, "modified"));

		primaryResource.setCreated(getString(resourceJson, "created"));

		primaryResource.setRevision(getString(resourceJson, "revision"));

		primaryResource.setState(getString(resourceJson, "state"));

		primaryResource.setOwner(getString(resourceJson, "owner"));

		primaryResource.setOrganization(getString(resourceJson, "organization"));

		primaryResource.setCollabspace(getString(resourceJson, "collabspace"));

		primaryResource.setAsset(getBoolean(resourceJson, "isAsset"));

		primaryResource.setDetailedResourceType(getString(resourceJson, "resourceType"));

		primaryResource.setCestamp(getString(resourceJson, "cestamp"));
	}

	/**
	 * Add detailed Resource information to SecondaryResourceInfo.
	 */
	private void mapSecondaryResourceDetails(JSONObject resourceJson, SecondaryResourceInfo secondaryResource) {

		secondaryResource.setName(getString(resourceJson, "name"));

		secondaryResource.setTitle(getString(resourceJson, "title"));

		secondaryResource.setDescription(getString(resourceJson, "description"));

		secondaryResource.setId(getString(resourceJson, "id"));

		secondaryResource.setType(getString(resourceJson, "type"));

		secondaryResource.setModified(getString(resourceJson, "modified"));

		secondaryResource.setCreated(getString(resourceJson, "created"));

		secondaryResource.setRevision(getString(resourceJson, "revision"));

		secondaryResource.setState(getString(resourceJson, "state"));

		secondaryResource.setOwner(getString(resourceJson, "owner"));

		secondaryResource.setOrganization(getString(resourceJson, "organization"));

		secondaryResource.setCollabspace(getString(resourceJson, "collabspace"));

		secondaryResource.setAsset(getBoolean(resourceJson, "isAsset"));

		secondaryResource.setDetailedResourceType(getString(resourceJson, "resourceType"));

		secondaryResource.setCestamp(getString(resourceJson, "cestamp"));
	}

	/**
	 * Parse a JSON response and verify that its root is a JSON object.
	 */
	private JSONObject parseJsonObject(String responseBody) throws Exception {

		if (responseBody == null || responseBody.isBlank()) {

			throw new RuntimeException("3DEXPERIENCE returned an empty response body.");
		}

		JSONParser parser = new JSONParser();

		Object parsedResponse = parser.parse(responseBody);

		if (!(parsedResponse instanceof JSONObject)) {

			throw new RuntimeException("Invalid 3DEXPERIENCE response. " + "The JSON root is not an object.");
		}

		return (JSONObject) parsedResponse;
	}

	/**
	 * Prepare the 3DEXPERIENCE authentication.
	 */
	private void prepareConnection() throws Exception {

		service3DXConnexion.prepareAuthorizationHeaderValue();

		service3DXConnexion.getCsrfTokenValueBasic();
	}

	/**
	 * Build the HTTP headers required by 3DEXPERIENCE.
	 */
	private Map<String, String> createSecurityHeaders() {

		Map<String, String> headers = new HashMap<>();

		headers.put("SecurityContext", secContext);

		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

		return headers;
	}

	private JSONObject getJsonObject(JSONObject json, String key) {

		if (json == null) {
			return new JSONObject();
		}

		Object value = json.get(key);

		if (value instanceof JSONObject) {
			return (JSONObject) value;
		}

		return new JSONObject();
	}

	private JSONArray getJsonArray(JSONObject json, String key) {

		if (json == null) {
			return null;
		}

		Object value = json.get(key);

		if (value instanceof JSONArray) {
			return (JSONArray) value;
		}

		return null;
	}

	private String getString(JSONObject json, String key) {

		if (json == null) {
			return "";
		}

		Object value = json.get(key);

		return value == null ? "" : value.toString();
	}

	private Integer getInteger(JSONObject json, String key) {

		if (json == null) {
			return null;
		}

		Object value = json.get(key);

		if (value == null) {
			return null;
		}

		if (value instanceof Number) {

			return ((Number) value).intValue();
		}

		try {

			return Integer.valueOf(value.toString());

		} catch (NumberFormatException e) {

			return null;
		}
	}

	private Double getDouble(JSONObject json, String key) {

		if (json == null) {
			return null;
		}

		Object value = json.get(key);

		if (value == null) {
			return null;
		}

		if (value instanceof Number) {

			return ((Number) value).doubleValue();
		}

		try {

			return Double.valueOf(value.toString());

		} catch (NumberFormatException e) {

			return null;
		}
	}

	private Boolean getBoolean(JSONObject json, String key) {

		if (json == null) {
			return null;
		}

		Object value = json.get(key);

		if (value == null) {
			return null;
		}

		if (value instanceof Boolean) {
			return (Boolean) value;
		}

		String stringValue = value.toString().trim();

		if ("true".equalsIgnoreCase(stringValue)) {
			return Boolean.TRUE;
		}

		if ("false".equalsIgnoreCase(stringValue)) {
			return Boolean.FALSE;
		}

		return null;
	}

	private void validatePhysicalId(String physicalId, String objectType) {

		if (physicalId == null || physicalId.isBlank()) {

			throw new IllegalArgumentException(objectType + " physical ID must not be null or empty.");
		}
	}

	private String normalizeBaseUrl(String baseUrl) {

		if (baseUrl == null || baseUrl.isBlank()) {

			throw new IllegalStateException("The property threedx.space-url " + "is not configured.");
		}

		String normalizedUrl = baseUrl.trim();

		while (normalizedUrl.endsWith("/")) {

			normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
		}

		return normalizedUrl;
	}
}