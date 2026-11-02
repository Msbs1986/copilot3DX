package com.keonys.copilot3DX.service;

import java.net.http.HttpResponse;
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
	 * Get a Change Action and its realized changes from 3DEXPERIENCE.
	 *
	 * Example:
	 *
	 * /resources/v1/modeler/dslc/changeaction/{changeActionId}
	 * ?$fields=realizedChanges
	 *
	 */
	public ChangeActionDto getChangeAction(String changeActionId) throws Exception {

		if (changeActionId == null || changeActionId.isBlank()) {
			throw new IllegalArgumentException("Change Action ID must not be null or empty.");
		}

		String effectiveChangeActionId = changeActionId.trim();

		/*
		 * Prepare 3DEXPERIENCE authentication.
		 */
		service3DXConnexion.prepareAuthorizationHeaderValue();

		/*
		 * Kept because the existing POC may use this call to initialize or refresh
		 * authentication/CSRF information.
		 */
		service3DXConnexion.getCsrfTokenValueBasic();

		Map<String, String> headers = createSecurityHeaders();

		/*
		 * Build the Change Action URL.
		 */
		String url = space3dsUrlStr + CHANGE_ACTION_ENDPOINT + effectiveChangeActionId + "?$fields=realizedChanges";

		/*
		 * Call 3DEXPERIENCE.
		 */
		HttpResponse<String> response = httpRequestService.loadUrl("GET", "", "", url, headers);

		/*
		 * Check HTTP response.
		 */
		if (response.statusCode() != 200) {

			throw new RuntimeException("3DEXPERIENCE Change Action retrieval failed. " + "Status="
					+ response.statusCode() + " Response=" + response.body());
		}

		/*
		 * Parse the Change Action.
		 */
		ChangeActionInfo changeActionInfo = extractChangeAction(response.body());

		/*
		 * Build Copilot response.
		 */
		List<ChangeActionInfo> result = new ArrayList<>();
		result.add(changeActionInfo);

		return new ChangeActionDto(true, response.statusCode(), effectiveChangeActionId, result);
	}

	/**
	 * Extract Change Action information from the 3DEXPERIENCE JSON response.
	 */
	private ChangeActionInfo extractChangeAction(String responseBody) throws Exception {

		JSONParser parser = new JSONParser();

		JSONObject json = (JSONObject) parser.parse(responseBody);

		ChangeActionInfo info = new ChangeActionInfo();

		/*
		 * Change Action standard attributes.
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
		List<RealizedChangeInfo> realizedChanges = extractRealizedChanges(json);

		info.setRealizedChanges(realizedChanges);

		return info;
	}

	/**
	 * Extract the realizedChanges array returned by 3DEXPERIENCE.
	 */
	private List<RealizedChangeInfo> extractRealizedChanges(JSONObject json) {

		List<RealizedChangeInfo> result = new ArrayList<>();

		JSONArray realizedChanges = (JSONArray) json.get("realizedChanges");

		/*
		 * No realized change.
		 */
		if (realizedChanges == null) {
			return result;
		}

		/*
		 * Process each realized change.
		 */
		for (Object object : realizedChanges) {

			if (!(object instanceof JSONObject)) {
				continue;
			}

			JSONObject realizedChangeJson = (JSONObject) object;

			/*
			 * The "where" object contains the impacted object information.
			 */
			JSONObject whereJson = (JSONObject) realizedChangeJson.get("where");

			if (whereJson == null) {
				continue;
			}

			RealizedChangeInfo realizedChange = new RealizedChangeInfo();

			/*
			 * Object identifier.
			 */
			realizedChange.setIdentifier(getString(whereJson, "identifier"));

			/*
			 * Relative 3DEXPERIENCE REST path.
			 */
			realizedChange.setRelativePath(getString(whereJson, "relativePath"));

			/*
			 * Object source.
			 */
			realizedChange.setSource(getString(whereJson, "source"));

			/*
			 * Relation / realized change type.
			 *
			 * Examples: - CreateAssembly - Provide - ProcessContinuousProvide
			 */
			realizedChange.setType(getString(whereJson, "type"));

			/*
			 * Operations.
			 *
			 * Examples: - Create - Add
			 */
			JSONArray operationsJson = (JSONArray) realizedChangeJson.get("operations");

			List<String> operations = new ArrayList<>();

			if (operationsJson != null) {

				for (Object operation : operationsJson) {

					if (operation != null) {
						operations.add(operation.toString());
					}
				}
			}

			realizedChange.setOperations(operations);

			/*
			 * Add the realized change to the result.
			 */
			result.add(realizedChange);
		}

		return result;
	}

	/**
	 * Build required 3DEXPERIENCE security headers.
	 */
	private Map<String, String> createSecurityHeaders() {

		Map<String, String> headers = new HashMap<>();

		headers.put("SecurityContext", secContext);

		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

		return headers;
	}

	/**
	 * Safely retrieve a string value from a JSONObject.
	 */
	private String getString(JSONObject json, String key) {

		Object value = json.get(key);

		return value == null ? "" : value.toString();
	}
}