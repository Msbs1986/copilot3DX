package com.keonys.copilot3DX.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.keonys.copilot3DX.mcp.ChangeActionMcpTools;
import com.keonys.copilot3DX.mcp.DocumentTools;
import com.keonys.copilot3DX.mcp.EngineeringItemExpandMcpTools;
import com.keonys.copilot3DX.mcp.EngineeringItemMcpTools;
import com.keonys.copilot3DX.mcp.ManufacturingItemExpandMcpTools;
import com.keonys.copilot3DX.mcp.ManufacturingItemMcpTools;
import com.keonys.copilot3DX.mcp.MaturityMcpTools;

@Configuration
public class McpConfiguration {

	@Bean
	public ToolCallbackProvider toolCallbackProvider(ChangeActionMcpTools changeActionMcpTools,
			ManufacturingItemMcpTools manufacturingItemMcpTools, ManufacturingItemExpandMcpTools expandMcpTools,
			EngineeringItemMcpTools engineeringItemMcpTools, DocumentTools documentTools,
			EngineeringItemExpandMcpTools engineeringItemExpandMcpTools, MaturityMcpTools maturityMcpTools) {

		return MethodToolCallbackProvider
				.builder().toolObjects(changeActionMcpTools, manufacturingItemMcpTools, expandMcpTools,
						engineeringItemMcpTools, documentTools, engineeringItemExpandMcpTools, maturityMcpTools)
				.build();
	}
}