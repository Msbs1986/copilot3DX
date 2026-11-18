package com.keonys.copilot3DX.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.keonys.copilot3DX.dto.ChangeActionDto;
import com.keonys.copilot3DX.service.ChangeActionService;

@Component
public class ChangeActionMcpTools {

	private final ChangeActionService changeActionService;

	public ChangeActionMcpTools(ChangeActionService changeActionService) {

		this.changeActionService = changeActionService;
	}

	@Tool(name = "searchChangeActions", description = """
			Search Change Actions in 3DEXPERIENCE.

			Use this tool when the user wants to search,
			find or list Change Actions.

			The search uses the 3DEXPERIENCE searchStr parameter.

			Examples:
			- Give me all Change Actions
			- List all Change Actions
			- Search Change Actions
			- Search Change Actions containing MBOM
			- Find Change Action CA-0001
			- Find the Change Action named Update MBOM

			If the user asks for all Change Actions,
			use "*" as searchStr.

			The search returns Change Action physical identifiers.
			These identifiers can then be used with
			getChangeActionRealizedChanges to retrieve detailed
			information about a specific Change Action.

			Use this tool if the Change Action physical ID
			is not already known.
			""")
	public ChangeActionDto searchChangeActions(

			@ToolParam(description = """
					Search string sent to the 3DEXPERIENCE
					Change Action searchStr parameter.

					Examples:
					- *
					- MBOM
					- CA-0001
					- Update MBOM

					Use "*" to retrieve all Change Actions.
					""", required = false) String searchStr) throws Exception {

		return changeActionService.searchChangeActions(searchStr);
	}

	@Tool(name = "getChangeActionRealizedChanges", description = """
			Get detailed information about a specific Change Action
			using its 3DEXPERIENCE physical ID.

			The tool also retrieves the objects contained in
			realizedChanges.

			Use this tool when the Change Action physical ID
			is already known.

			Examples:
			- Show Change Action BA1C5B6D16E92A006A8EF53600009315
			- Show the realized changes of Change Action
			  BA1C5B6D16E92A006A8EF53600009315
			- Which objects were created by Change Action
			  BA1C5B6D16E92A006A8EF53600009315?
			- Which objects were added by this Change Action?

			If the physical ID is not known,
			use searchChangeActions first.
			""")
	public ChangeActionDto getChangeActionRealizedChanges(

			@ToolParam(description = """
					Physical identifier of the Change Action.

					Example:
					BA1C5B6D16E92A006A8EF53600009315

					This identifier can be obtained from
					searchChangeActions.
					""", required = true) String changeActionId) throws Exception {

		return changeActionService.getChangeAction(changeActionId);
	}
}