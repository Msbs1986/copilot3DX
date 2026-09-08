package com.keonys.copilot3DX.config;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Service pour gérer les requêtes HTTP
 */
@Service
public class HttpRequestService {

	private static final Logger logger = LoggerFactory.getLogger(HttpRequestService.class);

	private final HttpClient httpClient;

	/**
	 * Constructor with HttpClient injection
	 * 
	 * @param httpClient HTTP client to use for requests
	 */
	@Autowired
	public HttpRequestService(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Send HTTP request to specified URL with parameters
	 * 
	 * @param method      HTTP method (GET, POST, etc.)
	 * @param contentType Content type for request
	 * @param postData    Data to send in request body
	 * @param url         Target URL
	 * @param headers     Additional headers
	 * @return HTTP response
	 * @throws Exception if request fails
	 */
	public HttpResponse<String> loadUrl(String method, String contentType, String postData, String url,
			Map<String, String> headers) throws Exception {
		// Build request
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url))
				.method(method, HttpRequest.BodyPublishers.ofString(postData)).setHeader("Accept", "application/json");

		// Set content type if provided
		if (contentType != null && !contentType.isEmpty()) {
			requestBuilder.setHeader("Content-Type", contentType);
		}

		// Add custom headers
		if (headers != null) {
			headers.forEach(requestBuilder::setHeader);
		}

		// Build and send request
		HttpRequest request = requestBuilder.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		logger.debug("Request to [{}] completed with status: {}", url, response.statusCode());

		return response;
	}

	public HttpResponse<byte[]> loadUrlReponseByte(String method, String contentType, String postData, String url,
			Map<String, String> headers) throws Exception {

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url))
				.method(method, HttpRequest.BodyPublishers.ofString(postData)).setHeader("Accept", "application/json");

		if (contentType != null && !contentType.isEmpty()) {
			requestBuilder.setHeader("Content-Type", contentType);
		}

		if (headers != null) {
			headers.forEach(requestBuilder::setHeader);
		}

		HttpRequest request = requestBuilder.build();
		HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

		logger.debug("Request to [{}] completed with status: {}", url, response.statusCode());

		return response;
	}

	public static String createFormUrlEncodedBody(Map<String, String> params) {
		return params.entrySet().stream().map(entry -> {
			String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
			String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
			return encodedKey + "=" + encodedValue;
		}).collect(Collectors.joining("&"));
	}

}
