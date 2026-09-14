package com.keonys.copilot3DX.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.keonys.copilot3DX.dto.ManufacturingItemDto;
import com.keonys.copilot3DX.service.ManufacturingItemService;

@Component
public class ManufacturingItemMcpTools {

	private final ManufacturingItemService manufacturingItemService;

	public ManufacturingItemMcpTools(ManufacturingItemService manufacturingItemService) {

		this.manufacturingItemService = manufacturingItemService;
	}

	@Tool(name = "searchManufacturingItems", description = """
			Search Manufacturing Items in 3DEXPERIENCE.

			Use this tool when the user asks about:
			- Manufacturing Items
			- Manufacturing references
			- Manufacturing identifiers
			- Manufacturing part numbers
			- Released Manufacturing Items

			Do not use this tool for MBOM expansion.
			""")
	public ManufacturingItemDto searchManufacturingItems(

			@ToolParam(description = """
					Manufacturing Item search criterion.
					Can be a name, title, identifier
					or search expression.
					""", required = false) String searchText

	) throws Exception {

		System.out.println("MCP TOOL CALLED -> searchManufacturingItems : " + searchText);

		return manufacturingItemService.searchManufacturingItems(searchText);
	}
}