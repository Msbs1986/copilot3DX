package com.keonys.copilot3DX.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "threedx")
public class ThreeDxProperties {

    private String passportUrl;
    private String spaceUrl;
    private String username;
    private String password;
    private String securityContext;

}
