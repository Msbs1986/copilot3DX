package com.keonys.copilot3DX.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.keonys.copilot3DX.dto.ManufacturingItemDto;
import com.keonys.copilot3DX.dto.WorkPlanDto;
import com.keonys.copilot3DX.model.WorkPlanInfo;
import com.keonys.copilot3DX.service.ManufacturingItemService;
import com.keonys.copilot3DX.service.WorkPlanService;

@Component
public class ManufacturingItemMcpTools {

	private final ManufacturingItemService manufacturingItemService;
	private final WorkPlanService workPlanService;

	public ManufacturingItemMcpTools(ManufacturingItemService manufacturingItemService,
			WorkPlanService workPlanService) {

		this.manufacturingItemService = manufacturingItemService;
		this.workPlanService = workPlanService;
	}

	@Tool(name = "searchManufacturingItems", description = """
			Search Manufacturing Items in 3DEXPERIENCE.

			Use this tool when the user asks about:
			- Manufacturing Items
			- Manufacturing references
			- Manufacturing identifiers
			- Manufacturing part numbers
			- Released Manufacturing Items

			Do not use this tool for:
			- MBOM expansion
			- WorkPlan expansion
			- Manufacturing operations
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

	@Tool(name = "getManufacturingItemWorkPlans", description = """
			Get the WorkPlans attached to a Manufacturing Item
			in 3DEXPERIENCE.

			Use this tool when the user asks to:
			- display the WorkPlans attached to a Manufacturing Item
			- display the Gammes attached to a Manufacturing Item
			- find the routing of a Manufacturing Item
			- find the manufacturing process of a Manufacturing Item

			The input must be the physical ID of the
			Manufacturing Item.

			If the user provides a Manufacturing Item name,
			title or part number instead of a physical ID,
			first use searchManufacturingItems to identify
			the Manufacturing Item and obtain its physical ID.
			""")
	public WorkPlanDto getManufacturingItemWorkPlans(

			@ToolParam(description = """
					Physical ID of the Manufacturing Item
					for which the attached WorkPlans or Gammes
					must be retrieved.
					""", required = true) String physicalId

	) throws Exception {

		System.out.println("MCP TOOL CALLED -> getManufacturingItemWorkPlans : " + physicalId);

		return workPlanService.getAttachedWorkPlans(physicalId);
	}

	@Tool(name = "getWorkPlanDetails", description = """
			Get detailed information about a WorkPlan or Gamme
			in 3DEXPERIENCE.

			Use this tool when the user asks to:
			- display WorkPlan details
			- display Gamme details
			- get information about a specific WorkPlan
			- get information about a specific Gamme
			- display the details of a WorkPlan returned by
			  getManufacturingItemWorkPlans

			The input must be the physical ID of the WorkPlan,
			not the physical ID of the Manufacturing Item.

			If the WorkPlan physical ID is not known,
			first use getManufacturingItemWorkPlans with the
			Manufacturing Item physical ID to retrieve the
			attached WorkPlans and their IDs.
			""")
	public WorkPlanInfo getWorkPlanDetails(

			@ToolParam(description = """
					Physical ID of the WorkPlan or Gamme
					for which detailed information must
					be retrieved.
					""", required = true) String physicalId

	) throws Exception {

		System.out.println("MCP TOOL CALLED -> getWorkPlanDetails : " + physicalId);

		return workPlanService.getWorkPlanInfo(physicalId);
	}
}