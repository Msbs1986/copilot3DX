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

@Service
public class ChangeActionService {

	private static final String CHANGE_ACTION_ENDPOINT = "/resources/v1/modeler/dslc/changeaction/search";

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

	public ChangeActionDto searchChangeActions(String searchStr) throws Exception {

		service3DXConnexion.prepareAuthorizationHeaderValue();

		// Conservé si cette méthode initialise ou rafraîchit le CSRF.
		service3DXConnexion.getCsrfTokenValueBasic();

		Map<String, String> headers = createSecurityHeaders();

		String effectiveSearchStr = searchStr == null || searchStr.isBlank() ? "*" : searchStr.trim();

		String encodedSearchStr = URLEncoder.encode(effectiveSearchStr, StandardCharsets.UTF_8);

		String searchUrl = space3dsUrlStr + CHANGE_ACTION_ENDPOINT + "?$searchStr=" + encodedSearchStr + "&$top=50";

		HttpResponse<String> searchResponse = httpRequestService.loadUrl("GET", "", "", searchUrl, headers);

		if (searchResponse.statusCode() != 200) {
			throw new RuntimeException("3DEXPERIENCE Change Action search failed. Status=" + searchResponse.statusCode()
					+ " Response=" + searchResponse.body());
		}

		List<ChangeActionInfo> result = extractChangeActions(searchResponse.body(), headers);

		return new ChangeActionDto(true, 200, effectiveSearchStr, result);
	}

	private List<ChangeActionInfo> extractChangeActions(String responseBody, Map<String, String> headers)
			throws Exception {

		JSONParser parser = new JSONParser();

		JSONObject searchJson = (JSONObject) parser.parse(responseBody);

		JSONArray changeActions = (JSONArray) searchJson.get("changeAction");

		List<ChangeActionInfo> result = new ArrayList<>();

		if (changeActions == null) {
			return result;
		}

		for (Object object : changeActions) {

			JSONObject changeAction = (JSONObject) object;

			String relativePath = getString(changeAction, "relativePath");

			if (relativePath.isBlank()) {
				continue;
			}

			try {
				ChangeActionInfo info = loadChangeActionDetails(relativePath, headers, parser);

				if (info != null) {
					result.add(info);
				}

			} catch (Exception exception) {
				System.err.println("Error while processing " + relativePath + ": " + exception.getMessage());
			}
		}

		return result;
	}

	private ChangeActionInfo loadChangeActionDetails(String relativePath, Map<String, String> headers,
			JSONParser parser) throws Exception {

		String detailUrl = space3dsUrlStr + relativePath;

		HttpResponse<String> detailResponse = httpRequestService.loadUrl("GET", "", "", detailUrl, headers);

		if (detailResponse.statusCode() != 200) {
			System.err.println("Change Action details failed. Status=" + detailResponse.statusCode() + " RelativePath="
					+ relativePath);

			return null;
		}

		JSONObject detailJson = (JSONObject) parser.parse(detailResponse.body());

		ChangeActionInfo info = new ChangeActionInfo();

		info.setId(getString(detailJson, "id"));
		info.setName(getString(detailJson, "name"));
		info.setTitle(getString(detailJson, "title"));
		info.setState(getString(detailJson, "state"));
		info.setOwner(getString(detailJson, "owner"));
		info.setOriginator(getString(detailJson, "originator"));
		info.setSeverity(getString(detailJson, "severity"));
		info.setCollabSpace(getString(detailJson, "collabSpace"));
		info.setDescription(getString(detailJson, "description"));
		info.setCreationDate(getString(detailJson, "Creation Date"));
		info.setModificationDate(getString(detailJson, "Last Modification Date"));

		return info;
	}

	private Map<String, String> createSecurityHeaders() {

		Map<String, String> headers = new HashMap<>();

		headers.put("SecurityContext", secContext);
		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

		return headers;
	}

	private String getString(JSONObject json, String key) {

		Object value = json.get(key);

		return value == null ? "" : value.toString();
	}
}