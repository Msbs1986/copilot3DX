package com.keonys.copilot3DX.controller;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.config.HttpRequestService;
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

	/**
	 * Exemples :
	 *
	 * http://localhost:9090/change-actions/search
	 *
	 * ou
	 *
	 * http://localhost:9090/change-actions/search?searchStr=current="Complete"
	 */
	@GetMapping("/search")
	public String searchChangeActions(@RequestParam(required = false) String searchStr) throws Exception {

		// Authentification 3DEXPERIENCE
		service3DXConnexion.prepareAuthorizationHeaderValue();

		// Création des headers d'authentification
		Map<String, String> headers = service3DXConnexion.createAuthenticatedHeaders();

		 String csrfToken = service3DXConnexion.getCsrfTokenValue();

		headers.put("SecurityContext", secContext);
		headers.put("Accept", "application/json");

		// Critère par défaut
		if (searchStr == null || searchStr.isBlank()) {

			searchStr = "*";
		}

		String encodedSearchStr = URLEncoder.encode(searchStr, StandardCharsets.UTF_8);

		String url = space3dsUrlStr + CHANGE_ACTION_ENDPOINT + "?$searchStr=" + encodedSearchStr;
		
		//String url = space3dsUrlStr + CHANGE_ACTION_ENDPOINT ;

		System.out.println("==========================================");
		System.out.println("3DX Security Context " + secContext);
		System.out.println("3DX CHANGE ACTION SEARCH");
		System.out.println("Search String : " + searchStr);
		System.out.println("URL           : " + url);
		System.out.println("==========================================");

		//url ="https://r1132101389013-eu1-space.3dexperience.3ds.com/enovia/resources/v1/modeler/dslc/changeaction/7B44EFF40E9716006A8EE28A00006059?$fields=realizedChanges";
		HttpResponse<String> response = httpRequestService.loadUrl("GET", "", "", url, headers);

		System.out.println("Status Code : " + response.statusCode());

		if (response.statusCode() != 200) {

			throw new RuntimeException(
					"3DEXPERIENCE Search failed. Status=" + response.statusCode() + " Response=" + response.body());
		}

		return response.body();
	}
}