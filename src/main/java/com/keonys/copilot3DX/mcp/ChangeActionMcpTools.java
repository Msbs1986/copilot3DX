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

			Use this tool when the user wants to find one or more
			Change Actions using an identifier, name, title or
			partial search text.

			Examples of relevant requests:
			- Find Change Action CA-000123
			- Search Change Actions containing engine
			- Show available Change Actions
			- What is the status of Change Action CA-000123?

			Use an asterisk when no search criterion is supplied.

			Do not use this tool to search Manufacturing Items,
			Physical Products, documents or MBOM structures.
			""")
	public ChangeActionDto searchChangeActions(

			@ToolParam(description = """
					Change Action identifier, name, title or partial text.

					Examples:
					CA-000123
					engine modification
					*

					Use * to retrieve Change Actions without a specific
					search criterion.
					""", required = false) String searchText

	) throws Exception {

		return changeActionService.searchChangeActions(searchText);
	}
}