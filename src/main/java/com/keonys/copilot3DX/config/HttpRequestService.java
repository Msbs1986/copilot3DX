package com.keonys.copilot3DX.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

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
     * @param httpClient HTTP client to use for requests
     */
    @Autowired
    public HttpRequestService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    /**
     * Send HTTP request to specified URL with parameters
     * @param method HTTP method (GET, POST, etc.)
     * @param contentType Content type for request
     * @param postData Data to send in request body
     * @param url Target URL
     * @param headers Additional headers
     * @return HTTP response
     * @throws Exception if request fails
     */
    public HttpResponse<String> loadUrl(String method, String contentType, 
                                      String postData, String url, 
                                      Map<String, String> headers) throws Exception {
        // Build request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method(method, HttpRequest.BodyPublishers.ofString(postData))
                .setHeader("Accept", "application/json");
        
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
    
    public String loadUrlString(String method, String contentType, String postData, String urlString,
    	      Map<String, String> headers) throws Exception {

    	    URL url = new URL(urlString);
    	    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    	    connection.setRequestMethod(method);
    	    connection.setRequestProperty("Accept", "application/json");

    	    if (contentType != null && !contentType.isEmpty()) {
    	      connection.setRequestProperty("Content-Type", contentType);
    	    }

    	    if (headers != null) {
    	      for (Map.Entry<String, String> entry : headers.entrySet()) {
    	        connection.setRequestProperty(entry.getKey(), entry.getValue());
    	      }
    	    }

    	    if (postData != null && !postData.isEmpty()
    	        && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
    	      connection.setDoOutput(true);
    	      try (OutputStream os = connection.getOutputStream()) {
    	        os.write(postData.getBytes("UTF-8"));
    	        os.flush();
    	      }
    	    }

    	    int responseCode = connection.getResponseCode();
    	    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
    	    String inputLine;
    	    StringBuilder response = new StringBuilder();

    	    while ((inputLine = in.readLine()) != null) {
    	      response.append(inputLine);
    	    }
    	    in.close();

    	    System.out.println("Request to [" + urlString + "] completed with status: " + responseCode);

    	    return response.toString();
    	  }
}
