package com.keonys.copilot3DX.mcp;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.keonys.copilot3DX.model.PrimaryResourceInfo;
import com.keonys.copilot3DX.model.ResourceInfo;
import com.keonys.copilot3DX.model.SecondaryResourceInfo;
import com.keonys.copilot3DX.service.HeaderOperationService;

@Component
public class HeaderOperationMcpTools {

	private final HeaderOperationService headerOperationService;

	public HeaderOperationMcpTools(HeaderOperationService headerOperationService) {

		this.headerOperationService = headerOperationService;
	}

	/**
	 * MCP Tool: Get Primary Capable Resources attached to a Header Operation.
	 */
	@Tool(name = "getHeaderOperationPrimaryResources", description = """
			Get the Primary Capable Resources attached to
			a Header Operation (HOP) in 3DEXPERIENCE.

			Use this tool when the user asks to:
			- display the primary resources of a Header Operation
			- display the primary resources of a HOP
			- list primary resources of an operation
			- find resources used as primary resources
			- retrieve Primary Capable Resources
			- display workers, machines or other resources
			  assigned as primary resources to an operation

			The input must be the physical ID of the
			Header Operation, not a WorkPlan ID and not
			a Resource ID.

			The tool returns the Primary Capable Resources
			together with detailed Resource information.

			Returned information can include:
			- resource identifier
			- resource type
			- resource quantity
			- resource name
			- resource title
			- detailed resource type
			- maturity state
			- revision
			- organization
			- collaborative space

			If only the WorkPlan or Gamme is known,
			first use getWorkPlanHeaderOperations to retrieve
			the Header Operations and their physical IDs.
			""")
	public List<PrimaryResourceInfo> getHeaderOperationPrimaryResources(

			@ToolParam(description = """
					Physical ID of the Header Operation (HOP)
					for which Primary Capable Resources
					must be retrieved.

					This must be a Header Operation physical ID,
					not a WorkPlan physical ID and not
					a Resource physical ID.
					""", required = true) String headerOperationPhysicalId

	) throws Exception {

		System.out.println("MCP TOOL CALLED -> " + "getHeaderOperationPrimaryResources : " + headerOperationPhysicalId);

		return headerOperationService.getPrimaryResources(headerOperationPhysicalId);
	}

	/**
	 * MCP Tool: Get Secondary Capable Resources attached to a Header Operation.
	 */
	@Tool(name = "getHeaderOperationSecondaryResources", description = """
			Get the Secondary Capable Resources attached to
			a Header Operation (HOP) in 3DEXPERIENCE.

			Use this tool when the user asks to:
			- display the secondary resources of a Header Operation
			- display the secondary resources of a HOP
			- list secondary resources of an operation
			- find resources used as secondary resources
			- retrieve Secondary Capable Resources
			- display tools, devices or other resources
			  assigned as secondary resources to an operation

			The input must be the physical ID of the
			Header Operation, not a WorkPlan ID and not
			a Resource ID.

			The tool returns the Secondary Capable Resources
			together with detailed Resource information.

			Returned information can include:
			- resource identifier
			- resource type
			- resource name
			- resource title
			- detailed resource type
			- maturity state
			- revision
			- organization
			- collaborative space
			- relationship to the Primary Capable Resource

			The dependsOnIdentifier identifies the
			Primary Capable Resource relation on which
			the Secondary Resource depends.

			If only the WorkPlan or Gamme is known,
			first use getWorkPlanHeaderOperations to retrieve
			the Header Operations and their physical IDs.
			""")
	public List<SecondaryResourceInfo> getHeaderOperationSecondaryResources(

			@ToolParam(description = """
					Physical ID of the Header Operation (HOP)
					for which Secondary Capable Resources
					must be retrieved.

					This must be a Header Operation physical ID,
					not a WorkPlan physical ID and not
					a Resource physical ID.
					""", required = true) String headerOperationPhysicalId

	) throws Exception {

		System.out
				.println("MCP TOOL CALLED -> " + "getHeaderOperationSecondaryResources : " + headerOperationPhysicalId);

		return headerOperationService.getSecondaryResources(headerOperationPhysicalId);
	}

	/**
	 * MCP Tool: Get detailed information about one Resource.
	 */
	@Tool(name = "getResourceDetails", description = """
			Get detailed information about one Resource
			in 3DEXPERIENCE using its physical ID.

			Use this tool when the user asks to:
			- display Resource details
			- retrieve information about a specific Resource
			- display information about a Worker
			- display information about a Tool or Device
			- display information about a Resource Asset
			- display the name of a Resource
			- display the title of a Resource
			- display the resource type
			- display the maturity state of a Resource
			- display Resource information from its physical ID

			The input must be the physical ID of the Resource.

			When a Resource comes from a Header Operation,
			the Resource physical ID corresponds to
			resourceIdentifier returned by
			getHeaderOperationPrimaryResources or
			getHeaderOperationSecondaryResources.

			The tool returns detailed Resource information
			including:
			- name
			- title
			- description
			- physical ID
			- type
			- resource type
			- revision
			- maturity state
			- owner
			- organization
			- collaborative space
			- creation date
			- modification date

			If only a Header Operation physical ID is known,
			first use getHeaderOperationPrimaryResources or
			getHeaderOperationSecondaryResources to retrieve
			the Resource identifiers.
			""")
	public ResourceInfo getResourceDetails(

			@ToolParam(description = """
					Physical ID of the 3DEXPERIENCE Resource
					for which detailed information must
					be retrieved.

					For a Resource attached to a Header Operation,
					this corresponds to resourceIdentifier
					returned by a Primary Capable Resource or
					Secondary Capable Resource.
					""", required = true) String resourcePhysicalId

	) throws Exception {

		System.out.println("MCP TOOL CALLED -> getResourceDetails : " + resourcePhysicalId);

		return headerOperationService.getResourceDetails(resourcePhysicalId);
	}
}