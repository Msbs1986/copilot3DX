package com.keonys.copilot3DX.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	/**
	 * Search Change Actions.
	 *
	 * Examples:
	 *
	 * GET /change-actions/search
	 *
	 * GET /change-actions/search?searchStr=*
	 *
	 * GET /change-actions/search?searchStr=MBOM
	 */
	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ChangeActionDto searchChangeActions(

			@RequestParam(name = "searchStr", required = false) String searchStr) throws Exception {

		return changeActionService.searchChangeActions(searchStr);
	}

	/**
	 * Get Change Action details and realized changes.
	 *
	 * Example:
	 *
	 * GET /change-actions/BA1C5B6D16E92A006A8EF53600009315
	 */
	@GetMapping(value = "/{changeActionId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ChangeActionDto getChangeAction(

			@PathVariable String changeActionId) throws Exception {

		return changeActionService.getChangeAction(changeActionId);
	}
}