package com.keonys.copilot3DX.service;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.keonys.copilot3DX.config.HttpRequestService;
import com.keonys.copilot3DX.dto.WorkPlanDto;
import com.keonys.copilot3DX.model.WorkPlanInfo;
import com.keonys.copilot3DX.model.WorkPlanReference;

@Service
public class WorkPlanService {

	private static final String WORKPLAN_TYPE = "DELLmiWorkPlanSystemReference";

	private static final String ITEM_SPECIFICATION_LOCATE_ENDPOINT = "/resources/v1/modeler/dsprcs/dsprcs:ItemSpecification/Locate";

	private static final String MANUFACTURING_ITEM_ENDPOINT = "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/";

	private static final String WORKPLAN_ENDPOINT = "/resources/v1/modeler/dsprcs/dsprcs:MfgProcess/";

	private static final String WORKPLAN_DETAILS_MASK = "dsprcs:MfgProcessMask.Details";

	@Value("${threedx.space-url}")
	private String space3dsUrlStr;

	@Value("${threedx.security-context}")
	private String secContext;

	private final Service3DXConnexion service3DXConnexion;
	private final HttpRequestService httpRequestService;

	public WorkPlanService(Service3DXConnexion service3DXConnexion, HttpRequestService httpRequestService) {

		this.service3DXConnexion = service3DXConnexion;
		this.httpRequestService = httpRequestService;
	}

	/**
	 * Retrieve all WorkPlans attached to a Manufacturing Item.
	 *
	 * STEP 1: POST /resources/v1/modeler/dsprcs/dsprcs:ItemSpecification/Locate
	 *
	 * STEP 2: Extract processRef objects with type: DELLmiWorkPlanSystemReference
	 *
	 * STEP 3: Retrieve WorkPlan details using:
	 *
	 * {relativePath}?$mask=dsprcs:MfgProcessMask.Details
	 */
	public WorkPlanDto getAttachedWorkPlans(String manufacturingItemPhysicalId) throws Exception {

		if (manufacturingItemPhysicalId == null || manufacturingItemPhysicalId.isBlank()) {

			throw new IllegalArgumentException("Manufacturing Item physical ID must not be null or empty.");
		}

		String effectivePhysicalId = manufacturingItemPhysicalId.trim();

		prepareConnection();

		Map<String, String> headers = createSecurityHeaders();

		/*
		 * STEP 1
		 *
		 * Locate WorkPlans attached to the Manufacturing Item.
		 */
		HttpResponse<String> locateResponse = locateWorkPlans(effectivePhysicalId, headers);

		/*
		 * STEP 2
		 *
		 * Extract WorkPlan references.
		 */
		List<WorkPlanReference> references = extractWorkPlanReferences(locateResponse.body());

		System.out.println("WorkPlans found for Manufacturing Item " + effectivePhysicalId + ": " + references.size());

		/*
		 * STEP 3
		 *
		 * Retrieve detailed information for every WorkPlan found.
		 */
		List<WorkPlanInfo> workPlans = new ArrayList<>();

		for (WorkPlanReference reference : references) {

			try {

				WorkPlanInfo workPlan = loadWorkPlanByRelativePath(reference.getRelativePath(), headers);

				if (workPlan != null) {
					workPlans.add(workPlan);
				}

			} catch (Exception e) {

				/*
				 * One inaccessible WorkPlan should not prevent the other WorkPlans from being
				 * returned.
				 */
				System.err.println("Unable to retrieve WorkPlan " + reference.getIdentifier() + ": " + e.getMessage());
			}
		}

		return new WorkPlanDto(effectivePhysicalId, workPlans);
	}

	/**
	 * Get one WorkPlan directly using its physical ID.
	 */
	public WorkPlanInfo getWorkPlanInfo(String workPlanPhysicalId) throws Exception {

		if (workPlanPhysicalId == null || workPlanPhysicalId.isBlank()) {

			throw new IllegalArgumentException("WorkPlan physical ID must not be null or empty.");
		}

		String effectivePhysicalId = workPlanPhysicalId.trim();

		prepareConnection();

		Map<String, String> headers = createSecurityHeaders();

		String encodedPhysicalId = URLEncoder.encode(effectivePhysicalId, StandardCharsets.UTF_8);

		String relativePath = WORKPLAN_ENDPOINT + encodedPhysicalId;

		WorkPlanInfo workPlan = loadWorkPlanByRelativePath(relativePath, headers);

		if (workPlan == null) {

			throw new RuntimeException("No WorkPlan found for physical ID: " + effectivePhysicalId);
		}

		return workPlan;
	}

	/**
	 * STEP 1
	 *
	 * Call:
	 *
	 * POST /resources/v1/modeler/dsprcs/ dsprcs:ItemSpecification/Locate
	 */
	private HttpResponse<String> locateWorkPlans(String manufacturingItemPhysicalId, Map<String, String> headers)
			throws Exception {

		String url = normalizeBaseUrl(space3dsUrlStr) + ITEM_SPECIFICATION_LOCATE_ENDPOINT;

		JSONObject requestBody = buildLocateRequestBody(manufacturingItemPhysicalId);

		System.out.println("WorkPlan Locate URL: " + url);

		System.out.println("WorkPlan Locate body: " + requestBody.toJSONString());

		/*
		 * Same pattern as the other 3DX services:
		 *
		 * loadUrl( method, contentType, body, url, headers)
		 */
		HttpResponse<String> response = httpRequestService.loadUrl("POST", MediaType.APPLICATION_JSON_VALUE,
				requestBody.toJSONString(), url, headers);

		if (response.statusCode() != 200) {

			throw new RuntimeException(
					"3DEXPERIENCE WorkPlan Locate failed. " + "ManufacturingItemId=" + manufacturingItemPhysicalId
							+ " Status=" + response.statusCode() + " Response=" + response.body());
		}

		return response;
	}

	/**
	 * Build body for ItemSpecification/Locate.
	 *
	 * { "referencedObjects": [ { "identifier": "...", "source": ".../enovia",
	 * "relativePath": "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/..." } ] }
	 */
	@SuppressWarnings("unchecked")
	private JSONObject buildLocateRequestBody(String manufacturingItemPhysicalId) {

		JSONObject referencedObject = new JSONObject();

		referencedObject.put("identifier", manufacturingItemPhysicalId);

		referencedObject.put("source", normalizeBaseUrl(space3dsUrlStr));

		referencedObject.put("relativePath", MANUFACTURING_ITEM_ENDPOINT + manufacturingItemPhysicalId);

		JSONArray referencedObjects = new JSONArray();

		referencedObjects.add(referencedObject);

		JSONObject requestBody = new JSONObject();

		requestBody.put("referencedObjects", referencedObjects);

		return requestBody;
	}

	/**
	 * Extract WorkPlan references returned by Locate.
	 *
	 * Path:
	 *
	 * member[] -> dsprcs:ItemSpecification -> member[] -> processRef
	 */
	private List<WorkPlanReference> extractWorkPlanReferences(String responseBody) throws Exception {

		JSONParser parser = new JSONParser();

		JSONObject rootJson = (JSONObject) parser.parse(responseBody);

		JSONArray rootMembers = getJsonArray(rootJson, "member");

		List<WorkPlanReference> result = new ArrayList<>();

		/*
		 * Used to avoid duplicated WorkPlans.
		 */
		Set<String> processedIdentifiers = new HashSet<>();

		if (rootMembers == null || rootMembers.isEmpty()) {

			return result;
		}

		for (Object rootMemberObject : rootMembers) {

			if (!(rootMemberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject rootMemberJson = (JSONObject) rootMemberObject;

			JSONObject itemSpecificationJson = getJsonObject(rootMemberJson, "dsprcs:ItemSpecification");

			if (itemSpecificationJson.isEmpty()) {
				continue;
			}

			JSONArray specificationMembers = getJsonArray(itemSpecificationJson, "member");

			if (specificationMembers == null || specificationMembers.isEmpty()) {

				continue;
			}

			for (Object specificationObject : specificationMembers) {

				if (!(specificationObject instanceof JSONObject)) {

					continue;
				}

				JSONObject specificationJson = (JSONObject) specificationObject;

				JSONObject processRefJson = getJsonObject(specificationJson, "processRef");

				if (processRefJson.isEmpty()) {
					continue;
				}

				String type = getString(processRefJson, "type");

				/*
				 * Only keep WorkPlan objects.
				 */
				if (!WORKPLAN_TYPE.equals(type)) {
					continue;
				}

				String identifier = getString(processRefJson, "identifier");

				String relativePath = getString(processRefJson, "relativePath");

				if (identifier.isBlank() || relativePath.isBlank()) {

					continue;
				}

				/*
				 * Ignore duplicated WorkPlans.
				 */
				if (!processedIdentifiers.add(identifier)) {

					continue;
				}

				WorkPlanReference reference = new WorkPlanReference();

				reference.setIdentifier(identifier);

				reference.setRelativePath(relativePath);

				reference.setType(type);

				result.add(reference);
			}
		}

		return result;
	}

	/**
	 * Load detailed information for one WorkPlan using its relativePath.
	 *
	 * GET
	 *
	 * {relativePath}?$mask=dsprcs:MfgProcessMask.Details
	 */
	private WorkPlanInfo loadWorkPlanByRelativePath(String relativePath, Map<String, String> headers) throws Exception {

		if (relativePath == null || relativePath.isBlank()) {

			throw new IllegalArgumentException("WorkPlan relativePath must not be null or empty.");
		}

		String normalizedRelativePath = normalizeRelativePath(relativePath);

		String url = normalizeBaseUrl(space3dsUrlStr) + normalizedRelativePath + "?$mask=" + WORKPLAN_DETAILS_MASK;

		System.out.println("Loading WorkPlan: " + url);

		HttpResponse<String> response = httpRequestService.loadUrl("GET", "", "", url, headers);

		if (response.statusCode() != 200) {

			throw new RuntimeException("WorkPlan retrieval failed. " + "RelativePath=" + normalizedRelativePath
					+ " Status=" + response.statusCode() + " Response=" + response.body());
		}

		return extractWorkPlan(response.body());
	}

	/**
	 * Parse detailed WorkPlan response.
	 *
	 * Expected:
	 *
	 * { "totalItems": 1, "member": [ { ... } ] }
	 */
	private WorkPlanInfo extractWorkPlan(String responseBody) throws Exception {

		JSONParser parser = new JSONParser();

		JSONObject rootJson = (JSONObject) parser.parse(responseBody);

		JSONArray members = getJsonArray(rootJson, "member");

		if (members == null || members.isEmpty()) {

			return null;
		}

		Object memberObject = members.get(0);

		if (!(memberObject instanceof JSONObject)) {
			return null;
		}

		JSONObject workPlanJson = (JSONObject) memberObject;

		String type = getString(workPlanJson, "type");

		/*
		 * Make sure that the returned object is actually a WorkPlan.
		 */
		if (!WORKPLAN_TYPE.equals(type)) {

			System.err.println("Object ignored because type is not " + WORKPLAN_TYPE + ". Returned type=" + type);

			return null;
		}

		WorkPlanInfo info = new WorkPlanInfo();

		info.setName(getString(workPlanJson, "name"));

		info.setTitle(getString(workPlanJson, "title"));

		info.setDescription(getString(workPlanJson, "description"));

		info.setId(getString(workPlanJson, "id"));

		info.setType(type);

		info.setModified(getString(workPlanJson, "modified"));

		info.setCreated(getString(workPlanJson, "created"));

		info.setRevision(getString(workPlanJson, "revision"));

		info.setState(getString(workPlanJson, "state"));

		info.setOwner(getString(workPlanJson, "owner"));

		info.setOrganization(getString(workPlanJson, "organization"));

		info.setCollabspace(getString(workPlanJson, "collabspace"));

		info.setReservedby(getString(workPlanJson, "reservedby"));

		info.setCreatedFrom(getString(workPlanJson, "createdFrom"));

		info.setVersionComment(getString(workPlanJson, "versionComment"));

		info.setSequencingMode(getString(workPlanJson, "sequencingMode"));

		info.setCapacity(getInteger(workPlanJson, "capacity"));

		info.setCycleTime(getDouble(workPlanJson, "cycleTime"));

		info.setFlowModeIN(getInteger(workPlanJson, "flowModeIN"));

		info.setFlowModeOUT(getInteger(workPlanJson, "flowModeOUT"));

		info.setMeanTimeBetweenFailure(getDouble(workPlanJson, "meanTimeBetweenFailure"));

		info.setMeanTimeToRepair(getDouble(workPlanJson, "meanTimeToRepair"));

		info.setOperationMode(getInteger(workPlanJson, "operationMode"));

		info.setQueuingModeIN(getInteger(workPlanJson, "queuingModeIN"));

		info.setQueuingModeOUT(getInteger(workPlanJson, "queuingModeOUT"));

		info.setTotalProductionTime(getDouble(workPlanJson, "totalProductionTime"));

		info.setCestamp(getString(workPlanJson, "cestamp"));

		return info;
	}

	/**
	 * Prepare 3DEXPERIENCE authentication.
	 */
	private void prepareConnection() throws Exception {

		service3DXConnexion.prepareAuthorizationHeaderValue();

		service3DXConnexion.getCsrfTokenValueBasic();
	}

	/**
	 * Build 3DEXPERIENCE headers.
	 *
	 * The SecurityContext is required by the WorkPlan APIs.
	 */
	private Map<String, String> createSecurityHeaders() {

		Map<String, String> headers = new HashMap<>();

		headers.put("SecurityContext", secContext);

		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

		return headers;
	}

	/**
	 * Safely get a JSONObject.
	 */
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

	/**
	 * Safely get a JSONArray.
	 */
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

	/**
	 * Safely get a String.
	 */
	private String getString(JSONObject json, String key) {

		if (json == null) {
			return "";
		}

		Object value = json.get(key);

		return value == null ? "" : value.toString();
	}

	/**
	 * Safely get an Integer.
	 */
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

	/**
	 * Safely get a Double.
	 */
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

	/**
	 * Normalize relativePath.
	 */
	private String normalizeRelativePath(String relativePath) {

		String normalizedPath = relativePath.trim();

		while (normalizedPath.startsWith("//")) {

			normalizedPath = normalizedPath.substring(1);
		}

		if (!normalizedPath.startsWith("/")) {

			normalizedPath = "/" + normalizedPath;
		}

		return normalizedPath;
	}

	/**
	 * Normalize 3DSpace base URL.
	 */
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