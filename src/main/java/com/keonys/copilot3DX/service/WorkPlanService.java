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

	private static final String HEADER_OPERATION_TYPE = "DELLmiHeaderOperationReference";

	private static final String WORKPLAN_SEARCH_ENDPOINT = "/resources/v1/modeler/dsprcs/dsprcs:MfgProcess/search";

	private static final String WORKPLAN_ENDPOINT = "/resources/v1/modeler/dsprcs/dsprcs:MfgProcess/";

	private static final String ITEM_SPECIFICATION_LOCATE_ENDPOINT = "/resources/v1/modeler/dsprcs/dsprcs:ItemSpecification/Locate";

	private static final String MANUFACTURING_ITEM_ENDPOINT = "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/";

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
	 * Search WorkPlans in 3DEXPERIENCE.
	 *
	 * If searchStr is null, empty or equal to "*", "Workplan*" is sent to
	 * 3DEXPERIENCE.
	 *
	 * This prevents MfgProcess/search from returning the first 50 objects of every
	 * MfgProcess type.
	 */
	public WorkPlanDto searchWorkPlans(String searchStr) throws Exception {

		prepareConnection();

		Map<String, String> headers = createSecurityHeaders();

		/*
		 * Criterion displayed in the response.
		 */
		String requestedSearchStr = searchStr == null || searchStr.isBlank() ? "*" : searchStr.trim();

		/*
		 * Criterion really sent to 3DEXPERIENCE.
		 *
		 * MfgProcess/search?$searchStr=* returns all types of MfgProcess and is limited
		 * to the first 50 results.
		 *
		 * Workplan* limits the search to objects whose name or title corresponds to a
		 * WorkPlan.
		 */
		String effective3DXSearchStr = "*".equals(requestedSearchStr) ? "Workplan*" : requestedSearchStr;

		String encodedSearchStr = URLEncoder.encode(effective3DXSearchStr, StandardCharsets.UTF_8);

		String url = normalizeBaseUrl(space3dsUrlStr) + WORKPLAN_SEARCH_ENDPOINT + "?$searchStr=" + encodedSearchStr;

		System.out.println("Requested WorkPlan search criterion: " + requestedSearchStr);

		System.out.println("Effective 3DX WorkPlan search criterion: " + effective3DXSearchStr);

		System.out.println("WorkPlan search URL: " + url);

		HttpResponse<String> response = httpRequestService.loadUrl("GET", "", "", url, headers);

		if (response.statusCode() != 200) {

			throw new RuntimeException("3DEXPERIENCE WorkPlan search failed. " + "SearchCriterion="
					+ effective3DXSearchStr + " Status=" + response.statusCode() + " Response=" + response.body());
		}

		List<WorkPlanInfo> workPlans = extractWorkPlansFromSearch(response.body());

		System.out.println("WorkPlans found after type filtering: " + workPlans.size());

		return new WorkPlanDto(true, response.statusCode(), requestedSearchStr, workPlans);
	}

	/**
	 * Get detailed information for one WorkPlan using its physical ID.
	 */
	public WorkPlanInfo getWorkPlanInfo(String workPlanPhysicalId) throws Exception {

		validatePhysicalId(workPlanPhysicalId, "WorkPlan");

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
	 * Expand one WorkPlan and return only its Header Operation references.
	 *
	 * The expand response may contain: - the WorkPlan reference; - Header Operation
	 * references; - Header Operation instances; - other Manufacturing Process
	 * objects.
	 *
	 * Only DELLmiHeaderOperationReference objects are returned.
	 */
	public List<WorkPlanInfo> getWorkPlanHeaderOperations(String workPlanPhysicalId) throws Exception {

		validatePhysicalId(workPlanPhysicalId, "WorkPlan");

		String effectivePhysicalId = workPlanPhysicalId.trim();

		prepareConnection();

		Map<String, String> headers = createSecurityHeaders();

		String encodedPhysicalId = URLEncoder.encode(effectivePhysicalId, StandardCharsets.UTF_8);

		String url = normalizeBaseUrl(space3dsUrlStr) + WORKPLAN_ENDPOINT + encodedPhysicalId + "/expand";

		JSONObject requestBody = buildWorkPlanExpandRequestBody();

		System.out.println("WorkPlan expand URL: " + url);

		System.out.println("WorkPlan expand body: " + requestBody.toJSONString());

		HttpResponse<String> response = httpRequestService.loadUrl("POST", MediaType.APPLICATION_JSON_VALUE,
				requestBody.toJSONString(), url, headers);

		if (response.statusCode() != 200) {

			throw new RuntimeException("3DEXPERIENCE WorkPlan expand failed. " + "WorkPlanId=" + effectivePhysicalId
					+ " Status=" + response.statusCode() + " Response=" + response.body());
		}

		List<WorkPlanInfo> headerOperations = extractHeaderOperations(response.body());

		System.out.println(
				"Header Operations found for WorkPlan " + effectivePhysicalId + ": " + headerOperations.size());

		return headerOperations;
	}

	/**
	 * Get all WorkPlans attached to a Manufacturing Item.
	 *
	 * Step 1: call ItemSpecification/Locate. Step 2: extract WorkPlan processRef
	 * objects. Step 3: load details for every WorkPlan.
	 */
	public WorkPlanDto getAttachedWorkPlans(String manufacturingItemPhysicalId) throws Exception {

		validatePhysicalId(manufacturingItemPhysicalId, "Manufacturing Item");

		String effectivePhysicalId = manufacturingItemPhysicalId.trim();

		prepareConnection();

		Map<String, String> headers = createSecurityHeaders();

		HttpResponse<String> locateResponse = locateWorkPlans(effectivePhysicalId, headers);

		List<WorkPlanReference> references = extractWorkPlanReferences(locateResponse.body());

		System.out.println("WorkPlans found for Manufacturing Item " + effectivePhysicalId + ": " + references.size());

		List<WorkPlanInfo> workPlans = new ArrayList<>();

		for (WorkPlanReference reference : references) {

			try {

				WorkPlanInfo workPlan = loadWorkPlanByRelativePath(reference.getRelativePath(), headers);

				if (workPlan != null) {
					workPlans.add(workPlan);
				}

			} catch (Exception e) {

				System.err.println("Unable to retrieve WorkPlan " + reference.getIdentifier() + ": " + e.getMessage());
			}
		}

		return new WorkPlanDto(true, locateResponse.statusCode(), effectivePhysicalId, workPlans);
	}

	/**
	 * Call ItemSpecification/Locate.
	 */
	private HttpResponse<String> locateWorkPlans(String manufacturingItemPhysicalId, Map<String, String> headers)
			throws Exception {

		String url = normalizeBaseUrl(space3dsUrlStr) + ITEM_SPECIFICATION_LOCATE_ENDPOINT;

		JSONObject requestBody = buildLocateRequestBody(manufacturingItemPhysicalId);

		System.out.println("WorkPlan Locate URL: " + url);

		System.out.println("WorkPlan Locate body: " + requestBody.toJSONString());

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
	 * Build the ItemSpecification/Locate body.
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
	 * Build the WorkPlan expand request body.
	 */
	@SuppressWarnings("unchecked")
	private JSONObject buildWorkPlanExpandRequestBody() {

		JSONObject requestBody = new JSONObject();

		requestBody.put("expandDepth", -1);

		requestBody.put("withPath", false);

		return requestBody;
	}

	/**
	 * Extract WorkPlans returned by MfgProcess/search.
	 */
	private List<WorkPlanInfo> extractWorkPlansFromSearch(String responseBody) throws Exception {

		JSONParser parser = new JSONParser();

		JSONObject rootJson = (JSONObject) parser.parse(responseBody);

		JSONArray members = getJsonArray(rootJson, "member");

		List<WorkPlanInfo> workPlans = new ArrayList<>();

		if (members == null || members.isEmpty()) {
			return workPlans;
		}

		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject workPlanJson = (JSONObject) memberObject;

			String type = getString(workPlanJson, "type");

			if (!WORKPLAN_TYPE.equals(type)) {
				continue;
			}

			workPlans.add(mapWorkPlanInfo(workPlanJson));
		}

		return workPlans;
	}

	/**
	 * Extract Header Operation references from a WorkPlan expand response.
	 *
	 * Header Operation instances are ignored.
	 */
	private List<WorkPlanInfo> extractHeaderOperations(String responseBody) throws Exception {

		JSONParser parser = new JSONParser();

		JSONObject rootJson = (JSONObject) parser.parse(responseBody);

		JSONArray members = getJsonArray(rootJson, "member");

		List<WorkPlanInfo> headerOperations = new ArrayList<>();

		if (members == null || members.isEmpty()) {
			return headerOperations;
		}

		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject memberJson = (JSONObject) memberObject;

			String type = getString(memberJson, "type");

			if (!HEADER_OPERATION_TYPE.equals(type)) {
				continue;
			}

			headerOperations.add(mapWorkPlanInfo(memberJson));
		}

		return headerOperations;
	}

	/**
	 * Extract WorkPlan references returned by ItemSpecification/Locate.
	 */
	private List<WorkPlanReference> extractWorkPlanReferences(String responseBody) throws Exception {

		JSONParser parser = new JSONParser();

		JSONObject rootJson = (JSONObject) parser.parse(responseBody);

		JSONArray rootMembers = getJsonArray(rootJson, "member");

		List<WorkPlanReference> references = new ArrayList<>();

		Set<String> processedIdentifiers = new HashSet<>();

		if (rootMembers == null || rootMembers.isEmpty()) {
			return references;
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

				if (!WORKPLAN_TYPE.equals(type)) {
					continue;
				}

				String identifier = getString(processRefJson, "identifier");

				String relativePath = getString(processRefJson, "relativePath");

				if (identifier.isBlank() || relativePath.isBlank()) {

					continue;
				}

				if (!processedIdentifiers.add(identifier)) {
					continue;
				}

				references.add(new WorkPlanReference(identifier, relativePath, type));
			}
		}

		return references;
	}

	/**
	 * Load details for one WorkPlan using its relativePath.
	 */
	private WorkPlanInfo loadWorkPlanByRelativePath(String relativePath, Map<String, String> headers) throws Exception {

		if (relativePath == null || relativePath.isBlank()) {

			throw new IllegalArgumentException("WorkPlan relativePath must not " + "be null or empty.");
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
	 * Extract one detailed WorkPlan.
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

		if (!WORKPLAN_TYPE.equals(type)) {

			System.err.println("Object ignored because its type is not " + WORKPLAN_TYPE + ". Returned type=" + type);

			return null;
		}

		return mapWorkPlanInfo(workPlanJson);
	}

	/**
	 * Map a Manufacturing Process JSON object to WorkPlanInfo.
	 *
	 * This method supports: - WorkPlan search results; - detailed WorkPlan
	 * responses; - Header Operation references returned by WorkPlan expand.
	 */
	private WorkPlanInfo mapWorkPlanInfo(JSONObject workPlanJson) {

		WorkPlanInfo info = new WorkPlanInfo();

		info.setName(getString(workPlanJson, "name"));

		info.setTitle(getString(workPlanJson, "title"));

		info.setDescription(getString(workPlanJson, "description"));

		info.setId(getString(workPlanJson, "id"));

		info.setType(getString(workPlanJson, "type"));

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
	 * Build required 3DEXPERIENCE headers.
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

	private void validatePhysicalId(String physicalId, String objectType) {

		if (physicalId == null || physicalId.isBlank()) {

			throw new IllegalArgumentException(objectType + " physical ID must not " + "be null or empty.");
		}
	}

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