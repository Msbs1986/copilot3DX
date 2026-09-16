package com.keonys.copilot3DX.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.dto.DocumentDetailDto;
import com.keonys.copilot3DX.dto.DocumentDto;
import com.keonys.copilot3DX.service.DocumentService;

@RestController
@RequestMapping("/documents")
public class DocumentController {

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {

		this.documentService = documentService;
	}

	@GetMapping("/search")
	public ResponseEntity<DocumentDto> searchDocuments(
			@RequestParam(name = "searchStr", required = false) String searchStr) throws Exception {

		DocumentDto result = documentService.searchDocuments(searchStr);

		return ResponseEntity.ok(result);
	}

	@GetMapping("/{physicalId}")
	public ResponseEntity<DocumentDetailDto> getDocumentByPhysicalId(@PathVariable("physicalId") String physicalId)
			throws Exception {

		DocumentDetailDto result = documentService.getDocumentByPhysicalId(physicalId);

		return ResponseEntity.ok(result);
	}
}