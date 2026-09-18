package com.keonys.copilot3DX.mcp;

import org.json.simple.JSONObject;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.keonys.copilot3DX.service.EngineeringItemExpandService;

@Component
public class EngineeringItemExpandMcpTools {

	private final EngineeringItemExpandService expandService;

	public EngineeringItemExpandMcpTools(EngineeringItemExpandService expandService) {

		this.expandService = expandService;
	}

	@Tool(name = "expandEngineeringItemStructure", description = """
			Expand the complete engineering structure
			of an Engineering Item in 3DEXPERIENCE.

			Use this tool when the user asks for an EBOM,
			Engineering BOM, engineering structure,
			product structure, assembly hierarchy,
			components, sub assemblies or children.

			The tool searches the root Engineering Item
			before expanding its complete structure.

			Do not use this tool for a simple Engineering
			Item search.

			Do not use this tool for Change Actions
			or Manufacturing BOM requests.
			""")
	public JSONObject expandEngineeringItemStructure(

			@ToolParam(description = """
					Search criterion identifying the root
					Engineering Item.

					The value can be a physicalId, name,
					title or valid 3DEXPERIENCE search
					expression.
					""", required = false) String searchText)

			throws Exception {

		System.out.println("MCP TOOL CALLED: " + "expandEngineeringItemStructure, criterion=" + searchText);

		return expandService.expandEngineeringItemsBySearch(searchText);
	}
}