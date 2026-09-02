package com.keonys.copilot3DX.config;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration pour les clients HTTP
 */
@Configuration
public class HttpClientConfig {

    /**
     * Crée et configure un HttpClient comme bean Spring
     * @return HttpClient configuré avec gestion des cookies et redirections
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }
}
