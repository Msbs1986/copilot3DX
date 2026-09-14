package com.keonys.copilot3DX.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.dto.ChangeActionDto;
import com.keonys.copilot3DX.service.ChangeActionService;

@RestController
@RequestMapping("/change-actions")
public class ChangeActionController {

	private final ChangeActionService changeActionService;

	public ChangeActionController(ChangeActionService changeActionService) {

		this.changeActionService = changeActionService;
	}

	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ChangeActionDto searchChangeActions(@RequestParam(required = false) String searchStr) throws Exception {

		return changeActionService.searchChangeActions(searchStr);
	}
}