package com.keonys.copilot3DX.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.config.HttpRequestService;
import com.keonys.copilot3DX.dto.ManufacturingItemDto;
import com.keonys.copilot3DX.model.ManufacturingItemInfo;
import com.keonys.copilot3DX.service.Service3DXConnexion;

@RestController
@RequestMapping("/manufacturing-items")
public class ManufacturingItemController {

	@Value("${threedx.space-url}")
	private String space3dsUrlStr;

	@Value("${threedx.security-context}")
	private String secContext;

	private final Service3DXConnexion service3DXConnexion;
	private final HttpRequestService httpRequestService;

	private static final String MANUFACTURING_ITEM_SEARCH_ENDPOINT = "/resources/v1/modeler/dsmfg/dsmfg:MfgItem/search";

	public ManufacturingItemController(Service3DXConnexion service3DXConnexion, HttpRequestService httpRequestService) {

		this.service3DXConnexion = service3DXConnexion;
		this.httpRequestService = httpRequestService;
	}

	/**
	 * Recherche les Manufacturing Items dans 3DEXPERIENCE, puis appelle le Get Info
	 * de chaque résultat.
	 *
	 * Exemple :
	 *
	 * GET /manufacturing-items/search
	 *
	 * GET /manufacturing-items/search?searchStr=*
	 *
	 * GET /manufacturing-items/search
	 * ?searchStr=ds6w:modified>"2026-07-01T13:40:00" AND current="Released"
	 */
	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ManufacturingItemDto searchManufacturingItems(@RequestParam(required = false) String searchStr)
			throws Exception {

		/*
		 * 1. Préparation de l'authentification 3DEXPERIENCE
		 */

//		service3DXConnexion.prepareAuthorizationHeaderValue();
//		Map<String, String> headers = service3DXConnexion.createAuthenticatedHeaders();
//		String csrf = service3DXConnexion.getCsrfTokenValue();
//		headers.put("SecurityContext", secContext);
//		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
		
		service3DXConnexion.prepareAuthorizationHeaderValue();
		String csrfToken = service3DXConnexion.getCsrfTokenValueBasic();
		Map<String, String> headers = createSecurityHeaders();


		/*
		 * 2. Critère de recherche par défaut
		 */
		if (searchStr == null || searchStr.isBlank()) {
			searchStr = "*";
		}

		String encodedSearchStr = URLEncoder.encode(searchStr, StandardCharsets.UTF_8);

		String searchUrl = space3dsUrlStr + MANUFACTURING_ITEM_SEARCH_ENDPOINT + "?$searchStr=" + encodedSearchStr+"&$top=100";

		System.out.println("==========================================");
		System.out.println("3DX MANUFACTURING ITEM SEARCH");
		System.out.println("Search String : " + searchStr);
		System.out.println("URL           : " + searchUrl);
		System.out.println("==========================================");

		/*
		 * 3. Appel de l'API Search
		 */
		HttpResponse<String> searchResponse = httpRequestService.loadUrl("GET", "", "", searchUrl, headers);

		System.out.println("Search Status : " + searchResponse.statusCode());

		if (searchResponse.statusCode() != 200) {

			throw new RuntimeException("3DEXPERIENCE Manufacturing Item Search failed. " + "Status="
					+ searchResponse.statusCode() + " Response=" + searchResponse.body());
		}

		/*
		 * 4. Parsing de la réponse Search
		 */
		JSONParser parser = new JSONParser();

		JSONObject searchJson = (JSONObject) parser.parse(searchResponse.body());

		JSONArray manufacturingItems = findManufacturingItemsArray(searchJson);

		List<ManufacturingItemInfo> result = new ArrayList<>();

		/*
		 * 5. Pour chaque résultat, appel de l'URL Get Info
		 */
		if (manufacturingItems != null) {

			for (Object obj : manufacturingItems) {

				if (!(obj instanceof JSONObject)) {
					continue;
				}

				JSONObject manufacturingItemSearchResult = (JSONObject) obj;

				String relativePath = getString(manufacturingItemSearchResult, "relativePath");

				String physicalId = getFirstString(manufacturingItemSearchResult, "physicalId", "physicalid", "id");

				System.out.println("------------------------------------------");
				System.out.println("Manufacturing Item Physical ID : " + physicalId);
				System.out.println("Relative Path                  : " + relativePath);

				/*
				 * Si relativePath est présent, on utilise directement le chemin fourni par
				 * 3DEXPERIENCE.
				 *
				 * Sinon, on reconstruit l'URL avec le physicalId.
				 */
				String detailUrl = buildDetailUrl(relativePath, physicalId);

				if (detailUrl == null) {

					System.err.println("Impossible de construire le Get Info URL.");

					continue;
				}

				System.out.println("Detail URL                     : " + detailUrl);

				try {

					HttpResponse<String> detailResponse = httpRequestService.loadUrl("GET", "", "", detailUrl, headers);

					System.out.println("Detail Status                 : " + detailResponse.statusCode());

					if (detailResponse.statusCode() != 200) {

						System.err.println("Get Manufacturing Item Info failed. " + "Status="
								+ detailResponse.statusCode() + " Response=" + detailResponse.body());

						continue;
					}

					JSONObject detailJson = (JSONObject) parser.parse(detailResponse.body());

					JSONObject manufacturingItemJson = extractDetailObject(detailJson);

					ManufacturingItemInfo info = mapManufacturingItem(manufacturingItemJson,
							manufacturingItemSearchResult);

					result.add(info);

				} catch (Exception ex) {

					System.err.println(
							"Error while processing Manufacturing Item " + physicalId + " : " + ex.getMessage());
				}
			}
		}

		System.out.println("==========================================");
		System.out.println("Manufacturing Items returned : " + result.size());
		System.out.println("==========================================");

		/*
		 * 6. Réponse simplifiée pour Copilot Studio
		 */
		return new ManufacturingItemDto(true, 200, searchStr, result);
	}

	/**
	 * Endpoint permettant d'appeler directement le Get Info à partir d'un
	 * physicalId.
	 *
	 * Exemple :
	 *
	 * GET /manufacturing-items/info ?physicalId=7B44EFF4B97739006A8BF29400001F6F
	 */
	@GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public ManufacturingItemInfo getManufacturingItemInfo(@RequestParam String physicalId) throws Exception {

		if (physicalId == null || physicalId.isBlank()) {
			throw new IllegalArgumentException("Le paramètre physicalId est obligatoire.");
		}

		service3DXConnexion.prepareAuthorizationHeaderValue();

		Map<String, String> headers = service3DXConnexion.createAuthenticatedHeaders();

		headers.put("SecurityContext", secContext);
		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

		String detailUrl = space3dsUrlStr + "/resources/v1/modeler/dsmfg/" + "dsmfg:MfgItem/"
				+ URLEncoder.encode(physicalId, StandardCharsets.UTF_8);

		System.out.println("==========================================");
		System.out.println("3DX MANUFACTURING ITEM GET INFO");
		System.out.println("Physical ID : " + physicalId);
		System.out.println("URL         : " + detailUrl);
		System.out.println("==========================================");

		HttpResponse<String> detailResponse = httpRequestService.loadUrl("GET", "", "", detailUrl, headers);

		System.out.println("Detail Status : " + detailResponse.statusCode());

		if (detailResponse.statusCode() != 200) {

			throw new RuntimeException("3DEXPERIENCE Manufacturing Item Get Info failed. " + "Status="
					+ detailResponse.statusCode() + " Response=" + detailResponse.body());
		}

		JSONParser parser = new JSONParser();

		JSONObject detailJson = (JSONObject) parser.parse(detailResponse.body());

		JSONObject manufacturingItemJson = extractDetailObject(detailJson);

		return mapManufacturingItem(manufacturingItemJson, null);
	}

	/**
	 * Construit l'URL Get Info.
	 */
	private String buildDetailUrl(String relativePath, String physicalId) {

		if (relativePath != null && !relativePath.isBlank()) {

			if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {

				return relativePath;
			}

			if (!relativePath.startsWith("/")) {
				relativePath = "/" + relativePath;
			}

			return space3dsUrlStr + relativePath;
		}

		if (physicalId != null && !physicalId.isBlank()) {

			return space3dsUrlStr + "/resources/v1/modeler/dsmfg/" + "dsmfg:MfgItem/"
					+ URLEncoder.encode(physicalId, StandardCharsets.UTF_8);
		}

		return null;
	}

	/**
	 * Recherche le tableau contenant les Manufacturing Items.
	 *
	 * Les noms possibles sont gardés temporairement pour rendre le POC compatible
	 * avec plusieurs formes de réponses 3DX.
	 *
	 * Une fois la réponse réelle connue, les clés inutiles pourront être
	 * supprimées.
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
	 * Certaines APIs retournent directement l'objet. D'autres le retournent sous
	 * member, data ou mfgItem.
	 */
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

				JSONArray array = (JSONArray) value;

				if (!array.isEmpty() && array.get(0) instanceof JSONObject) {

					return (JSONObject) array.get(0);
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

	/**
	 * Transforme la réponse 3DEXPERIENCE en modèle simplifié.
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