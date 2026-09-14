package com.keonys.copilot3DX.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.keonys.copilot3DX.dto.EngineeringItemDto;
import com.keonys.copilot3DX.model.EngineeringItemInfo;
import com.keonys.copilot3DX.service.EngineeringItemService;

@Component
public class EngineeringItemMcpTools {

	private final EngineeringItemService engineeringItemService;

	public EngineeringItemMcpTools(EngineeringItemService engineeringItemService) {

		this.engineeringItemService = engineeringItemService;
	}

	@Tool(name = "searchEngineeringItems", description = "Search Engineering Items in 3DEXPERIENCE using a name, title, external id or any search string")
	public EngineeringItemDto searchEngineeringItems(

			@ToolParam(description = "Search string used to find Engineering Items") String searchStr)

			throws Exception {

		return engineeringItemService.searchEngineeringItems(searchStr);
	}

	@Tool(name = "getEngineeringItemInfo", description = "Get detailed information about an Engineering Item from its physical id")
	public EngineeringItemInfo getEngineeringItemInfo(

			@ToolParam(description = "Engineering Item physical id") String physicalId)

			throws Exception {

		return engineeringItemService.getEngineeringItemInfo(physicalId);
	}
}