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

		/*
		 * Obligatoire pour essayer de récupérer les chemins permettant de reconstruire
		 * les niveaux de l'EBOM.
		 */
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

		Map<String, String> businessObjectIdByCestamp = new HashMap<>();

		Map<String, List<String>> childrenByParent = new LinkedHashMap<>();

		JSONArray unresolvedRelationships = new JSONArray();

		if (members != null) {

			collectBusinessObjects(members, businessObjects, businessObjectIdByCestamp);

			collectRelationships(members, relationships);

			buildHierarchyRelations(members, rootPhysicalId, businessObjects, businessObjectIdByCestamp,
					childrenByParent, unresolvedRelationships);
		}

		JSONObject root = buildHierarchyNode(rootPhysicalId, "", businessObjects, childrenByParent, new HashSet<>(), 0);

		JSONArray flatItems = new JSONArray();

		buildFlatHierarchy(root, flatItems);

		JSONArray flatRelationships = new JSONArray();

		for (JSONObject relationship : relationships.values()) {

			flatRelationships.add(simplifyRelationship(relationship, businessObjectIdByCestamp));
		}

		JSONObject response = new JSONObject();

		response.put("success", true);

		response.put("source", "3DEXPERIENCE_ENGINEERING_EXPAND");

		response.put("structureType", "EBOM");

		response.put("searchCriteria", searchStr);

		response.put("rootPhysicalId", rootPhysicalId);

		response.put("totalBusinessObjects", businessObjects.size());

		response.put("totalRelationships", relationships.size());

		response.put("totalStructureItems", flatItems.size());

		response.put("root", root);

		response.put("items", flatItems);

		response.put("relationships", flatRelationships);

		response.put("unresolvedRelationships", unresolvedRelationships);

		boolean structureReliable = unresolvedRelationships.isEmpty();

		response.put("structureReliable", structureReliable);

		if (!structureReliable) {

			response.put("warning", "Some relationships do not contain sufficient "
					+ "parent or path information to determine " + "their exact level in the EBOM.");
		}

		return response;
	}

	private void collectBusinessObjects(JSONArray members, Map<String, JSONObject> businessObjects,
			Map<String, String> businessObjectIdByCestamp) {

		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject member = (JSONObject) memberObject;

			String type = getFirstString(member, "type");

			if (!isSupportedBusinessObject(type)) {
				continue;
			}

			String id = getFirstString(member, "physicalId", "physicalid", "id");

			if (id.isBlank()) {
				continue;
			}

			businessObjects.putIfAbsent(id, member);

			String cestamp = getFirstString(member, "cestamp");

			if (!cestamp.isBlank()) {

				businessObjectIdByCestamp.putIfAbsent(cestamp, id);
			}
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

			String id = getFirstString(member, "physicalId", "physicalid", "id");

			if (!id.isBlank()) {

				relationships.putIfAbsent(id, member);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void buildHierarchyRelations(JSONArray members, String rootPhysicalId,
			Map<String, JSONObject> businessObjects, Map<String, String> businessObjectIdByCestamp,
			Map<String, List<String>> childrenByParent, JSONArray unresolvedRelationships) {

		/*
		 * Première tentative : lecture des objets contenant directement un path.
		 * Certaines versions 3DEXPERIENCE retournent des membres dédiés au chemin sans
		 * type métier classique.
		 */
		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject member = (JSONObject) memberObject;

			buildRelationsFromPathObject(member, rootPhysicalId, businessObjects, businessObjectIdByCestamp,
					childrenByParent);
		}

		/*
		 * Deuxième tentative : lecture des VPMInstance et VPMRepInstance.
		 */
		for (Object memberObject : members) {

			if (!(memberObject instanceof JSONObject)) {
				continue;
			}

			JSONObject relationship = (JSONObject) memberObject;

			String relationshipType = getFirstString(relationship, "type");

			if (!isSupportedRelationship(relationshipType)) {

				continue;
			}

			String childBusinessObjectId = resolveChildBusinessObjectId(relationship, businessObjectIdByCestamp);

			if (childBusinessObjectId.isBlank()) {

				unresolvedRelationships.add(
						createUnresolvedRelationship(relationship, "", "Unable to resolve the child Business Object."));

				continue;
			}

			String parentBusinessObjectId = resolveParentBusinessObjectId(relationship);

			if (!parentBusinessObjectId.isBlank()) {

				addChildRelation(childrenByParent, parentBusinessObjectId, childBusinessObjectId);

				continue;
			}

			List<String> pathIds = extractBusinessObjectPath(relationship.get("path"), businessObjects,
					businessObjectIdByCestamp);

			if (!pathIds.isEmpty()) {

				completeAndAddPath(pathIds, rootPhysicalId, childBusinessObjectId, childrenByParent);

				continue;
			}

			/*
			 * Si une autre entrée path a déjà créé la relation, l'occurrence est considérée
			 * comme résolue.
			 */
			if (isChildAlreadyLinked(childrenByParent, childBusinessObjectId)) {

				continue;
			}

			unresolvedRelationships.add(createUnresolvedRelationship(relationship, childBusinessObjectId,
					"No parent or path information was returned."));
		}
	}

	private void buildRelationsFromPathObject(JSONObject member, String rootPhysicalId,
			Map<String, JSONObject> businessObjects, Map<String, String> businessObjectIdByCestamp,
			Map<String, List<String>> childrenByParent) {

		String parentId = resolveParentBusinessObjectId(member);

		String childId = resolveChildBusinessObjectId(member, businessObjectIdByCestamp);

		if (!parentId.isBlank() && !childId.isBlank()) {

			addChildRelation(childrenByParent, parentId, childId);
		}

		List<String> pathIds = extractBusinessObjectPath(member.get("path"), businessObjects,
				businessObjectIdByCestamp);

		if (pathIds.isEmpty()) {
			return;
		}

		if (!rootPhysicalId.equals(pathIds.get(0))) {

			pathIds.add(0, rootPhysicalId);
		}

		if (!childId.isBlank() && !pathIds.contains(childId)) {

			pathIds.add(childId);
		}

		addPathRelations(pathIds, childrenByParent);
	}

	private String resolveParentBusinessObjectId(JSONObject relationship) {

		return getFirstString(relationship, "parent", "parentId", "parentPhysicalId", "parentReference",
				"parentReferenceId", "parentphysicalid");
	}

	private String resolveChildBusinessObjectId(JSONObject relationship,
			Map<String, String> businessObjectIdByCestamp) {

		String referenceId = getFirstString(relationship, "reference", "referenceId", "referencePhysicalId", "child",
				"childId", "childPhysicalId", "childReference", "childReferenceId");

		if (!referenceId.isBlank()) {
			return referenceId;
		}

		String cestamp = getFirstString(relationship, "cestamp");

		if (cestamp.isBlank()) {
			return "";
		}

		return businessObjectIdByCestamp.getOrDefault(cestamp, "");
	}

	private void completeAndAddPath(List<String> pathIds, String rootPhysicalId, String childBusinessObjectId,
			Map<String, List<String>> childrenByParent) {

		if (!rootPhysicalId.equals(pathIds.get(0))) {

			pathIds.add(0, rootPhysicalId);
		}

		if (!pathIds.contains(childBusinessObjectId)) {

			pathIds.add(childBusinessObjectId);
		}

		addPathRelations(pathIds, childrenByParent);
	}

	private List<String> extractBusinessObjectPath(Object pathObject, Map<String, JSONObject> businessObjects,
			Map<String, String> businessObjectIdByCestamp) {

		List<String> pathIds = new ArrayList<>();

		if (pathObject == null) {
			return pathIds;
		}

		if (pathObject instanceof JSONArray) {

			JSONArray pathArray = (JSONArray) pathObject;

			for (Object pathElement : pathArray) {

				String resolvedId = resolvePathElement(pathElement, businessObjects, businessObjectIdByCestamp);

				addUniquePathId(pathIds, resolvedId);
			}

			return pathIds;
		}

		if (pathObject instanceof JSONObject) {

			JSONObject pathJson = (JSONObject) pathObject;

			Object membersObject = getFirstObject(pathJson, "member", "members", "items", "path");

			if (membersObject != null && membersObject != pathObject) {

				return extractBusinessObjectPath(membersObject, businessObjects, businessObjectIdByCestamp);
			}

			String resolvedId = resolvePathElement(pathJson, businessObjects, businessObjectIdByCestamp);

			addUniquePathId(pathIds, resolvedId);

			return pathIds;
		}

		if (pathObject instanceof String) {

			String pathString = pathObject.toString().trim();

			if (pathString.isBlank()) {
				return pathIds;
			}

			String normalizedPath = pathString.replace("[", "").replace("]", "").replace("\"", "");

			String[] pathParts = normalizedPath.split("[/,;>|]");

			for (String pathPart : pathParts) {

				String candidate = pathPart.trim();

				if (businessObjects.containsKey(candidate)) {

					addUniquePathId(pathIds, candidate);
				}
			}
		}

		return pathIds;
	}

	private String resolvePathElement(Object pathElement, Map<String, JSONObject> businessObjects,
			Map<String, String> businessObjectIdByCestamp) {

		if (pathElement == null) {
			return "";
		}

		if (pathElement instanceof String) {

			String candidate = pathElement.toString().trim();

			if (businessObjects.containsKey(candidate)) {

				return candidate;
			}

			return "";
		}

		if (!(pathElement instanceof JSONObject)) {
			return "";
		}

		JSONObject pathJson = (JSONObject) pathElement;

		String id = getFirstString(pathJson, "physicalId", "physicalid", "id", "reference", "referenceId", "child",
				"childId");

		if (businessObjects.containsKey(id)) {
			return id;
		}

		String cestamp = getFirstString(pathJson, "cestamp");

		if (cestamp.isBlank()) {
			return "";
		}

		return businessObjectIdByCestamp.getOrDefault(cestamp, "");
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

	private void addUniquePathId(List<String> pathIds, String physicalId) {

		if (physicalId == null || physicalId.isBlank()) {

			return;
		}

		if (!pathIds.contains(physicalId)) {
			pathIds.add(physicalId);
		}
	}

	private void addPathRelations(List<String> pathIds, Map<String, List<String>> childrenByParent) {

		if (pathIds == null || pathIds.size() < 2) {

			return;
		}

		for (int index = 0; index < pathIds.size() - 1; index++) {

			String parentId = pathIds.get(index);

			String childId = pathIds.get(index + 1);

			addChildRelation(childrenByParent, parentId, childId);
		}
	}

	private void addChildRelation(Map<String, List<String>> childrenByParent, String parentId, String childId) {

		if (parentId == null || parentId.isBlank() || childId == null || childId.isBlank()
				|| parentId.equals(childId)) {

			return;
		}

		List<String> children = childrenByParent.computeIfAbsent(parentId, key -> new ArrayList<>());

		if (!children.contains(childId)) {
			children.add(childId);
		}
	}

	private boolean isChildAlreadyLinked(Map<String, List<String>> childrenByParent, String childId) {

		for (List<String> children : childrenByParent.values()) {

			if (children.contains(childId)) {
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	private JSONObject buildHierarchyNode(String physicalId, String parentPhysicalId,
			Map<String, JSONObject> businessObjects, Map<String, List<String>> childrenByParent, Set<String> visited,
			int level) {

		JSONObject sourceItem = businessObjects.get(physicalId);

		JSONObject node;

		if (sourceItem != null) {

			node = simplifyBusinessObject(sourceItem);

		} else {

			node = new JSONObject();

			node.put("physicalId", physicalId);

			node.put("name", "");
			node.put("title", "");
			node.put("type", "");
			node.put("revision", "");
			node.put("state", "");
		}

		node.put("parentPhysicalId", parentPhysicalId);

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

				JSONObject childNode = buildHierarchyNode(childId, physicalId, businessObjects, childrenByParent,
						branchVisited, level + 1);

				children.add(childNode);
			}
		}

		node.put("children", children);

		node.put("childrenCount", children.size());

		return node;
	}

	@SuppressWarnings("unchecked")
	private void buildFlatHierarchy(JSONObject node, JSONArray flatItems) {

		if (node == null) {
			return;
		}

		JSONObject flatItem = new JSONObject();

		flatItem.put("physicalId", getFirstString(node, "physicalId"));

		flatItem.put("parentPhysicalId", getFirstString(node, "parentPhysicalId"));

		flatItem.put("name", getFirstString(node, "name"));

		flatItem.put("title", getFirstString(node, "title"));

		flatItem.put("type", getFirstString(node, "type"));

		flatItem.put("revision", getFirstString(node, "revision"));

		flatItem.put("state", getFirstString(node, "state"));

		flatItem.put("owner", getFirstString(node, "owner"));

		flatItem.put("organization", getFirstString(node, "organization"));

		flatItem.put("collabSpace", getFirstString(node, "collabSpace"));

		flatItem.put("level", node.getOrDefault("level", Long.valueOf(0)));

		flatItem.put("childrenCount", node.getOrDefault("childrenCount", Long.valueOf(0)));

		flatItems.add(flatItem);

		Object childrenObject = node.get("children");

		if (!(childrenObject instanceof JSONArray)) {
			return;
		}

		JSONArray children = (JSONArray) childrenObject;

		for (Object childObject : children) {

			if (childObject instanceof JSONObject) {

				buildFlatHierarchy((JSONObject) childObject, flatItems);
			}
		}
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
	private JSONObject simplifyRelationship(JSONObject source, Map<String, String> businessObjectIdByCestamp) {

		JSONObject relationship = new JSONObject();

		relationship.put("physicalId", getFirstString(source, "physicalId", "physicalid", "id"));

		relationship.put("name", getFirstString(source, "name"));

		relationship.put("type", getFirstString(source, "type"));

		relationship.put("parentBusinessObjectPhysicalId", resolveParentBusinessObjectId(source));

		relationship.put("childBusinessObjectPhysicalId",
				resolveChildBusinessObjectId(source, businessObjectIdByCestamp));

		relationship.put("cestamp", getFirstString(source, "cestamp"));

		relationship.put("created", getFirstString(source, "created"));

		relationship.put("modified", getFirstString(source, "modified"));

		return relationship;
	}

	@SuppressWarnings("unchecked")
	private JSONObject createUnresolvedRelationship(JSONObject relationship, String resolvedChildPhysicalId,
			String reason) {

		JSONObject unresolved = new JSONObject();

		unresolved.put("physicalId", getFirstString(relationship, "physicalId", "physicalid", "id"));

		unresolved.put("name", getFirstString(relationship, "name"));

		unresolved.put("type", getFirstString(relationship, "type"));

		unresolved.put("cestamp", getFirstString(relationship, "cestamp"));

		unresolved.put("resolvedChildPhysicalId", resolvedChildPhysicalId);

		unresolved.put("reason", reason);

		return unresolved;
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