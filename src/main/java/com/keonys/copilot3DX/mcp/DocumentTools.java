package com.keonys.copilot3DX.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.keonys.copilot3DX.dto.DocumentDetailDto;
import com.keonys.copilot3DX.dto.DocumentDto;
import com.keonys.copilot3DX.service.DocumentService;

@Component
public class DocumentTools {

	private final DocumentService documentService;

	public DocumentTools(DocumentService documentService) {

		this.documentService = documentService;
	}

	@Tool(name = "searchDocuments", description = "Search documents in 3DEXPERIENCE using a document title, name or keyword")
	public DocumentDto searchDocuments(
			@ToolParam(description = "Document title, name or keyword to search in 3DEXPERIENCE") String searchStr)
			throws Exception {

		return documentService.searchDocuments(searchStr);
	}

	@Tool(name = "getDocumentByPhysicalId", description = "Get detailed information and attached files of a 3DEXPERIENCE document using its physical identifier")
	public DocumentDetailDto getDocumentByPhysicalId(
			@ToolParam(description = "Physical identifier of the 3DEXPERIENCE document") String physicalId)
			throws Exception {

		return documentService.getDocumentByPhysicalId(physicalId);
	}
}