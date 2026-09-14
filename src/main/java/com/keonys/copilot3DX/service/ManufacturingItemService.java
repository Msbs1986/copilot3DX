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
import com.keonys.copilot3DX.dto.ManufacturingItemDto;
import com.keonys.copilot3DX.model.ManufacturingItemInfo;

@Service
public class ManufacturingItemService {

	private static final String SEARCH_ENDPOINT = "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/search";

	private static final String INFO_ENDPOINT = "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/";

	@Value("${threedx.space-url}")
	private String space3dsUrlStr;

	@Value("${threedx.security-context}")
	private String secContext;

	private final Service3DXConnexion service3DXConnexion;
	private final HttpRequestService httpRequestService;

	public ManufacturingItemService(Service3DXConnexion service3DXConnexion, HttpRequestService httpRequestService) {

		this.service3DXConnexion = service3DXConnexion;
		this.httpRequestService = httpRequestService;
	}

	public ManufacturingItemDto searchManufacturingItems(String searchStr) throws Exception {

		service3DXConnexion.prepareAuthorizationHeaderValue();
		service3DXConnexion.getCsrfTokenValueBasic();

		Map<String, String> headers = createSecurityHeaders();

		if (searchStr == null || searchStr.isBlank()) {

			searchStr = "*";

		}

		String effectiveSearchStr = searchStr;

		String encodedSearchStr =

				URLEncoder.encode(
						effectiveSearchStr,
						StandardCharsets.UTF_8
				);

		String searchUrl = space3dsUrlStr + SEARCH_ENDPOINT + "?$searchStr=" + encodedSearchStr + "&$top=50";

		System.out.println("==========================================");
		System.out.println("3DX MANUFACTURING ITEM SEARCH");
		System.out.println("Search criterion: " + effectiveSearchStr);
		System.out.println("Search URL: " + searchUrl);
		System.out.println("==========================================");

		HttpResponse<String> searchResponse = httpRequestService.loadUrl("GET", "", "", searchUrl, headers);

		if (searchResponse.statusCode() != 200) {
			throw new RuntimeException("Manufacturing Item search failed. Status=" + searchResponse.statusCode()
					+ " Response=" + searchResponse.body());
		}

		JSONParser parser = new JSONParser();

		JSONObject searchJson = (JSONObject) parser.parse(searchResponse.body());

		JSONArray manufacturingItems = findManufacturingItemsArray(searchJson);

		List<ManufacturingItemInfo> result = new ArrayList<>();

		if (manufacturingItems != null) {

			for (Object object : manufacturingItems) {

				if (!(object instanceof JSONObject)) {
					continue;
				}

				JSONObject searchResult = (JSONObject) object;

				String relativePath = getString(searchResult, "relativePath");

				String physicalId = getFirstString(searchResult, "physicalId", "physicalid", "id");

				String detailUrl = buildDetailUrl(relativePath, physicalId);

				if (detailUrl == null) {
					System.err.println("Unable to build detail URL for physicalId=" + physicalId);
					continue;
				}

				try {
					ManufacturingItemInfo info = loadManufacturingItemDetails(detailUrl, searchResult, headers, parser);

					if (info != null) {
						result.add(info);
					}

				} catch (Exception exception) {
					System.err.println(
							"Error while processing Manufacturing Item " + physicalId + ": " + exception.getMessage());
				}
			}
		}

		System.out.println("Manufacturing Items returned: " + result.size());

		return new ManufacturingItemDto(true, 200, effectiveSearchStr, result);
	}

	public ManufacturingItemInfo getManufacturingItemInfo(String physicalId) throws Exception {

		if (physicalId == null || physicalId.isBlank()) {
			throw new IllegalArgumentException("The physicalId parameter is required.");
		}

		String effectivePhysicalId = physicalId.trim();

		service3DXConnexion.prepareAuthorizationHeaderValue();

		Map<String, String> headers = service3DXConnexion.createAuthenticatedHeaders();

		headers.put("SecurityContext", secContext);
		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

		String detailUrl = space3dsUrlStr + INFO_ENDPOINT
				+ URLEncoder.encode(effectivePhysicalId, StandardCharsets.UTF_8);

		HttpResponse<String> detailResponse = httpRequestService.loadUrl("GET", "", "", detailUrl, headers);

		if (detailResponse.statusCode() != 200) {
			throw new RuntimeException("Manufacturing Item Get Info failed. Status=" + detailResponse.statusCode()
					+ " Response=" + detailResponse.body());
		}

		JSONParser parser = new JSONParser();

		JSONObject detailJson = (JSONObject) parser.parse(detailResponse.body());

		JSONObject manufacturingItemJson = extractDetailObject(detailJson);

		return mapManufacturingItem(manufacturingItemJson, null);
	}

	private ManufacturingItemInfo loadManufacturingItemDetails(String detailUrl, JSONObject searchResult,
			Map<String, String> headers, JSONParser parser) throws Exception {

		HttpResponse<String> detailResponse = httpRequestService.loadUrl("GET", "", "", detailUrl, headers);

		if (detailResponse.statusCode() != 200) {
			System.err.println("Manufacturing Item Get Info failed. Status=" + detailResponse.statusCode());
			return null;
		}

		JSONObject detailJson = (JSONObject) parser.parse(detailResponse.body());

		JSONObject manufacturingItemJson = extractDetailObject(detailJson);

		return mapManufacturingItem(manufacturingItemJson, searchResult);
	}

	private String buildDetailUrl(String relativePath, String physicalId) {

		if (relativePath != null && !relativePath.isBlank()) {

			String normalizedPath = relativePath.trim();

			if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {

				return normalizedPath;
			}

			if (!normalizedPath.startsWith("/")) {
				normalizedPath = "/" + normalizedPath;
			}

			return space3dsUrlStr + normalizedPath;
		}

		if (physicalId != null && !physicalId.isBlank()) {

			return space3dsUrlStr + INFO_ENDPOINT + URLEncoder.encode(physicalId.trim(), StandardCharsets.UTF_8);
		}

		return null;
	}

	private JSONArray findManufacturingItemsArray(JSONObject searchJson) {

		String[] possibleKeys = { "manufacturingItem", "manufacturingItems", "mfgItem", "mfgItems", "member", "members",
				"items", "results" };

		for (String key : possibleKeys) {

			Object value = searchJson.get(key);

			if (value instanceof JSONArray) {
				return (JSONArray) value;
			}
		}

		System.err.println("No Manufacturing Item array found. Keys=" + searchJson.keySet());

		return null;
	}

	private JSONObject extractDetailObject(JSONObject detailJson) {

		if (detailJson == null) {
			return new JSONObject();
		}

		String[] objectKeys = { "manufacturingItem", "mfgItem", "data" };

		for (String key : objectKeys) {

			Object value = detailJson.get(key);

			if (value instanceof JSONObject) {
				return (JSONObject) value;
			}

			if (value instanceof JSONArray) {

				JSONArray values = (JSONArray) value;

				if (!values.isEmpty() && values.get(0) instanceof JSONObject) {

					return (JSONObject) values.get(0);
				}
			}
		}

		Object memberValue = detailJson.get("member");

		if (memberValue instanceof JSONArray) {

			JSONArray members = (JSONArray) memberValue;

			if (!members.isEmpty() && members.get(0) instanceof JSONObject) {

				return (JSONObject) members.get(0);
			}
		}

		return detailJson;
	}

	private ManufacturingItemInfo mapManufacturingItem(JSONObject detailJson, JSONObject searchJson) {

		ManufacturingItemInfo info = new ManufacturingItemInfo();

		info.setId(getFirstAvailableValue(detailJson, searchJson, "id"));

		info.setPhysicalId(getFirstAvailableValue(detailJson, searchJson, "physicalId", "physicalid", "id"));

		info.setName(getFirstAvailableValue(detailJson, searchJson, "name"));

		info.setTitle(getFirstAvailableValue(detailJson, searchJson, "title"));

		info.setRevision(getFirstAvailableValue(detailJson, searchJson, "revision"));

		info.setType(getFirstAvailableValue(detailJson, searchJson, "type"));

		info.setState(getFirstAvailableValue(detailJson, searchJson, "state", "current", "maturity"));

		info.setOwner(getFirstAvailableValue(detailJson, searchJson, "owner"));

		info.setCollabSpace(
				getFirstAvailableValue(detailJson, searchJson, "collabSpace", "collaborativeSpace", "project"));

		info.setOrganization(getFirstAvailableValue(detailJson, searchJson, "organization", "organizationName"));

		info.setDescription(getFirstAvailableValue(detailJson, searchJson, "description"));

		info.setCreationDate(
				getFirstAvailableValue(detailJson, searchJson, "Creation Date", "created", "creationDate"));

		info.setModificationDate(getFirstAvailableValue(detailJson, searchJson, "Last Modification Date", "modified",
				"modificationDate"));

		return info;
	}

	private String getFirstAvailableValue(JSONObject detailJson, JSONObject searchJson, String... keys) {

		String detailValue = getFirstString(detailJson, keys);

		if (!detailValue.isBlank()) {
			return detailValue;
		}

		return getFirstString(searchJson, keys);
	}

	private String getFirstString(JSONObject json, String... keys) {

		if (json == null || keys == null) {
			return "";
		}

		for (String key : keys) {

			Object value = json.get(key);

			if (value == null) {
				continue;
			}

			String stringValue = value.toString();

			if (!stringValue.isBlank()) {
				return stringValue;
			}
		}

		return "";
	}

	private String getString(JSONObject json, String key) {

		if (json == null) {
			return "";
		}

		Object value = json.get(key);

		return value == null ? "" : value.toString();
	}

	private Map<String, String> createSecurityHeaders() {

		Map<String, String> headers = new HashMap<>();

		headers.put("SecurityContext", secContext);
		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

		return headers;
	}
}
