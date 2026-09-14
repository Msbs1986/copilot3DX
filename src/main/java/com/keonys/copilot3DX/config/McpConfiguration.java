package com.keonys.copilot3DX.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.keonys.copilot3DX.mcp.ChangeActionMcpTools;


@Configuration
public class McpConfiguration {

    @Bean
    ToolCallbackProvider toolCallbackProvider(
            ChangeActionMcpTools changeActionMcpTools) {

        return MethodToolCallbackProvider.builder()
                .toolObjects(
                    changeActionMcpTools
                )
                .build();
    }
}