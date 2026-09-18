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

		if (searchResponse.body() == null || searchResponse.body().isBlank()) {

			return null;
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
	private JSONObject expandEngineeringItem(String physicalId, Map<String, String> headers) throws Exception {

		String encodedPhysicalId = URLEncoder.encode(physicalId, StandardCharsets.UTF_8);

		String expandUrl = space3dsUrlStr + ENG_ITEM_ENDPOINT + encodedPhysicalId + "/expand";

		JSONObject expandBody = new JSONObject();

		expandBody.put("expandDepth", Long.valueOf(-1));

		expandBody.put("withPath", Boolean.TRUE);

		JSONArray businessObjectTypes = new JSONArray();

		businessObjectTypes.add("VPMReference");
		businessObjectTypes.add("Drawing");

		expandBody.put("type_filter_bo", businessObjectTypes);

		JSONArray relationshipTypes = new JSONArray();

		relationshipTypes.add("VPMInstance");
		relationshipTypes.add("VPMRepInstance");

		expandBody.put("type_filter_rel", relationshipTypes);

		String requestBody = expandBody.toJSONString();

		System.out.println("==========================================");
		System.out.println("3DX ENGINEERING ITEM EXPAND");
		System.out.println("Root physicalId : " + physicalId);
		System.out.println("Expand URL      : " + expandUrl);
		System.out.println("Request body    : " + requestBody);
		System.out.println("==========================================");

		HttpResponse<String> expandResponse = httpRequestService.loadUrl("POST", "", requestBody, expandUrl, headers);

		System.out.println("Expand status : " + expandResponse.statusCode());

		if (expandResponse.statusCode() != 200 && expandResponse.statusCode() != 201) {

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

		Map<String, JSONObject> businessObjects = new LinkedHashMap<>();

		Map<String, JSONObject> relationships = new LinkedHashMap<>();

		Map<String, List<String>> childrenByParent = new LinkedHashMap<>();

		JSONArray unresolvedPaths = new JSONArray();

		if (members != null) {

			collectBusinessObjects(members, businessObjects);

			collectRelationships(members, relationships);

			buildRelationsFromPaths(members, rootPhysicalId, businessObjects, childrenByParent, unresolvedPaths);
		}

		/*
		 * La réponse d'expand doit normalement contenir l'objet racine.
		 *
		 * Cette sécurité permet cependant de reprendre les informations retournées par
		 * le search.
		 */
		JSONObject root = buildHierarchyNode(rootPhysicalId, "", businessObjects, childrenByParent, new HashSet<>(), 0);

		JSONArray flatItems = new JSONArray();

		buildFlatHierarchy(root, flatItems, "");

		JSONArray flatRelationships = new JSONArray();

		for (JSONObject relationship : relationships.values()) {

			flatRelationships.add(simplifyRelationship(relationship));
		}

		JSONObject response = new JSONObject();

		response.put("success", Boolean.TRUE);

		response.put("source", "3DEXPERIENCE_ENGINEERING_EXPAND");

		response.put("structureType", "EBOM");

		response.put("searchCriteria", searchStr);

		response.put("rootPhysicalId", rootPhysicalId);

		response.put("totalBusinessObjects", Long.valueOf(businessObjects.size()));

		response.put("totalRelationships", Long.valueOf(relationships.size()));

		response.put("totalStructureItems", Long.valueOf(flatItems.size()));

		response.put("totalPaths", countPaths(members));

		response.put("maximumLevel", findMaximumLevel(flatItems));

		response.put("root", root);

		response.put("items", flatItems);

		response.put("relationships", flatRelationships);

		response.put("unresolvedPaths", unresolvedPaths);

		boolean structureReliable = unresolvedPaths.isEmpty();

		response.put("structureReliable", structureReliable);

		if (!structureReliable) {

			response.put("warning", "Some Path entries could not be resolved completely.");
		}

		return response;
	}

	private void collectBusinessObjects(JSONArray members, Map<String, JSONObject> businessObjects) {

		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject member = (JSONObject) memberObject;

			String type = getFirstString(member, "type");

			if (!isSupportedBusinessObject(type)) {
				continue;
			}

			String physicalId = getFirstString(member, "physicalId", "physicalid", "id");

			if (physicalId.isBlank()) {
				continue;
			}

			businessObjects.putIfAbsent(physicalId, member);
		}
	}

	private void collectRelationships(JSONArray members, Map<String, JSONObject> relationships) {

		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject member = (JSONObject) memberObject;

			String type = getFirstString(member, "type");

			if (!isSupportedRelationship(type)) {
				continue;
			}

			String physicalId = getFirstString(member, "physicalId", "physicalid", "id");

			if (physicalId.isBlank()) {
				continue;
			}

			relationships.putIfAbsent(physicalId, member);
		}
	}

	@SuppressWarnings("unchecked")
	private void buildRelationsFromPaths(JSONArray members, String rootPhysicalId,
			Map<String, JSONObject> businessObjects, Map<String, List<String>> childrenByParent,
			JSONArray unresolvedPaths) {

		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject member = (JSONObject) memberObject;

			/*
			 * La réponse 3DEXPERIENCE utilise "Path" avec une majuscule.
			 *
			 * "path" est également accepté pour assurer la compatibilité avec d'autres
			 * versions.
			 */
			Object pathObject = getFirstObject(member, "Path", "path");

			if (pathObject == null) {
				continue;
			}

			List<String> completePath = extractCompletePath(pathObject);

			if (completePath.isEmpty()) {

				unresolvedPaths.add(createUnresolvedPath(pathObject, "The Path is empty or invalid."));

				continue;
			}

			/*
			 * Le chemin 3DEXPERIENCE contient une alternance :
			 *
			 * Business Object Relationship Business Object Relationship Business Object
			 *
			 * On conserve uniquement les IDs présents dans businessObjects. Les VPMInstance
			 * et VPMRepInstance sont donc automatiquement ignorés.
			 */
			List<String> businessObjectPath = filterBusinessObjectsFromPath(completePath, businessObjects);

			if (businessObjectPath.isEmpty()) {

				unresolvedPaths
						.add(createUnresolvedPath(pathObject, "No supported Business Object was found in the Path."));

				continue;
			}

			if (!rootPhysicalId.equals(businessObjectPath.get(0))) {

				businessObjectPath.add(0, rootPhysicalId);
			}

			addPathRelations(businessObjectPath, childrenByParent);
		}
	}

	private List<String> extractCompletePath(Object pathObject) {

		List<String> completePath = new ArrayList<>();

		if (pathObject == null) {
			return completePath;
		}

		if (pathObject instanceof JSONArray) {

			JSONArray pathArray = (JSONArray) pathObject;

			for (Object pathElement : pathArray) {

				String physicalId = extractPhysicalIdFromPathElement(pathElement);

				if (!physicalId.isBlank()) {
					completePath.add(physicalId);
				}
			}

			return completePath;
		}

		if (pathObject instanceof JSONObject) {

			JSONObject pathJson = (JSONObject) pathObject;

			Object nestedPath = getFirstObject(pathJson, "Path", "path", "member", "members", "items");

			if (nestedPath != null && nestedPath != pathObject) {

				return extractCompletePath(nestedPath);
			}

			String physicalId = extractPhysicalIdFromPathElement(pathJson);

			if (!physicalId.isBlank()) {
				completePath.add(physicalId);
			}

			return completePath;
		}

		if (pathObject instanceof String) {

			String pathString = pathObject.toString().trim();

			if (pathString.isBlank()) {
				return completePath;
			}

			String normalizedPath = pathString.replace("[", "").replace("]", "").replace("\"", "");

			String[] pathParts = normalizedPath.split("[/,;>|]");

			for (String pathPart : pathParts) {

				String physicalId = pathPart.trim();

				if (!physicalId.isBlank()) {
					completePath.add(physicalId);
				}
			}
		}

		return completePath;
	}

	private String extractPhysicalIdFromPathElement(Object pathElement) {

		if (pathElement == null) {
			return "";
		}

		if (pathElement instanceof String) {

			return pathElement.toString().trim();
		}

		if (!(pathElement instanceof JSONObject)) {
			return "";
		}

		JSONObject pathJson = (JSONObject) pathElement;

		return getFirstString(pathJson, "physicalId", "physicalid", "id");
	}

	private List<String> filterBusinessObjectsFromPath(List<String> completePath,
			Map<String, JSONObject> businessObjects) {

		List<String> businessObjectPath = new ArrayList<>();

		for (String physicalId : completePath) {

			if (!businessObjects.containsKey(physicalId)) {
				continue;
			}

			/*
			 * On évite seulement les doublons consécutifs.
			 *
			 * Le même Business Object peut être présent plusieurs fois dans une structure à
			 * travers des occurrences différentes.
			 */
			if (businessObjectPath.isEmpty()
					|| !physicalId.equals(businessObjectPath.get(businessObjectPath.size() - 1))) {

				businessObjectPath.add(physicalId);
			}
		}

		return businessObjectPath;
	}

	private void addPathRelations(List<String> businessObjectPath, Map<String, List<String>> childrenByParent) {

		if (businessObjectPath == null || businessObjectPath.size() < 2) {

			return;
		}

		for (int index = 0; index < businessObjectPath.size() - 1; index++) {

			String parentPhysicalId = businessObjectPath.get(index);

			String childPhysicalId = businessObjectPath.get(index + 1);

			addChildRelation(childrenByParent, parentPhysicalId, childPhysicalId);
		}
	}

	private void addChildRelation(Map<String, List<String>> childrenByParent, String parentPhysicalId,
			String childPhysicalId) {

		if (parentPhysicalId == null || parentPhysicalId.isBlank() || childPhysicalId == null
				|| childPhysicalId.isBlank() || parentPhysicalId.equals(childPhysicalId)) {

			return;
		}

		List<String> children = childrenByParent.computeIfAbsent(parentPhysicalId, key -> new ArrayList<>());

		if (!children.contains(childPhysicalId)) {
			children.add(childPhysicalId);
		}
	}

	@SuppressWarnings("unchecked")
	private JSONObject buildHierarchyNode(String physicalId, String parentPhysicalId,
			Map<String, JSONObject> businessObjects, Map<String, List<String>> childrenByParent,
			Set<String> branchVisited, int level) {

		JSONObject sourceObject = businessObjects.get(physicalId);

		JSONObject node;

		if (sourceObject != null) {

			node = simplifyBusinessObject(sourceObject);

		} else {

			node = new JSONObject();

			node.put("physicalId", physicalId);

			node.put("name", "");
			node.put("title", "");
			node.put("type", "");
			node.put("revision", "");
			node.put("state", "");
			node.put("owner", "");
			node.put("organization", "");
			node.put("collabSpace", "");
			node.put("created", "");
			node.put("modified", "");
			node.put("cestamp", "");
		}

		node.put("parentPhysicalId", parentPhysicalId);

		node.put("level", Long.valueOf(level));

		JSONArray children = new JSONArray();

		if (branchVisited.contains(physicalId)) {

			node.put("cycleDetected", Boolean.TRUE);

			node.put("children", children);

			node.put("childrenCount", Long.valueOf(0));

			return node;
		}

		branchVisited.add(physicalId);

		List<String> childIds = childrenByParent.get(physicalId);

		if (childIds != null) {

			for (String childId : childIds) {

				Set<String> childBranchVisited = new HashSet<>(branchVisited);

				JSONObject childNode = buildHierarchyNode(childId, physicalId, businessObjects, childrenByParent,
						childBranchVisited, level + 1);

				children.add(childNode);
			}
		}

		node.put("children", children);

		node.put("childrenCount", Long.valueOf(children.size()));

		return node;
	}

	@SuppressWarnings("unchecked")
	private void buildFlatHierarchy(JSONObject node, JSONArray flatItems, String parentDisplayPath) {

		if (node == null) {
			return;
		}

		String title = getFirstString(node, "title", "name", "physicalId");

		String displayPath;

		if (parentDisplayPath == null || parentDisplayPath.isBlank()) {

			displayPath = title;

		} else {

			displayPath = parentDisplayPath + " > " + title;
		}

		long level = getLongValue(node.get("level"), 0L);

		JSONObject flatItem = new JSONObject();

		flatItem.put("physicalId", getFirstString(node, "physicalId"));

		flatItem.put("parentPhysicalId", getFirstString(node, "parentPhysicalId"));

		flatItem.put("name", getFirstString(node, "name"));

		flatItem.put("title", title);

		flatItem.put("type", getFirstString(node, "type"));

		flatItem.put("revision", getFirstString(node, "revision"));

		flatItem.put("state", getFirstString(node, "state"));

		flatItem.put("owner", getFirstString(node, "owner"));

		flatItem.put("organization", getFirstString(node, "organization"));

		flatItem.put("collabSpace", getFirstString(node, "collabSpace"));

		flatItem.put("level", Long.valueOf(level));

		flatItem.put("levelLabel", "Level " + level);

		flatItem.put("childrenCount", Long.valueOf(getLongValue(node.get("childrenCount"), 0L)));

		flatItem.put("displayPath", displayPath);

		flatItem.put("displayLabel", createIndentedLabel(title, level));

		flatItems.add(flatItem);

		Object childrenObject = node.get("children");

		if (!(childrenObject instanceof JSONArray)) {
			return;
		}

		JSONArray children = (JSONArray) childrenObject;

		for (Object childObject : children) {

			if (childObject instanceof JSONObject) {

				buildFlatHierarchy((JSONObject) childObject, flatItems, displayPath);
			}
		}
	}

	private String createIndentedLabel(String title, long level) {

		StringBuilder label = new StringBuilder();

		for (long index = 0; index < level; index++) {

			label.append("  ");
		}

		if (level > 0) {
			label.append("|- ");
		}

		label.append(title);

		return label.toString();
	}

	@SuppressWarnings("unchecked")
	private JSONObject simplifyBusinessObject(JSONObject source) {

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
	private JSONObject simplifyRelationship(JSONObject source) {

		JSONObject relationship = new JSONObject();

		relationship.put("physicalId", getFirstString(source, "physicalId", "physicalid", "id"));

		relationship.put("name", getFirstString(source, "name"));

		relationship.put("type", getFirstString(source, "type"));

		relationship.put("cestamp", getFirstString(source, "cestamp"));

		relationship.put("created", getFirstString(source, "created"));

		relationship.put("modified", getFirstString(source, "modified"));

		return relationship;
	}

	@SuppressWarnings("unchecked")
	private JSONObject createUnresolvedPath(Object pathObject, String reason) {

		JSONObject unresolved = new JSONObject();

		unresolved.put("path", pathObject);

		unresolved.put("reason", reason);

		return unresolved;
	}

	private long countPaths(JSONArray members) {

		if (members == null) {
			return 0L;
		}

		long totalPaths = 0L;

		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject member = (JSONObject) memberObject;

			Object pathObject = getFirstObject(member, "Path", "path");

			if (pathObject != null) {
				totalPaths++;
			}
		}

		return totalPaths;
	}

	private long findMaximumLevel(JSONArray flatItems) {

		long maximumLevel = 0L;

		if (flatItems == null) {
			return maximumLevel;
		}

		for (Object itemObject : flatItems) {

			if (!(itemObject instanceof JSONObject)) {
				continue;
			}

			JSONObject item = (JSONObject) itemObject;

			long level = getLongValue(item.get("level"), 0L);

			if (level > maximumLevel) {
				maximumLevel = level;
			}
		}

		return maximumLevel;
	}

	private long getLongValue(Object value, long defaultValue) {

		if (value == null) {
			return defaultValue;
		}

		if (value instanceof Number) {

			return ((Number) value).longValue();
		}

		try {

			return Long.parseLong(value.toString());

		} catch (NumberFormatException exception) {

			return defaultValue;
		}
	}

	private boolean isSupportedBusinessObject(String type) {

		return "VPMReference".equalsIgnoreCase(type) || "Drawing".equalsIgnoreCase(type);
	}

	private boolean isSupportedRelationship(String type) {

		return "VPMInstance".equalsIgnoreCase(type) || "VPMRepInstance".equalsIgnoreCase(type);
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

	private Object getFirstObject(JSONObject json, String... keys) {

		if (json == null || keys == null) {
			return null;
		}

		for (String key : keys) {

			Object value = json.get(key);

			if (value != null) {
				return value;
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