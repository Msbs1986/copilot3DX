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
import com.keonys.copilot3DX.mcp.WorkPlanMcpTools;

@Configuration
public class McpConfiguration {

	@Bean
	public ToolCallbackProvider toolCallbackProvider(ChangeActionMcpTools changeActionMcpTools,
			ManufacturingItemMcpTools manufacturingItemMcpTools,
			ManufacturingItemExpandMcpTools manufacturingItemExpandMcpTools,
			EngineeringItemMcpTools engineeringItemMcpTools, DocumentTools documentTools,
			EngineeringItemExpandMcpTools engineeringItemExpandMcpTools, MaturityMcpTools maturityMcpTools,
			WorkPlanMcpTools workPlanMcpTools) {

		return MethodToolCallbackProvider.builder()
				.toolObjects(changeActionMcpTools, manufacturingItemMcpTools, manufacturingItemExpandMcpTools,
						engineeringItemMcpTools, documentTools, engineeringItemExpandMcpTools, maturityMcpTools,
						workPlanMcpTools)
				.build();
	}
}