package com.keonys.copilot3DX.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.keonys.copilot3DX.dto.MaturityDto;
import com.keonys.copilot3DX.service.MaturityService;

@Component
public class MaturityMcpTools {

	private final MaturityService maturityService;

	public MaturityMcpTools(MaturityService maturityService) {

		this.maturityService = maturityService;
	}

	@Tool(name = "changeObjectMaturityState", description = """
			Change the maturity state of an object in 3DEXPERIENCE.

			Use this tool when the user wants to promote or change
			the maturity state of a Manufacturing Item or
			Engineering Item.

			The object must be identified by its 3DEXPERIENCE
			physical ID.

			The target maturity state must be explicitly provided
			by the user.

			Examples:
			- Promote Manufacturing Item 57BA... to Frozen
			- Change Engineering Item 12345 to Released
			- Set the maturity state of this object to Frozen

			Do not use this tool to search for objects.
			""")
	public MaturityDto changeObjectMaturityState(

			@ToolParam(description = """
					3DEXPERIENCE physical ID of the Manufacturing
					Item or Engineering Item.
					""", required = true) String objectId,

			@ToolParam(description = """
					Target maturity state of the object.

					Convert natural language states to the following
					3DEXPERIENCE values:

					Draft / Brouillon
					-> DRAFT

					In Work / En cours / En travail
					-> IN_WORK

					Frozen / Freeze / Geler / Gelé
					-> FROZEN

					Release / Released / Publier / Publié
					-> RELEASED

					Obsolete / Rendre obsolète
					-> OBSOLETE

					Examples:

					"passer à l'état In Work"
					-> IN_WORK

					"mettre In Work"
					-> IN_WORK

					"passer à l'état Release"
					-> RELEASED

					"releaser l'objet"
					-> RELEASED

					"geler l'objet"
					-> FROZEN

					"passer l'objet à Frozen"
					-> FROZEN

					"mettre l'objet en brouillon"
					-> DRAFT

					"rendre l'objet obsolète"
					-> OBSOLETE

					Always provide the normalized 3DEXPERIENCE
					state value.
					""", required = true) String nextState

	) throws Exception {

		return maturityService.changeState(objectId, nextState);
	}
}