package com.keonys.copilot3DX.controller;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.config.HttpRequestService;
import com.keonys.copilot3DX.service.Service3DXConnexion;

@RestController
@RequestMapping("/manufacturing-items")
public class ManufacturingItemExpandController {

	@Value("${threedx.space-url}")
	private String space3dsUrlStr;

	@Value("${threedx.security-context}")
	private String secContext;

	private final Service3DXConnexion service3DXConnexion;
	private final HttpRequestService httpRequestService;

	private static final String SEARCH_ENDPOINT = "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/search";

	private static final String EXPAND_MASK = "dsmfg:MfgItem.ExpandMask.Details.V1";

	public ManufacturingItemExpandController(Service3DXConnexion service3DXConnexion,
			HttpRequestService httpRequestService) {

		this.service3DXConnexion = service3DXConnexion;
		this.httpRequestService = httpRequestService;
	}

	/**
	 * Search + Expand
	 *
	 * Exemple :
	 *
	 * GET /manufacturing-items/expand-search
	 *
	 * GET /manufacturing-items/expand-search?searchStr=*
	 *
	 * GET /manufacturing-items/expand-search ?searchStr=current=="Released"
	 */
	@GetMapping(value = "/expand-search", produces = MediaType.APPLICATION_JSON_VALUE)
	public JSONObject expandManufacturingItemsBySearch(@RequestParam(required = false) String searchStr)
			throws Exception {

		if (searchStr == null || searchStr.isBlank()) {
			searchStr = "*";
		}

		/*
		 * Authentification 3DX
		 */
		service3DXConnexion.prepareAuthorizationHeaderValue();

		String csrfToken = service3DXConnexion.getCsrfTokenValueBasic();

		Map<String, String> headers = createSecurityHeaders(csrfToken);

		/*
		 * Search
		 */
		String encodedSearchStr = URLEncoder.encode(searchStr, StandardCharsets.UTF_8);

		String searchUrl = space3dsUrlStr + SEARCH_ENDPOINT + "?$searchStr=" + encodedSearchStr + "&$top=1";

		HttpResponse<String> searchResponse = httpRequestService.loadUrl("GET", "", "", searchUrl, headers);

		if (searchResponse.statusCode() != 200) {

			throw new RuntimeException("Manufacturing Item Search failed. " + "Status=" + searchResponse.statusCode()
					+ " Response=" + searchResponse.body());
		}

		JSONParser parser = new JSONParser();

		JSONObject searchJson = (JSONObject) parser.parse(searchResponse.body());

		JSONArray members = findMembers(searchJson);

		JSONObject response = new JSONObject();

		if (members == null || members.isEmpty()) {

			response.put("success", false);
			response.put("message", "No Manufacturing Item found");

			return response;
		}

		JSONObject rootItem = (JSONObject) members.get(0);

		String physicalId = getFirstString(rootItem, "physicalId", "physicalid", "id");

		/*
		 * Expand
		 */
		String expandUrl = space3dsUrlStr + "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/" + physicalId + "/expand?$mask="
				+ EXPAND_MASK;

		JSONObject expandBody = new JSONObject();

		expandBody.put("expandDepth", -1);

		expandBody.put("withPath", true);

		HttpResponse<String> expandResponse = httpRequestService.loadUrl("POST", "", expandBody.toJSONString(),
				expandUrl, headers);

		if (expandResponse.statusCode() != 200 && expandResponse.statusCode() != 201) {

			throw new RuntimeException("Manufacturing Item Expand failed. " + "Status=" + expandResponse.statusCode()
					+ " Response=" + expandResponse.body());
		}

		/*
		 * Retourne directement le JSON du Expand
		 */
		return (JSONObject) parser.parse(expandResponse.body());
	}

	private JSONArray findMembers(JSONObject json) {

		String[] keys = { "member", "members", "results", "items", "mfgItems" };

		for (String key : keys) {

			Object value = json.get(key);

			if (value instanceof JSONArray) {
				return (JSONArray) value;
			}
		}

		return null;
	}

	private String getFirstString(JSONObject json, String... keys) {

		if (json == null) {
			return "";
		}

		for (String key : keys) {

			Object value = json.get(key);

			if (value != null && !value.toString().isBlank()) {

				return value.toString();
			}
		}

		return "";
	}

	private Map<String, String> createSecurityHeaders(String csrfToken) {

		Map<String, String> headers = new HashMap<>();

		headers.put("SecurityContext", secContext);

		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

		headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

		headers.put("ENO_CSRF_TOKEN", csrfToken);

		return headers;
	}
}