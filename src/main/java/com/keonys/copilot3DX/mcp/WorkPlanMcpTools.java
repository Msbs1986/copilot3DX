package com.keonys.copilot3DX.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.keonys.copilot3DX.dto.WorkPlanDto;
import com.keonys.copilot3DX.model.WorkPlanInfo;
import com.keonys.copilot3DX.service.WorkPlanService;

@Component
public class WorkPlanMcpTools {

	private final WorkPlanService workPlanService;

	public WorkPlanMcpTools(WorkPlanService workPlanService) {

		this.workPlanService = workPlanService;
	}

	@Tool(name = "searchWorkPlans", description = """
			Search WorkPlans or Gammes in 3DEXPERIENCE.

			Use this tool when the user asks to:
			- search for a WorkPlan
			- search for a Gamme
			- list all WorkPlans
			- list all Gammes
			- find a WorkPlan by name or title
			- find a Gamme by name or title

			If no search criterion is supplied, the tool uses
			the wildcard "*" and returns all accessible WorkPlans.

			Do not use this tool to find the WorkPlans attached
			to a specific Manufacturing Item. For that request,
			use getManufacturingItemWorkPlans.
			""")
	public WorkPlanDto searchWorkPlans(

			@ToolParam(description = """
					WorkPlan or Gamme search criterion.
					Can be a name, title or search expression.
					Leave empty to search all WorkPlans using "*".
					""", required = false) String searchText

	) throws Exception {

		System.out.println("MCP TOOL CALLED -> searchWorkPlans : " + searchText);

		return workPlanService.searchWorkPlans(searchText);
	}

	@Tool(name = "getWorkPlanDetails", description = """
			Get detailed information about one WorkPlan
			or Gamme in 3DEXPERIENCE.

			Use this tool when the user asks to:
			- display WorkPlan details
			- display Gamme details
			- retrieve information about a specific WorkPlan
			- retrieve information about a specific Gamme

			The input must be the physical ID of the WorkPlan,
			not the physical ID of a Manufacturing Item.

			If only the WorkPlan name or title is known,
			first call searchWorkPlans to obtain its physical ID.
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

	@Tool(name = "getManufacturingItemWorkPlans", description = """
			Get the WorkPlans or Gammes attached to
			a Manufacturing Item in 3DEXPERIENCE.

			Use this tool when the user asks to:
			- display WorkPlans attached to a Manufacturing Item
			- display Gammes attached to a Manufacturing Item
			- find the routing of a Manufacturing Item
			- find the manufacturing process of a Manufacturing Item

			The input must be the physical ID of the
			Manufacturing Item, not a WorkPlan ID.

			If the Manufacturing Item physical ID is not known,
			first use searchManufacturingItems to retrieve it.
			""")
	public WorkPlanDto getManufacturingItemWorkPlans(

			@ToolParam(description = """
					Physical ID of the Manufacturing Item
					for which attached WorkPlans or Gammes
					must be retrieved.
					""", required = true) String manufacturingItemPhysicalId

	) throws Exception {

		System.out.println("MCP TOOL CALLED -> getManufacturingItemWorkPlans : " + manufacturingItemPhysicalId);

		return workPlanService.getAttachedWorkPlans(manufacturingItemPhysicalId);
	}
}