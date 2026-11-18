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
import com.keonys.copilot3DX.dto.ChangeActionDto;
import com.keonys.copilot3DX.model.ChangeActionInfo;
import com.keonys.copilot3DX.model.RealizedChangeInfo;

@Service
public class ChangeActionService {

	private static final String CHANGE_ACTION_SEARCH_ENDPOINT = "/resources/v1/modeler/dslc/changeaction/search";

	private static final String CHANGE_ACTION_ENDPOINT = "/resources/v1/modeler/dslc/changeaction/";

	@Value("${threedx.space-url}")
	private String space3dsUrlStr;

	@Value("${threedx.security-context}")
	private String secContext;

	private final Service3DXConnexion service3DXConnexion;
	private final HttpRequestService httpRequestService;

	public ChangeActionService(Service3DXConnexion service3DXConnexion, HttpRequestService httpRequestService) {

		this.service3DXConnexion = service3DXConnexion;
		this.httpRequestService = httpRequestService;
	}

	/**
	 * Search Change Actions and retrieve their complete information.
	 *
	 * Step 1: /resources/v1/modeler/dslc/changeaction/search?searchStr=*
	 *
	 * Step 2: For each returned identifier:
	 *
	 * /resources/v1/modeler/dslc/changeaction/{id}?$fields=realizedChanges
	 */
	public ChangeActionDto searchChangeActions(String searchStr) throws Exception {

		prepareConnection();

		Map<String, String> headers = createSecurityHeaders();

		/*
		 * "*" means all Change Actions.
		 */
		String effectiveSearchStr = searchStr == null || searchStr.isBlank() ? "*" : searchStr.trim();

		String encodedSearchStr = URLEncoder.encode(effectiveSearchStr, StandardCharsets.UTF_8);

		/*
		 * Build search URL.
		 */
		String searchUrl = normalizeBaseUrl(space3dsUrlStr) + CHANGE_ACTION_SEARCH_ENDPOINT + "?searchStr="
				+ encodedSearchStr;

		System.out.println("Change Action search URL: " + searchUrl);

		/*
		 * STEP 1
		 *
		 * Search Change Actions.
		 */
		HttpResponse<String> searchResponse = httpRequestService.loadUrl("GET", "", "", searchUrl, headers);

		if (searchResponse.statusCode() != 200) {

			throw new RuntimeException("3DEXPERIENCE Change Action search failed. Status=" + searchResponse.statusCode()
					+ " Response=" + searchResponse.body());
		}

		/*
		 * Extract physical IDs returned by the search.
		 */
		List<String> changeActionIds = extractChangeActionIds(searchResponse.body());

		System.out.println("Change Actions found: " + changeActionIds.size());

		/*
		 * STEP 2
		 *
		 * Retrieve complete information for every Change Action.
		 */
		List<ChangeActionInfo> changeActions = new ArrayList<>();

		for (String changeActionId : changeActionIds) {

			try {

				ChangeActionInfo changeAction = loadChangeAction(changeActionId, headers);

				if (changeAction != null) {

					changeActions.add(changeAction);
				}

			} catch (Exception e) {

				/*
				 * One invalid/inaccessible Change Action should not prevent the complete search
				 * result from being returned.
				 */
				System.err.println("Unable to retrieve Change Action " + changeActionId + ": " + e.getMessage());
			}
		}

		/*
		 * Return enriched Change Actions.
		 */
		return new ChangeActionDto(true, searchResponse.statusCode(), effectiveSearchStr, changeActions);
	}

	/**
	 * Get one Change Action using its physical ID.
	 */
	public ChangeActionDto getChangeAction(String changeActionId) throws Exception {

		if (changeActionId == null || changeActionId.isBlank()) {

			throw new IllegalArgumentException("Change Action ID must not be null or empty.");
		}

		String effectiveChangeActionId = changeActionId.trim();

		prepareConnection();

		Map<String, String> headers = createSecurityHeaders();

		ChangeActionInfo changeAction = loadChangeAction(effectiveChangeActionId, headers);

		List<ChangeActionInfo> result = new ArrayList<>();

		result.add(changeAction);

		return new ChangeActionDto(true, 200, effectiveChangeActionId, result);
	}

	/**
	 * Load one Change Action from 3DEXPERIENCE.
	 *
	 * This method is shared by:
	 *
	 * - getChangeAction() - searchChangeActions()
	 */
	private ChangeActionInfo loadChangeAction(String changeActionId, Map<String, String> headers) throws Exception {

		String encodedChangeActionId = URLEncoder.encode(changeActionId, StandardCharsets.UTF_8);

		String url = normalizeBaseUrl(space3dsUrlStr) + CHANGE_ACTION_ENDPOINT + encodedChangeActionId
				+ "?$fields=realizedChanges";

		System.out.println("Loading Change Action: " + changeActionId);

		HttpResponse<String> response = httpRequestService.loadUrl("GET", "", "", url, headers);

		if (response.statusCode() != 200) {

			throw new RuntimeException("Change Action retrieval failed. ID=" + changeActionId + " Status="
					+ response.statusCode() + " Response=" + response.body());
		}

		return extractChangeAction(response.body());
	}

	/**
	 * Extract physical IDs returned by:
	 *
	 * /dslc/changeaction/search
	 *
	 * Expected response:
	 *
	 * { "changeAction": [ { "identifier": "...", "relativePath": "...", "source":
	 * "...", "type": "Change Action" } ] }
	 */
	private List<String> extractChangeActionIds(String responseBody) throws Exception {

		JSONParser parser = new JSONParser();

		JSONObject searchJson = (JSONObject) parser.parse(responseBody);

		JSONArray changeActionArray = getJsonArray(searchJson, "changeAction");

		List<String> result = new ArrayList<>();

		if (changeActionArray == null || changeActionArray.isEmpty()) {

			return result;
		}

		for (Object object : changeActionArray) {

			if (!(object instanceof JSONObject)) {
				continue;
			}

			JSONObject changeActionJson = (JSONObject) object;

			String identifier = getString(changeActionJson, "identifier");

			if (!identifier.isBlank()) {

				result.add(identifier);
			}
		}

		return result;
	}

	/**
	 * Extract detailed Change Action information.
	 */
	private ChangeActionInfo extractChangeAction(String responseBody) throws Exception {

		JSONParser parser = new JSONParser();

		JSONObject json = (JSONObject) parser.parse(responseBody);

		ChangeActionInfo info = new ChangeActionInfo();

		/*
		 * Standard Change Action information.
		 */
		info.setId(getString(json, "id"));

		info.setName(getString(json, "name"));

		info.setTitle(getString(json, "title"));

		info.setState(getString(json, "state"));

		info.setOwner(getString(json, "owner"));

		info.setOriginator(getString(json, "originator"));

		info.setSeverity(getString(json, "severity"));

		info.setCollabSpace(getString(json, "collabSpace"));

		info.setDescription(getString(json, "description"));

		info.setCreationDate(getString(json, "Creation Date"));

		info.setModificationDate(getString(json, "Last Modification Date"));

		/*
		 * Realized Changes.
		 */
		info.setRealizedChanges(extractRealizedChanges(json));

		return info;
	}

	/**
	 * Extract realizedChanges.
	 */
	private List<RealizedChangeInfo> extractRealizedChanges(JSONObject json) {

		List<RealizedChangeInfo> result = new ArrayList<>();

		JSONArray realizedChanges = getJsonArray(json, "realizedChanges");

		if (realizedChanges == null) {
			return result;
		}

		for (Object object : realizedChanges) {

			if (!(object instanceof JSONObject)) {
				continue;
			}

			JSONObject realizedChangeJson = (JSONObject) object;

			JSONObject whereJson = getJsonObject(realizedChangeJson, "where");

			if (whereJson.isEmpty()) {
				continue;
			}

			RealizedChangeInfo realizedChange = new RealizedChangeInfo();

			realizedChange.setIdentifier(getString(whereJson, "identifier"));

			realizedChange.setRelativePath(getString(whereJson, "relativePath"));

			realizedChange.setSource(getString(whereJson, "source"));

			realizedChange.setType(getString(whereJson, "type"));

			/*
			 * Operations.
			 */
			JSONArray operationsJson = getJsonArray(realizedChangeJson, "operations");

			List<String> operations = new ArrayList<>();

			if (operationsJson != null) {

				for (Object operation : operationsJson) {

					if (operation != null) {

						operations.add(operation.toString());
					}
				}
			}

			realizedChange.setOperations(operations);

			result.add(realizedChange);
		}

		return result;
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

	private String normalizeBaseUrl(String baseUrl) {

		if (baseUrl == null || baseUrl.isBlank()) {

			throw new IllegalStateException("The property threedx.space-url is not configured.");
		}

		String normalizedUrl = baseUrl.trim();

		while (normalizedUrl.endsWith("/")) {

			normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
		}

		return normalizedUrl;
	}
}