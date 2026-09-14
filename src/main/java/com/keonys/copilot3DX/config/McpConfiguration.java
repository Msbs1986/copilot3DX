package com.keonys.copilot3DX.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.keonys.copilot3DX.mcp.ChangeActionMcpTools;
import com.keonys.copilot3DX.mcp.ManufacturingItemExpandMcpTools;
import com.keonys.copilot3DX.mcp.ManufacturingItemMcpTools;

@Configuration
public class McpConfiguration {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            ChangeActionMcpTools changeActionMcpTools,
            ManufacturingItemMcpTools manufacturingItemMcpTools,
            ManufacturingItemExpandMcpTools expandMcpTools) {

        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        changeActionMcpTools,
                        manufacturingItemMcpTools,
                        expandMcpTools
                )
                .build();
    }
}