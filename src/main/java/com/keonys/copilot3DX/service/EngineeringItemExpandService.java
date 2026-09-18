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
public class EngineeringItemExpandService {

	private static final String SEARCH_ENDPOINT = "/resources/v1/modeler/dseng/dseng:EngItem/search";

	private static final String ENG_ITEM_ENDPOINT = "/resources/v1/modeler/dseng/dseng:EngItem/";

	private static final String DEFAULT_SEARCH_CRITERION = "\u002A";

	@Value("${threedx.space-url}")
	private String space3dsUrlStr;

	@Value("${threedx.security-context}")
	private String secContext;

	private final Service3DXConnexion service3DXConnexion;
	private final HttpRequestService httpRequestService;

	public EngineeringItemExpandService(Service3DXConnexion service3DXConnexion,
			HttpRequestService httpRequestService) {

		this.service3DXConnexion = service3DXConnexion;
		this.httpRequestService = httpRequestService;
	}

	@SuppressWarnings("unchecked")
	public JSONObject expandEngineeringItemsBySearch(String searchStr) throws Exception {

		if (searchStr == null || searchStr.isBlank()) {
			searchStr = DEFAULT_SEARCH_CRITERION;
		}

		service3DXConnexion.prepareAuthorizationHeaderValue();

		String csrfToken = service3DXConnexion.getCsrfTokenValueBasic();

		Map<String, String> headers = createSecurityHeaders(csrfToken);

		JSONObject searchResult = searchFirstEngineeringItem(searchStr, headers);

		if (searchResult == null) {

			JSONObject response = new JSONObject();

			response.put("success", false);
			response.put("searchCriteria", searchStr);
			response.put("message", "No Engineering Item found for the provided search criteria.");

			return response;
		}

		String rootPhysicalId = getFirstString(searchResult, "physicalId", "physicalid", "id");

		if (rootPhysicalId.isBlank()) {

			JSONObject response = new JSONObject();

			response.put("success", false);
			response.put("searchCriteria", searchStr);
			response.put("message", "The Engineering Item found does not contain a physicalId.");

			return response;
		}

		JSONObject rawExpandResponse = expandEngineeringItem(rootPhysicalId, headers);

		return buildCopilotResponse(searchStr, rootPhysicalId, rawExpandResponse);
	}

	private JSONObject searchFirstEngineeringItem(String searchStr, Map<String, String> headers) throws Exception {

		String encodedSearchStr = URLEncoder.encode(searchStr, StandardCharsets.UTF_8);

		String searchUrl = space3dsUrlStr + SEARCH_ENDPOINT + "?$searchStr=" + encodedSearchStr + "&$top=1";

		System.out.println("==========================================");
		System.out.println("3DX ENGINEERING ITEM SEARCH");
		System.out.println("Search criteria : " + searchStr);
		System.out.println("Search URL      : " + searchUrl);
		System.out.println("==========================================");

		HttpResponse<String> searchResponse = httpRequestService.loadUrl("GET", "", "", searchUrl, headers);

		System.out.println("Search status : " + searchResponse.statusCode());

		if (searchResponse.statusCode() != 200) {

			throw new RuntimeException("Engineering Item Search failed. " + "Status=" + searchResponse.statusCode()
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

	private JSONObject expandEngineeringItem(String physicalId, Map<String, String> headers) throws Exception {

		String encodedPhysicalId = URLEncoder.encode(physicalId, StandardCharsets.UTF_8);

		String expandUrl = space3dsUrlStr + ENG_ITEM_ENDPOINT + encodedPhysicalId + "/expand";

		JSONObject expandBody = new JSONObject();

		expandBody.put("expandDepth", -1);
		expandBody.put("withPath", Boolean.FALSE);

		JSONArray typeFilterBo = new JSONArray();
		typeFilterBo.add("VPMReference");
		typeFilterBo.add("Drawing");

		expandBody.put("type_filter_bo", typeFilterBo);

		JSONArray typeFilterRel = new JSONArray();
		typeFilterRel.add("VPMInstance");
		typeFilterRel.add("VPMRepInstance");

		expandBody.put("type_filter_rel", typeFilterRel);

		String requestBody = expandBody.toJSONString();

		System.out.println("Request body : " + requestBody);

		System.out.println("==========================================");
		System.out.println("3DX ENGINEERING ITEM EXPAND");
		System.out.println("Root physicalId : " + physicalId);
		System.out.println("Expand URL      : " + expandUrl);
		System.out.println("==========================================");

		HttpResponse<String> expandResponse = httpRequestService.loadUrl("POST", "", requestBody, expandUrl, headers);

		System.out.println("Expand status : " + expandResponse.statusCode());

		if (expandResponse.statusCode() != 200) {

			throw new RuntimeException("Engineering Item Expand failed. " + "PhysicalId=" + physicalId + " Status="
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

		Map<String, JSONObject> engineeringItems = new LinkedHashMap<>();

		Map<String, JSONObject> instances = new LinkedHashMap<>();

		Map<String, String> referenceIdByCestamp = new HashMap<>();

		Map<String, List<String>> childrenByParent = new LinkedHashMap<>();

		if (members != null) {

			for (Object memberObject : members) {

				if (!(memberObject instanceof JSONObject)) {
					continue;
				}

				JSONObject member = (JSONObject) memberObject;

				String type = getFirstString(member, "type");

				String id = getFirstString(member, "physicalId", "physicalid", "id");

				if (id.isBlank()) {
					continue;
				}

				if (isEngineeringReference(type)) {

					engineeringItems.putIfAbsent(id, member);

					String cestamp = getFirstString(member, "cestamp");

					if (!cestamp.isBlank()) {

						referenceIdByCestamp.putIfAbsent(cestamp, id);
					}

				} else if (isEngineeringInstance(type)) {

					instances.putIfAbsent(id, member);
				}
			}

			buildRelationsFromExplicitFields(members, childrenByParent);

			buildRelationsFromPaths(members, rootPhysicalId, childrenByParent);

			buildRelationsFromInstances(rootPhysicalId, instances, referenceIdByCestamp, childrenByParent);
		}

		JSONObject root = buildHierarchyNode(rootPhysicalId, engineeringItems, childrenByParent, new HashSet<>(), 0);

		JSONArray flatItems = new JSONArray();

		for (JSONObject item : engineeringItems.values()) {
			flatItems.add(simplifyEngineeringItem(item));
		}

		JSONArray flatInstances = new JSONArray();

		for (JSONObject instance : instances.values()) {
			flatInstances.add(simplifyEngineeringInstance(instance, referenceIdByCestamp));
		}

		JSONObject response = new JSONObject();

		response.put("success", true);
		response.put("source", "3DEXPERIENCE_ENGINEERING_EXPAND");

		response.put("structureType", "EBOM");
		response.put("searchCriteria", searchStr);
		response.put("rootPhysicalId", rootPhysicalId);

		response.put("totalEngineeringItems", engineeringItems.size());

		response.put("totalInstances", instances.size());

		response.put("root", root);
		response.put("items", flatItems);
		response.put("instances", flatInstances);

		return response;
	}

	private void buildRelationsFromExplicitFields(JSONArray members, Map<String, List<String>> childrenByParent) {

		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject member = (JSONObject) memberObject;

			String parentId = getFirstString(member, "parent", "parentId", "parentPhysicalId");

			String referenceId = getFirstString(member, "reference", "referenceId", "child", "childId",
					"childPhysicalId");

			if (!parentId.isBlank() && !referenceId.isBlank()) {

				addChildRelation(childrenByParent, parentId, referenceId);
			}
		}
	}

	private void buildRelationsFromPaths(JSONArray members, String rootPhysicalId,
			Map<String, List<String>> childrenByParent) {

		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject member = (JSONObject) memberObject;

			Object pathObject = member.get("path");

			if (!(pathObject instanceof JSONArray)) {
				continue;
			}

			JSONArray path = (JSONArray) pathObject;

			List<String> pathIds = extractPathIds(path);

			if (pathIds.isEmpty()) {
				continue;
			}

			if (!rootPhysicalId.equals(pathIds.get(0))) {
				pathIds.add(0, rootPhysicalId);
			}

			for (int index = 0; index < pathIds.size() - 1; index++) {

				String parentId = pathIds.get(index);
				String childId = pathIds.get(index + 1);

				addChildRelation(childrenByParent, parentId, childId);
			}
		}
	}

	private void buildRelationsFromInstances(String rootPhysicalId, Map<String, JSONObject> instances,
			Map<String, String> referenceIdByCestamp, Map<String, List<String>> childrenByParent) {

		for (JSONObject instance : instances.values()) {

			String childReferenceId = getFirstString(instance, "reference", "referenceId", "child", "childId");

			if (childReferenceId.isBlank()) {

				String cestamp = getFirstString(instance, "cestamp");

				childReferenceId = referenceIdByCestamp.getOrDefault(cestamp, "");
			}

			if (childReferenceId.isBlank()) {
				continue;
			}

			String parentReferenceId = getFirstString(instance, "parent", "parentId", "parentReference",
					"parentReferenceId");

			if (parentReferenceId.isBlank()) {

				/*
				 * Fallback pour la réponse actuelle.
				 *
				 * Dans le JSON fourni, les VPMInstance permettent d'identifier les VPMReference
				 * enfants grâce au cestamp, mais aucun parent n'est retourné.
				 *
				 * On considère donc que ces occurrences sont des enfants directs de la racine.
				 */
				parentReferenceId = rootPhysicalId;
			}

			if (!parentReferenceId.equals(childReferenceId)) {

				addChildRelation(childrenByParent, parentReferenceId, childReferenceId);
			}
		}
	}

	private List<String> extractPathIds(JSONArray path) {

		List<String> pathIds = new ArrayList<>();

		for (Object pathElement : path) {

			if (pathElement == null) {
				continue;
			}

			if (pathElement instanceof String) {

				String id = pathElement.toString();

				if (!id.isBlank()) {
					pathIds.add(id);
				}

				continue;
			}

			if (pathElement instanceof JSONObject) {

				JSONObject pathObject = (JSONObject) pathElement;

				String id = getFirstString(pathObject, "physicalId", "physicalid", "id", "reference", "referenceId");

				if (!id.isBlank()) {
					pathIds.add(id);
				}
			}
		}

		return pathIds;
	}

	private void addChildRelation(Map<String, List<String>> childrenByParent, String parentId, String childId) {

		if (parentId == null || parentId.isBlank() || childId == null || childId.isBlank()) {
			return;
		}

		List<String> children = childrenByParent.computeIfAbsent(parentId, key -> new ArrayList<>());

		if (!children.contains(childId)) {
			children.add(childId);
		}
	}

	@SuppressWarnings("unchecked")
	private JSONObject buildHierarchyNode(String physicalId, Map<String, JSONObject> engineeringItems,
			Map<String, List<String>> childrenByParent, Set<String> visited, int level) {

		JSONObject sourceItem = engineeringItems.get(physicalId);

		JSONObject node;

		if (sourceItem != null) {

			node = simplifyEngineeringItem(sourceItem);

		} else {

			node = new JSONObject();

			node.put("physicalId", physicalId);
			node.put("name", "");
			node.put("title", "");
			node.put("type", "");
			node.put("revision", "");
			node.put("state", "");
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

				JSONObject childNode = buildHierarchyNode(childId, engineeringItems, childrenByParent, branchVisited,
						level + 1);

				children.add(childNode);
			}
		}

		node.put("children", children);

		return node;
	}

	@SuppressWarnings("unchecked")
	private JSONObject simplifyEngineeringItem(JSONObject source) {

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

		item.put("created", getFirstString(source, "created"));

		item.put("modified", getFirstString(source, "modified"));

		item.put("cestamp", getFirstString(source, "cestamp"));

		return item;
	}

	@SuppressWarnings("unchecked")
	private JSONObject simplifyEngineeringInstance(JSONObject source, Map<String, String> referenceIdByCestamp) {

		JSONObject instance = new JSONObject();

		String cestamp = getFirstString(source, "cestamp");

		String referenceId = getFirstString(source, "reference", "referenceId", "child", "childId");

		if (referenceId.isBlank()) {

			referenceId = referenceIdByCestamp.getOrDefault(cestamp, "");
		}

		instance.put("physicalId", getFirstString(source, "physicalId", "physicalid", "id"));

		instance.put("name", getFirstString(source, "name"));

		instance.put("type", getFirstString(source, "type"));

		instance.put("referencePhysicalId", referenceId);

		instance.put("cestamp", cestamp);

		instance.put("created", getFirstString(source, "created"));

		instance.put("modified", getFirstString(source, "modified"));

		return instance;
	}

	private boolean isEngineeringReference(String type) {

		return "VPMReference".equalsIgnoreCase(type) || "Engineering Item".equalsIgnoreCase(type)
				|| "Physical Product".equalsIgnoreCase(type);
	}

	private boolean isEngineeringInstance(String type) {

		return "VPMInstance".equalsIgnoreCase(type);
	}

	private JSONArray findMembersArray(JSONObject json) {

		if (json == null) {
			return null;
		}

		String[] possibleKeys = { "member", "members", "engineeringItem", "engineeringItems", "engItem", "engItems",
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