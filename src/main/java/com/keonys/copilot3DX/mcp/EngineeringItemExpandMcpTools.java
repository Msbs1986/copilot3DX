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
			product structure, design structure,
			assembly hierarchy, components, subassemblies,
			children or drawings attached to the structure.

			The tool searches the root Engineering Item
			before expanding its complete structure.

			The result contains:
			- the EBOM as a hierarchical root structure;
			- all children grouped under their parents;
			- the level of every Engineering Item;
			- Drawing objects;
			- VPMInstance and VPMRepInstance relationships;
			- a flat list of all objects with their levels.

			Use this tool for requests such as:
			- Show me the EBOM of an Engineering Item.
			- Expand the Engineering Item structure.
			- Display the product structure.
			- Show all children and their levels.
			- Show the drawings in the engineering structure.

			Do not use this tool for a simple Engineering
			Item search.

			Do not use this tool for Manufacturing Items,
			MBOM, Manufacturing BOM or Change Actions.
			""")
	public JSONObject expandEngineeringItemStructure(

			@ToolParam(description = """
					Search criterion identifying the root
					Engineering Item.

					The value can be a physicalId, name,
					title or valid 3DEXPERIENCE search
					expression.

					Examples:
					- Physical Product00350754
					- prd-R1132101389013-00350754
					- 57BAADA0F78015006AACDF5900000965
					""", required = false) String searchText)

			throws Exception {

		System.out.println("MCP TOOL CALLED: " + "expandEngineeringItemStructure" + ", criterion=" + searchText);

		return expandService.expandEngineeringItemsBySearch(searchText);
	}
}