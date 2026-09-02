package com.keonys.copilot3DX.service;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.keonys.copilot3DX.config.HttpRequestService;


import jakarta.annotation.PostConstruct;

/**
 * Service pour la connexion à la plateforme 3DEXPERIENCE
 */
@Service
public class Service3DXConnexion {

    private static final Logger logger = LoggerFactory.getLogger(Service3DXConnexion.class);
    
    // Configuration injected from properties
    @Value("${threedx.tenantid}")
    private String tenant;

    @Value("${threedx.passport-url}")
    private String passport3dsUrlStr;

    @Value("${threedx.space-url}")
    private String space3dsUrlStr;

    @Value("${threedx.username}")
    private String login3DX;

    @Value("${threedx.password}")
    private String password3DX;

    // Authentication state
    private String csrfToken = "";
    private String loginTicket = "";
    
    // Injected HTTP request service
    private final HttpRequestService httpRequestService;
    
    /**
     * Constructor with HttpRequestService injection
     * @param httpRequestService Service to handle HTTP requests
     */
    @Autowired
    public Service3DXConnexion(HttpRequestService httpRequestService) {
        this.httpRequestService = httpRequestService;
    }

    /**
     * Initialize the service after properties are injected
     */
    @PostConstruct
    public void init() {
        logger.debug("Initializing 3DX Connection Service");
        logger.debug("Passport URL: {}", passport3dsUrlStr);
        logger.debug("Space URL: {}", space3dsUrlStr);
        logger.debug("Tenant: {}", tenant);
    }

    /**
     * Prepare authorization header by obtaining login ticket, authenticating, and getting CSRF token
     * @throws Exception if any authentication step fails
     */
    public void prepareAuthorizationHeaderValue() throws Exception {
        logger.info("Preparing authorization header value");
        
        // Step 1: Get login ticket
        loginTicket = getLoginTicketPassport();
        
        // Step 2: Authenticate
        authenticate();
        
      
        

    }

    /**
     * Logout from passport
     * @throws Exception if logout fails
     */
    public void logoutPassport() throws Exception {
        String logoutUrl = passport3dsUrlStr + "/logout";
        
        HttpResponse<String> response = httpRequestService.loadUrl("GET", "", "", logoutUrl, null);
        
        if (response.statusCode() == 200) {
            logger.info("Logout successful");
        } else {
            logger.warn("Logout may have failed. Status code: {}", response.statusCode());
        }
    }

    /**
     * Get login ticket from passport
     * @return Login ticket string
     * @throws Exception if retrieving login ticket fails
     */
    public String getLoginTicketPassport() throws Exception {
        String loginTicketUrl = passport3dsUrlStr + "/login?action=get_auth_params";
        
        HttpResponse<String> response = httpRequestService.loadUrl("GET", "", "", loginTicketUrl, null);
        logger.debug("Login ticket response: {}", response.body());
        
        // Parse JSON response
        JSONParser parser = new JSONParser();
        JSONObject jsonObj = (JSONObject) parser.parse(response.body());
        
        String ticket = jsonObj.get("lt").toString();
        logger.info("Login ticket obtained: {}", ticket);
        
        return ticket;
    }

    /**
     * Authenticate with passport using login ticket and credentials
     * @throws Exception if authentication fails
     */
    public void authenticate() throws Exception {
        String authUrl = passport3dsUrlStr + "/login";
        String postData = String.format("lt=%s&username=%s&password=%s", 
                                       loginTicket, login3DX, password3DX);
        String contentType = "application/x-www-form-urlencoded;charset=UTF-8";
        
        HttpResponse<String> response = httpRequestService.loadUrl("POST", contentType, postData, authUrl, null);
        
        if (response == null) {
            throw new Exception("The authentication web service response message is null");
        }
        
        logger.debug("Authentication response: {}", response.body());
        
        // Check for successful authentication
        if (response.statusCode() != 200) {
            throw new Exception("Authentication failed with status code: " + response.statusCode());
        }
        
        logger.info("Authentication successful");
    }

    /**
     * Get CSRF token required for secure operations
     * @return CSRF token string
     * @throws Exception if retrieving CSRF token fails
     */
    public String getCsrfTokenValue() throws Exception {
        String csrfTokenUrl = space3dsUrlStr + "/resources/v1/application/CSRF" + "?tenant=" + tenant;
        
        HttpResponse<String> response = httpRequestService.loadUrl("GET", "", "", csrfTokenUrl, null);
        logger.debug("CSRF token response: {}", response.body());
        
        // Parse JSON response
        JSONParser parser = new JSONParser();
        JSONObject jsonObj = (JSONObject) parser.parse(response.body());
        
        String token = "";
        if (jsonObj.get("success").toString().equalsIgnoreCase("true")) {
            JSONObject csrf = (JSONObject) jsonObj.get("csrf");
            token = csrf.get("value").toString();
            logger.info("CSRF token obtained");
        } else {
            logger.error("Failed to get CSRF token");
            throw new Exception("Failed to get CSRF token");
        }
        
        return token;
    }
    
    /**
     * Create an authenticated request with CSRF token
     * @param headers Map to add headers to
     * @return Map with authentication headers
     */
    public Map<String, String> createAuthenticatedHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("ENO_CSRF_TOKEN", csrfToken);
        // Add other common headers if needed
        return headers;
    }
}
