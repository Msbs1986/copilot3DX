package com.keonys.copilot3DX.controller;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.config.HttpRequestService;
import com.keonys.copilot3DX.dto.ChangeActionDto;
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

	@GetMapping(
	        value = "/search",
	        produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ChangeActionDto searchChangeActions(
	        @RequestParam(required = false) String searchStr) throws Exception {

	    service3DXConnexion.prepareAuthorizationHeaderValue();

	    Map<String, String> headers =
	            service3DXConnexion.createAuthenticatedHeaders();
	    
	    String csrfToken = service3DXConnexion.getCsrfTokenValue();

	    headers.put("SecurityContext", secContext);
	    headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

	    if (searchStr == null || searchStr.isBlank()) {
	        searchStr = "*";
	    }

	    String encodedSearchStr =
	            URLEncoder.encode(searchStr, StandardCharsets.UTF_8);

	    String url = space3dsUrlStr
	            + CHANGE_ACTION_ENDPOINT
	            + "?$searchStr="
	            + encodedSearchStr;

	    System.out.println("==========================================");
	    System.out.println("3DX Security Context : " + secContext);
	    System.out.println("3DX CHANGE ACTION SEARCH");
	    System.out.println("Search String : " + searchStr);
	    System.out.println("URL           : " + url);
	    System.out.println("==========================================");

	    HttpResponse<String> response =
	            httpRequestService.loadUrl(
	                    "GET",
	                    "",
	                    "",
	                    url,
	                    headers
	            );

	    System.out.println("Status Code : " + response.statusCode());

	    if (response.statusCode() != 200) {
	        throw new RuntimeException(
	                "3DEXPERIENCE Search failed. Status="
	                        + response.statusCode()
	                        + " Response="
	                        + response.body()
	        );
	    }

	    return new ChangeActionDto(
	            true,
	            response.statusCode(),
	            searchStr,
	            response.body()
	    );
	}
}