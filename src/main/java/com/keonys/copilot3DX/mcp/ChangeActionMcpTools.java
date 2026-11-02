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

    @Tool(
        name = "getChangeActionRealizedChanges",
        description = """
                Get a Change Action from 3DEXPERIENCE using its ID
                and retrieve the objects contained in its realizedChanges.

                Use this tool when the user wants information about
                a specific Change Action and the objects impacted,
                created, added, provided or modified by this Change Action.

                The tool returns:
                - Change Action information
                - Change Action status
                - Change Action title and description
                - Change Action owner
                - Change Action severity
                - Collaborative Space
                - Realized Changes
                - Object identifiers
                - Object relative paths
                - Realized Change types
                - Operations such as Create or Add

                Examples of relevant requests:
                - Show Change Action BA1C5B6D16E92A006A8EF53600009315
                - Show the realized changes of Change Action BA1C5B6D16E92A006A8EF53600009315
                - Which objects are impacted by Change Action BA1C5B6D16E92A006A8EF53600009315?
                - Which Manufacturing Items were created by this Change Action?
                - What objects were added by this Change Action?
                - Show me the objects affected by this Change Action.

                The Change Action ID must be provided.

                Do not use this tool to search Change Actions by name
                or partial text.

                Do not use this tool to search Manufacturing Items
                independently of a Change Action.
                """
    )
    public ChangeActionDto getChangeActionRealizedChanges(

            @ToolParam(
                description = """
                        Technical 3DEXPERIENCE identifier of the Change Action.

                        Example:
                        BA1C5B6D16E92A006A8EF53600009315

                        This must be the physical ID of the Change Action,
                        not its title or name.
                        """,
                required = true
            )
            String changeActionId

    ) throws Exception {

        return changeActionService.getChangeAction(changeActionId);
    }
}