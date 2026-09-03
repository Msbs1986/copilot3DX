package com.keonys.copilot3DX.controller;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import com.keonys.copilot3DX.dto.ChangeActionDto;
import com.keonys.copilot3DX.dto.ChangeActionInfo;
import com.keonys.copilot3DX.service.Service3DXConnexion;

@RestController
@RequestMapping("/change-actions")
public class ChangeActionController {

	@Value("${threedx.space-url}")
	private String space3dsUrlStr;

	@Value("${threedx.security-context}")
	private String secContext;

	private final Service3DXConnexion service3DXConnexion;
	private final HttpRequestService httpRequestService;

	private static final String CHANGE_ACTION_ENDPOINT = "/resources/v1/modeler/dslc/changeaction/search";

	public ChangeActionController(Service3DXConnexion service3DXConnexion, HttpRequestService httpRequestService) {

		this.service3DXConnexion = service3DXConnexion;
		this.httpRequestService = httpRequestService;
	}

	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ChangeActionDto searchChangeActions(@RequestParam(required = false) String searchStr) throws Exception {

		service3DXConnexion.prepareAuthorizationHeaderValue();

		Map<String, String> headers = service3DXConnexion.createAuthenticatedHeaders();

		String csrf = service3DXConnexion.getCsrfTokenValue();

		headers.put("SecurityContext", secContext);
		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

		if (searchStr == null || searchStr.isBlank()) {
			searchStr = "*";
		}

		String encodedSearchStr = URLEncoder.encode(searchStr, StandardCharsets.UTF_8);

		String searchUrl = space3dsUrlStr + CHANGE_ACTION_ENDPOINT + "?$searchStr=" + encodedSearchStr;

		System.out.println("==========================================");
		System.out.println("3DX CHANGE ACTION SEARCH");
		System.out.println("Search String : " + searchStr);
		System.out.println("URL           : " + searchUrl);
		System.out.println("==========================================");

		HttpResponse<String> searchResponse = httpRequestService.loadUrl("GET", "", "", searchUrl, headers);

		System.out.println("Search Status : " + searchResponse.statusCode());

		if (searchResponse.statusCode() != 200) {

			throw new RuntimeException("3DEXPERIENCE Search failed. Status=" + searchResponse.statusCode()
					+ " Response=" + searchResponse.body());
		}

		JSONParser parser = new JSONParser();

		JSONObject searchJson = (JSONObject) parser.parse(searchResponse.body());

		JSONArray changeActions = (JSONArray) searchJson.get("changeAction");

		List<ChangeActionInfo> result = new ArrayList<>();

		if (changeActions != null) {

			for (Object obj : changeActions) {

				JSONObject ca = (JSONObject) obj;

				String relativePath = (String) ca.get("relativePath");

				if (relativePath == null || relativePath.isBlank()) {
					continue;
				}

				String detailUrl = space3dsUrlStr + relativePath;

				System.out.println("------------------------------------------");
				System.out.println("Detail URL : " + detailUrl);

				try {

					HttpResponse<String> detailResponse = httpRequestService.loadUrl("GET", "", "", detailUrl, headers);

					if (detailResponse.statusCode() != 200) {

						System.out.println("Failed : " + detailResponse.statusCode());

						continue;
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

					result.add(info);

				} catch (Exception ex) {

					System.err.println("Error while processing " + relativePath + " : " + ex.getMessage());

				}
			}
		}

		return new ChangeActionDto(true, 200, searchStr, result);
	}

	private String getString(JSONObject json, String key) {

		Object value = json.get(key);

		return value == null ? "" : value.toString();
	}
}