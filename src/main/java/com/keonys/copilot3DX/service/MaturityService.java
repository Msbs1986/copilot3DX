package com.keonys.copilot3DX.service;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.keonys.copilot3DX.config.HttpRequestService;
import com.keonys.copilot3DX.dto.MaturityDto;

@Service
public class MaturityService {

	private static final String CHANGE_STATE_ENDPOINT = "/resources/v1/modeler/dslc/maturity/changeState";

	@Value("${threedx.space-url}")
	private String space3dsUrlStr;

	@Value("${threedx.security-context}")
	private String secContext;

	private final Service3DXConnexion service3DXConnexion;
	private final HttpRequestService httpRequestService;

	public MaturityService(Service3DXConnexion service3DXConnexion, HttpRequestService httpRequestService) {

		this.service3DXConnexion = service3DXConnexion;
		this.httpRequestService = httpRequestService;
	}

	@SuppressWarnings("unchecked")
	public MaturityDto changeState(String objectId, String nextState) throws Exception {

		/*
		 * Validate parameters
		 */
		if (objectId == null || objectId.isBlank()) {
			throw new IllegalArgumentException("Object ID must not be null or empty.");
		}

		if (nextState == null || nextState.isBlank()) {
			throw new IllegalArgumentException("Next state must not be null or empty.");
		}

		String effectiveObjectId = objectId.trim();
		String effectiveNextState = nextState.trim();

		/*
		 * Prepare authentication
		 */
		service3DXConnexion.prepareAuthorizationHeaderValue();

		/*
		 * Retrieve CSRF token
		 */
		String csrfToken = service3DXConnexion.getCsrfTokenValueBasic();

		/*
		 * Build headers
		 */
		Map<String, String> headers = createSecurityHeaders(csrfToken);

		/*
		 * Build request body
		 */
		JSONObject objectData = new JSONObject();

		objectData.put("id", effectiveObjectId);

		objectData.put("nextState", effectiveNextState);

		JSONArray data = new JSONArray();

		data.add(objectData);

		JSONObject requestBody = new JSONObject();

		requestBody.put("data", data);

		/*
		 * Build URL
		 */
		String url = space3dsUrlStr + CHANGE_STATE_ENDPOINT;

		/*
		 * Call 3DEXPERIENCE
		 */
		HttpResponse<String> response = httpRequestService.loadUrl("POST", MediaType.APPLICATION_JSON_VALUE,
				requestBody.toJSONString(), url, headers);
		/*
		 * Check response
		 */
		if (response.statusCode() < 200 || response.statusCode() >= 300) {

			throw new RuntimeException("3DEXPERIENCE maturity changeState failed. " + "Status=" + response.statusCode()
					+ " Response=" + response.body());
		}

		/*
		 * Build response
		 */
		return new MaturityDto(true, response.statusCode(), effectiveObjectId, effectiveNextState, response.body());
	}

	private Map<String, String> createSecurityHeaders(String csrfToken) {

		Map<String, String> headers = new HashMap<>();

		headers.put("SecurityContext", secContext);

		headers.put("ENO_CSRF_TOKEN", csrfToken);

		headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

		headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

		return headers;
	}
}