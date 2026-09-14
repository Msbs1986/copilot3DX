package com.keonys.copilot3DX.mcp;

import org.json.simple.JSONObject;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.keonys.copilot3DX.service.ManufacturingItemExpandService;

@Component
public class ManufacturingItemExpandMcpTools {

	private final ManufacturingItemExpandService expandService;

	public ManufacturingItemExpandMcpTools(ManufacturingItemExpandService expandService) {

		this.expandService = expandService;
	}

	@Tool(name = "expandManufacturingItemStructure", description = """
			Expand the complete manufacturing structure
			of a Manufacturing Item in 3DEXPERIENCE.

			Use this tool when the user asks for an MBOM,
			Manufacturing BOM, manufacturing structure,
			children, components or assembly hierarchy.

			The tool searches the root Manufacturing Item
			before expanding its complete structure.

			Do not use this tool for a simple Manufacturing
			Item search.

			Do not use this tool for Change Actions.
			""")
	public JSONObject expandManufacturingItemStructure(

			@ToolParam(description = """
					Search criterion identifying the root
					Manufacturing Item.

					The value can be a physicalId, name,
					title or valid 3DEXPERIENCE search
					expression.
					""", required = false) String searchText

	) throws Exception {

		System.out.println("MCP TOOL CALLED: " + "expandManufacturingItemStructure, criterion=" + searchText);

		return expandService.expandManufacturingItemsBySearch(searchText);
	}
}