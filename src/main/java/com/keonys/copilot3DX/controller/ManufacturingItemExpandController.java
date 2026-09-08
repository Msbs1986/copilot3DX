package com.keonys.copilot3DX.controller;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.keonys.copilot3DX.model.ManufacturingItemInfo;
import com.keonys.copilot3DX.service.Service3DXConnexion;

@RestController
@RequestMapping("/manufacturing-items")
public class ManufacturingItemExpandController {

	private static final String SEARCH_ENDPOINT = "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/search";

	private static final String MFG_ITEM_ENDPOINT = "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/";

	private static final String EXPAND_MASK = "dsmfg:MfgItem.ExpandMask.Details.V1";

	@Value("${threedx.space-url}")
	private String space3dsUrlStr;

	@Value("${threedx.security-context}")
	private String secContext;

	private final Service3DXConnexion service3DXConnexion;
	private final HttpRequestService httpRequestService;

	public ManufacturingItemExpandController(Service3DXConnexion service3DXConnexion,
			HttpRequestService httpRequestService) {

		this.service3DXConnexion = service3DXConnexion;
		this.httpRequestService = httpRequestService;
	}

	/**
	 * Recherche les Manufacturing Items à partir d'un critère, effectue un Expand
	 * sur chaque résultat, puis appelle le Get Info sur chaque élément trouvé.
	 *
	 * Exemples :
	 *
	 * GET /manufacturing-items/expand-search
	 *
	 * GET /manufacturing-items/expand-search?searchStr=*
	 *
	 * GET /manufacturing-items/expand-search ?searchStr=ds6w:name:"My Manufacturing
	 * Item"
	 *
	 * GET /manufacturing-items/expand-search ?searchStr=current="Released"
	 */
	@GetMapping(value = "/expand-search", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> expandManufacturingItemsBySearch(@RequestParam(required = false) String searchStr)
			throws Exception {

		/*
		 * 1. Critère de recherche par défaut
		 */
		if (searchStr == null || searchStr.isBlank()) {
			searchStr = "*";
		}

		/*
		 * 2. Préparation de l'authentification 3DEXPERIENCE
		 */
		service3DXConnexion.prepareAuthorizationHeaderValue();

		String csrfToken = service3DXConnexion.getCsrfTokenValueBasic();

		Map<String, String> headers = createSecurityHeaders(csrfToken);

		/*
		 * 3. Recherche des Manufacturing Items racines
		 */
		List<JSONObject> searchResults = searchManufacturingItems(searchStr, headers);

		/*
		 * Évite d'effectuer plusieurs fois le Get Info sur le même physicalId.
		 */
		Map<String, ManufacturingItemInfo> itemDetailsCache = new LinkedHashMap<>();

		List<Map<String, Object>> structures = new ArrayList<>();

		/*
		 * 4. Traitement de chaque résultat de recherche
		 */
		for (JSONObject searchResult : searchResults) {

			String rootPhysicalId = getFirstString(searchResult, "physicalId", "physicalid", "id");

			if (rootPhysicalId.isBlank()) {

				System.err.println("Résultat de recherche sans physicalId : " + searchResult);

				continue;
			}

			System.out.println("==========================================");
			System.out.println("ROOT MANUFACTURING ITEM");
			System.out.println("Physical ID : " + rootPhysicalId);
			System.out.println("==========================================");

			/*
			 * 4.1 Get Info du Manufacturing Item racine
			 */
			ManufacturingItemInfo rootInfo = getManufacturingItemInfo(rootPhysicalId, headers);

			if (rootInfo != null) {
				itemDetailsCache.put(rootPhysicalId, rootInfo);
			}

			/*
			 * 4.2 POST Expand
			 */
			JSONObject expandResponse = expandManufacturingItem(rootPhysicalId, headers);

			/*
			 * 4.3 Extraction récursive des physicalIds présents dans la réponse Expand.
			 */
			Map<String, JSONObject> expandedItems = new LinkedHashMap<>();

			collectManufacturingItems(expandResponse, expandedItems);

			/*
			 * On ajoute aussi explicitement la racine.
			 */
			expandedItems.putIfAbsent(rootPhysicalId, searchResult);

			/*
			 * 4.4 Get Info pour chaque Manufacturing Item
			 */
			List<ManufacturingItemInfo> structureItems = new ArrayList<>();

			for (String physicalId : expandedItems.keySet()) {

				ManufacturingItemInfo itemInfo = itemDetailsCache.get(physicalId);

				if (itemInfo == null) {

					itemInfo = getManufacturingItemInfo(physicalId, headers);

					if (itemInfo != null) {
						itemDetailsCache.put(physicalId, itemInfo);
					}
				}

				if (itemInfo != null) {
					structureItems.add(itemInfo);
				}
			}

			/*
			 * 4.5 Construction de la réponse pour cette structure
			 */
			Map<String, Object> structure = new LinkedHashMap<>();

			structure.put("rootPhysicalId", rootPhysicalId);

			structure.put("root", rootInfo);

			structure.put("itemCount", structureItems.size());

			structure.put("items", structureItems);

			/*
			 * Utile pour analyser la réponse réelle de 3DEXPERIENCE. Tu pourras retirer ce
			 * champ après validation du POC.
			 */
			structure.put("rawExpandResponse", expandResponse);

			structures.add(structure);
		}

		/*
		 * 5. Construction de la réponse finale
		 */
		Map<String, Object> response = new LinkedHashMap<>();

		response.put("success", true);

		response.put("status", 200);

		response.put("searchCriteria", searchStr);

		response.put("rootItemCount", searchResults.size());

		response.put("uniqueItemCount", itemDetailsCache.size());

		response.put("structures", structures);

		return response;
	}

	/**
	 * Recherche les Manufacturing Items.
	 */
	private List<JSONObject> searchManufacturingItems(String searchStr, Map<String, String> headers) throws Exception {

		String encodedSearchStr = URLEncoder.encode(searchStr, StandardCharsets.UTF_8);

		String searchUrl = space3dsUrlStr + SEARCH_ENDPOINT + "?$searchStr=" + encodedSearchStr + "&$top=50";

//		System.out.println("==========================================");
//		System.out.println("3DX MANUFACTURING ITEM SEARCH");
//		System.out.println("Search String : " + searchStr);
//		System.out.println("URL           : " + searchUrl);
//		System.out.println("==========================================");

		HttpResponse<String> searchResponse = httpRequestService.loadUrl("GET", "", "", searchUrl, headers);

		System.out.println("Search Status : " + searchResponse.statusCode());

		if (searchResponse.statusCode() != 200) {

			throw new RuntimeException("3DEXPERIENCE Manufacturing Item Search failed. " + "Status="
					+ searchResponse.statusCode() + " Response=" + searchResponse.body());
		}

		JSONParser parser = new JSONParser();

		JSONObject searchJson = (JSONObject) parser.parse(searchResponse.body());

		JSONArray manufacturingItems = findManufacturingItemsArray(searchJson);

		List<JSONObject> results = new ArrayList<>();

		if (manufacturingItems != null) {

			for (Object object : manufacturingItems) {

				if (object instanceof JSONObject) {
					results.add((JSONObject) object);
				}
			}
		}

		System.out.println("Search results : " + results.size());

		return results;
	}

	/**
	 * Effectue le POST Expand.
	 *
	 * Body envoyé :
	 *
	 * { "expandDepth": -1, "withPath": true }
	 */
	@SuppressWarnings("unchecked")
	private JSONObject expandManufacturingItem(String physicalId, Map<String, String> headers) throws Exception {

		String encodedPhysicalId = URLEncoder.encode(physicalId, StandardCharsets.UTF_8);

		/*
		 * Le nom du mask contient des caractères : et . Il peut généralement être
		 * conservé tel quel.
		 */
		String expandUrl = space3dsUrlStr + MFG_ITEM_ENDPOINT + encodedPhysicalId + "/expand?$mask=" + EXPAND_MASK;

		/*
		 * Construction du body JSON.
		 */
		JSONObject expandBody = new JSONObject();

		expandBody.put("expandDepth", -1);

		expandBody.put("withPath", Boolean.TRUE);

		String requestBody = expandBody.toJSONString();

//		System.out.println("------------------------------------------");
//		System.out.println("3DX MANUFACTURING ITEM EXPAND");
//		System.out.println("Physical ID : " + physicalId);
//		System.out.println("URL         : " + expandUrl);
//		System.out.println("Body        : " + requestBody);
//		System.out.println("------------------------------------------");

		
		HttpResponse<String> expandResponse = httpRequestService.loadUrl("POST", "application/json",requestBody, expandUrl, headers);
		System.out.println("Expand Status : " + expandResponse.statusCode());

		/*
		 * Selon l'environnement, un POST peut répondre 200 ou 201.
		 */
		if (expandResponse.statusCode() != 200 && expandResponse.statusCode() != 201) {

			throw new RuntimeException("3DEXPERIENCE Manufacturing Item Expand failed. " + "PhysicalId=" + physicalId
					+ " Status=" + expandResponse.statusCode() + " Response=" + expandResponse.body());
		}

		if (expandResponse.body() == null || expandResponse.body().isBlank()) {

			return new JSONObject();
		}

		JSONParser parser = new JSONParser();

		Object parsedResponse = parser.parse(expandResponse.body());

		/*
		 * Cas standard : la réponse est un JSONObject.
		 */
		if (parsedResponse instanceof JSONObject) {
			return (JSONObject) parsedResponse;
		}

		/*
		 * Cas alternatif : la réponse racine est un JSONArray.
		 */
		if (parsedResponse instanceof JSONArray) {

			JSONObject wrapper = new JSONObject();

			wrapper.put("member", parsedResponse);

			return wrapper;
		}

		return new JSONObject();
	}

	/**
	 * Effectue le Get Info d'un Manufacturing Item.
	 */
	private ManufacturingItemInfo getManufacturingItemInfo(String physicalId, Map<String, String> headers) {

		try {

			String encodedPhysicalId = URLEncoder.encode(physicalId, StandardCharsets.UTF_8);

			String detailUrl = space3dsUrlStr + MFG_ITEM_ENDPOINT + encodedPhysicalId;

//			System.out.println("------------------------------------------");
//			System.out.println("3DX MANUFACTURING ITEM GET INFO");
//			System.out.println("Physical ID : " + physicalId);
//			System.out.println("URL         : " + detailUrl);
//			System.out.println("------------------------------------------");

			HttpResponse<String> detailResponse = httpRequestService.loadUrl("GET", "", "", detailUrl, headers);

			System.out.println("Detail Status : " + detailResponse.statusCode());

			if (detailResponse.statusCode() != 200) {

				System.err.println("Get Manufacturing Item Info failed. " + "PhysicalId=" + physicalId + " Status="
						+ detailResponse.statusCode() + " Response=" + detailResponse.body());

				return null;
			}

			JSONParser parser = new JSONParser();

			JSONObject detailJson = (JSONObject) parser.parse(detailResponse.body());

			JSONObject manufacturingItemJson = extractDetailObject(detailJson);

			return mapManufacturingItem(manufacturingItemJson, null);

		} catch (Exception exception) {

			System.err.println("Error while getting Manufacturing Item " + physicalId + " : " + exception.getMessage());

			return null;
		}
	}

	/**
	 * Parcourt récursivement toute la réponse Expand et récupère les objets
	 * possédant un physicalId.
	 */
	private void collectManufacturingItems(Object jsonValue, Map<String, JSONObject> collectedItems) {

		if (jsonValue == null) {
			return;
		}

		if (jsonValue instanceof JSONObject) {

			JSONObject jsonObject = (JSONObject) jsonValue;

			String physicalId = getFirstString(jsonObject, "physicalId", "physicalid", "id");

			if (!physicalId.isBlank() && looksLikeManufacturingItem(jsonObject)) {

				collectedItems.putIfAbsent(physicalId, jsonObject);
			}

			/*
			 * Parcours récursif de toutes les propriétés.
			 */
			for (Object childValue : jsonObject.values()) {

				collectManufacturingItems(childValue, collectedItems);
			}

			return;
		}

		if (jsonValue instanceof JSONArray) {

			JSONArray jsonArray = (JSONArray) jsonValue;

			for (Object childValue : jsonArray) {

				collectManufacturingItems(childValue, collectedItems);
			}
		}
	}

	/**
	 * Vérifie si l'objet JSON semble représenter un Manufacturing Item et non une
	 * relation d'instance.
	 */
	private boolean looksLikeManufacturingItem(JSONObject jsonObject) {

		String type = getFirstString(jsonObject, "type", "typeName", "@type");

		if (type.isBlank()) {

			return jsonObject.containsKey("physicalId") || jsonObject.containsKey("physicalid");
		}

		String normalizedType = type.toLowerCase();

		return normalizedType.contains("mfgitem") || normalizedType.contains("manufacturing")
				|| normalizedType.contains("process") || normalizedType.contains("operation");
	}

	/**
	 * Recherche le tableau contenant les résultats de l'API Search.
	 */
	private JSONArray findManufacturingItemsArray(JSONObject searchJson) {

		String[] possibleKeys = { "manufacturingItem", "manufacturingItems", "mfgItem", "mfgItems", "member", "members",
				"items", "results" };

		for (String key : possibleKeys) {

			Object value = searchJson.get(key);

			if (value instanceof JSONArray) {

				System.out.println("Manufacturing Item array found with key: " + key);

				return (JSONArray) value;
			}
		}

		System.err.println("No Manufacturing Item array found. " + "Available keys: " + searchJson.keySet());

		return null;
	}

	/**
	 * Extrait l'objet principal de la réponse Get Info.
	 */
	private JSONObject extractDetailObject(JSONObject detailJson) {

		if (detailJson == null) {
			return new JSONObject();
		}

		String[] possibleKeys = { "manufacturingItem", "mfgItem", "data", "member" };

		for (String key : possibleKeys) {

			Object value = detailJson.get(key);

			if (value instanceof JSONObject) {
				return (JSONObject) value;
			}

			if (value instanceof JSONArray) {

				JSONArray array = (JSONArray) value;

				if (!array.isEmpty() && array.get(0) instanceof JSONObject) {

					return (JSONObject) array.get(0);
				}
			}
		}

		return detailJson;
	}

	/**
	 * Transforme la réponse 3DEXPERIENCE en ManufacturingItemInfo.
	 */
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

		String value = getFirstString(detailJson, keys);

		if (!value.isBlank()) {
			return value;
		}

		return getFirstString(searchJson, keys);
	}

	private String getFirstString(JSONObject json, String... keys) {

		if (json == null || keys == null) {
			return "";
		}

		for (String key : keys) {

			Object value = json.get(key);

			if (value != null) {

				String stringValue = value.toString();

				if (!stringValue.isBlank()) {
					return stringValue;
				}
			}
		}

		return "";
	}

	/**
	 * Construit les headers utilisés pour Search, Expand et Get Info.
	 */
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