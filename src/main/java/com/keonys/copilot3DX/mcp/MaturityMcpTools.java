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
			Change the maturity state of a Manufacturing Item or
			Engineering Item in 3DEXPERIENCE.

			IMPORTANT:
			In this tool, the words Release, Released, Frozen,
			In Work, Draft and Obsolete refer to 3DEXPERIENCE
			maturity lifecycle states.

			"Release" means changing the 3DEXPERIENCE maturity
			state to RELEASED. It does NOT mean escalating,
			transferring or handing the conversation to a person.

			Use this tool when the user asks to:
			- change the maturity state of an object
			- promote an object
			- release an object
			- freeze an object
			- geler un objet
			- passer un objet à In Work
			- passer un objet à Release
			- rendre un objet obsolète

			Examples:

			"Je veux passer cet objet à Release"
			-> call this tool with nextState = RELEASED

			"Release this Manufacturing Item"
			-> call this tool with nextState = RELEASED

			"Je veux geler cet objet"
			-> call this tool with nextState = FROZEN

			"Freeze this Manufacturing Item"
			-> call this tool with nextState = FROZEN

			"Passe cet objet à In Work"
			-> call this tool with nextState = IN_WORK

			"Passe cet objet en Draft"
			-> call this tool with nextState = DRAFT

			The object must be identified by its 3DEXPERIENCE
			physical ID.
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