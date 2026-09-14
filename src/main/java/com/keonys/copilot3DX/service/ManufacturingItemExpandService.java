package com.keonys.copilot3DX.service;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

@Service
public class ManufacturingItemExpandService {

	private static final String SEARCH_ENDPOINT = "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/search";

	private static final String MFG_ITEM_ENDPOINT = "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/";

	private static final String EXPAND_MASK = "dsmfg:MfgItem.ExpandMask.Details.V1";

	private static final String DEFAULT_SEARCH_CRITERION = "\u002A";

	@Value("${threedx.space-url}")
	private String space3dsUrlStr;

	@Value("${threedx.security-context}")
	private String secContext;

	private final Service3DXConnexion service3DXConnexion;
	private final HttpRequestService httpRequestService;

	public ManufacturingItemExpandService(Service3DXConnexion service3DXConnexion,
			HttpRequestService httpRequestService) {

		this.service3DXConnexion = service3DXConnexion;
		this.httpRequestService = httpRequestService;
	}

	@SuppressWarnings("unchecked")
	public JSONObject expandManufacturingItemsBySearch(String searchStr) throws Exception {

		// 1. Critère de recherche par défaut
		if (searchStr == null || searchStr.isBlank()) {
			searchStr = DEFAULT_SEARCH_CRITERION;
		}

		// 2. Préparation de l'authentification
		service3DXConnexion.prepareAuthorizationHeaderValue();

		String csrfToken = service3DXConnexion.getCsrfTokenValueBasic();

		Map<String, String> headers = createSecurityHeaders(csrfToken);

		// 3. Recherche du Manufacturing Item racine
		JSONObject searchResult = searchFirstManufacturingItem(searchStr, headers);

		if (searchResult == null) {

			JSONObject response = new JSONObject();

			response.put("success", false);
			response.put("searchCriteria", searchStr);
			response.put("message", "No Manufacturing Item found for the provided search criteria.");

			return response;
		}

		String rootPhysicalId = getFirstString(searchResult, "physicalId", "physicalid", "id");

		if (rootPhysicalId.isBlank()) {

			JSONObject response = new JSONObject();

			response.put("success", false);
			response.put("searchCriteria", searchStr);
			response.put("message", "The Manufacturing Item found does not contain a physicalId.");

			return response;
		}

		// 4. Appel de l'API Expand
		JSONObject rawExpandResponse = expandManufacturingItem(rootPhysicalId, headers);

		// 5. Transformation en structure métier
		return buildCopilotResponse(searchStr, rootPhysicalId, rawExpandResponse);
	}

	private JSONObject searchFirstManufacturingItem(String searchStr, Map<String, String> headers) throws Exception {

		String encodedSearchStr = URLEncoder.encode(searchStr, StandardCharsets.UTF_8);

		String searchUrl = space3dsUrlStr + SEARCH_ENDPOINT + "?$searchStr=" + encodedSearchStr + "&$top=1";

		System.out.println("==========================================");
		System.out.println("3DX MANUFACTURING ITEM SEARCH");
		System.out.println("Search criteria : " + searchStr);
		System.out.println("Search URL      : " + searchUrl);
		System.out.println("==========================================");

		HttpResponse<String> searchResponse = httpRequestService.loadUrl("GET", "", "", searchUrl, headers);

		System.out.println("Search status : " + searchResponse.statusCode());

		if (searchResponse.statusCode() != 200) {

			throw new RuntimeException("Manufacturing Item Search failed. " + "Status=" + searchResponse.statusCode()
					+ " Response=" + searchResponse.body());
		}

		JSONParser parser = new JSONParser();

		Object parsedResponse = parser.parse(searchResponse.body());

		if (!(parsedResponse instanceof JSONObject)) {
			return null;
		}

		JSONObject searchJson = (JSONObject) parsedResponse;

		JSONArray members = findMembersArray(searchJson);

		if (members == null || members.isEmpty()) {
			return null;
		}

		Object firstResult = members.get(0);

		if (!(firstResult instanceof JSONObject)) {
			return null;
		}

		return (JSONObject) firstResult;
	}

	@SuppressWarnings("unchecked")
	private JSONObject expandManufacturingItem(String physicalId, Map<String, String> headers) throws Exception {

		String encodedPhysicalId = URLEncoder.encode(physicalId, StandardCharsets.UTF_8);

		String expandUrl = space3dsUrlStr + MFG_ITEM_ENDPOINT + encodedPhysicalId + "/expand?$mask=" + EXPAND_MASK;

		JSONObject expandBody = new JSONObject();

		expandBody.put("expandDepth", -1);
		expandBody.put("withPath", Boolean.TRUE);

		String requestBody = expandBody.toJSONString();

		System.out.println("==========================================");
		System.out.println("3DX MANUFACTURING ITEM EXPAND");
		System.out.println("Root physicalId : " + physicalId);
		System.out.println("Expand URL      : " + expandUrl);
		System.out.println("Request body    : " + requestBody);
		System.out.println("==========================================");

		HttpResponse<String> expandResponse = httpRequestService.loadUrl("POST", "", requestBody, expandUrl, headers);

		System.out.println("Expand status : " + expandResponse.statusCode());

		if (expandResponse.statusCode() != 200 && expandResponse.statusCode() != 201) {

			throw new RuntimeException("Manufacturing Item Expand failed. " + "PhysicalId=" + physicalId + " Status="
					+ expandResponse.statusCode() + " Response=" + expandResponse.body());
		}

		if (expandResponse.body() == null || expandResponse.body().isBlank()) {

			return new JSONObject();
		}

		JSONParser parser = new JSONParser();

		Object parsedResponse = parser.parse(expandResponse.body());

		if (parsedResponse instanceof JSONObject) {
			return (JSONObject) parsedResponse;
		}

		if (parsedResponse instanceof JSONArray) {

			JSONObject wrapper = new JSONObject();

			wrapper.put("member", parsedResponse);

			return wrapper;
		}

		return new JSONObject();
	}

	@SuppressWarnings("unchecked")
	private JSONObject buildCopilotResponse(String searchStr, String rootPhysicalId, JSONObject expandResponse) {

		JSONArray members = findMembersArray(expandResponse);

		Map<String, JSONObject> businessItems = new LinkedHashMap<>();

		Map<String, List<String>> childrenByParent = new LinkedHashMap<>();

		if (members != null) {

			for (Object memberObject : members) {

				if (!(memberObject instanceof JSONObject)) {
					continue;
				}

				JSONObject member = (JSONObject) memberObject;

				if (member.containsKey("path") && !member.containsKey("id")) {

					continue;
				}

				String id = getFirstString(member, "physicalId", "physicalid", "id");

				String parentId = getFirstString(member, "parent");

				String referenceId = getFirstString(member, "reference");

				if (!parentId.isBlank() && !referenceId.isBlank()) {

					childrenByParent.computeIfAbsent(parentId, key -> new ArrayList<>()).add(referenceId);

					continue;
				}

				if (!id.isBlank()) {
					businessItems.putIfAbsent(id, member);
				}
			}
		}

		Set<String> visited = new HashSet<>();

		JSONObject root = buildHierarchyNode(rootPhysicalId, businessItems, childrenByParent, visited, 0);

		JSONArray flatItems = new JSONArray();

		for (JSONObject item : businessItems.values()) {
			flatItems.add(simplifyBusinessItem(item));
		}

		JSONObject response = new JSONObject();

		response.put("success", true);

		response.put("source", "3DEXPERIENCE_MANUFACTURING_EXPAND");

		response.put("searchCriteria", searchStr);

		response.put("rootPhysicalId", rootPhysicalId);

		response.put("totalBusinessItems", businessItems.size());

		response.put("root", root);

		response.put("items", flatItems);

		return response;
	}

	@SuppressWarnings("unchecked")
	private JSONObject buildHierarchyNode(String physicalId, Map<String, JSONObject> businessItems,
			Map<String, List<String>> childrenByParent, Set<String> visited, int level) {

		JSONObject sourceItem = businessItems.get(physicalId);

		JSONObject node;

		if (sourceItem != null) {

			node = simplifyBusinessItem(sourceItem);

		} else {

			node = new JSONObject();

			node.put("physicalId", physicalId);
			node.put("title", "");
			node.put("type", "");
		}

		node.put("level", level);

		JSONArray children = new JSONArray();

		if (visited.contains(physicalId)) {

			node.put("cycleDetected", true);
			node.put("children", children);

			return node;
		}

		visited.add(physicalId);

		List<String> childIds = childrenByParent.get(physicalId);

		if (childIds != null) {

			for (String childId : childIds) {

				Set<String> branchVisited = new HashSet<>(visited);

				JSONObject childNode = buildHierarchyNode(childId, businessItems, childrenByParent, branchVisited,
						level + 1);

				children.add(childNode);
			}
		}

		node.put("children", children);

		return node;
	}

	@SuppressWarnings("unchecked")
	private JSONObject simplifyBusinessItem(JSONObject source) {

		JSONObject item = new JSONObject();

		item.put("physicalId", getFirstString(source, "physicalId", "physicalid", "id"));

		item.put("name", getFirstString(source, "name"));

		item.put("title", getFirstString(source, "title"));

		item.put("type", getFirstString(source, "type"));

		item.put("revision", getFirstString(source, "revision"));

		item.put("state", getFirstString(source, "state", "current", "maturity"));

		item.put("owner", getFirstString(source, "owner"));

		item.put("organization", getFirstString(source, "organization"));

		item.put("collabSpace", getFirstString(source, "collabSpace", "collabspace", "collaborativeSpace"));

		return item;
	}

	private JSONArray findMembersArray(JSONObject json) {

		if (json == null) {
			return null;
		}

		String[] possibleKeys = { "member", "members", "manufacturingItem", "manufacturingItems", "mfgItem", "mfgItems",
				"items", "results" };

		for (String key : possibleKeys) {

			Object value = json.get(key);

			if (value instanceof JSONArray) {
				return (JSONArray) value;
			}
		}

		return null;
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

	private Map<String, String> createSecurityHeaders(String csrfToken) {

		Map<String, String> headers = new HashMap<>();

		headers.put("SecurityContext", secContext);

		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

		headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

		if (csrfToken != null && !csrfToken.isBlank()) {

			headers.put("ENO_CSRF_TOKEN", csrfToken);
		}

		return headers;
	}
}