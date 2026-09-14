package com.keonys.copilot3DX.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.dto.EngineeringItemDto;
import com.keonys.copilot3DX.model.EngineeringItemInfo;
import com.keonys.copilot3DX.service.EngineeringItemService;

@RestController
@RequestMapping("/engineering-items")
public class EngineeringItemController {

	private final EngineeringItemService engineeringItemService;

	public EngineeringItemController(EngineeringItemService engineeringItemService) {

		this.engineeringItemService = engineeringItemService;
	}

	@GetMapping("/search")
	public EngineeringItemDto searchEngineeringItems(@RequestParam(required = false) String searchStr)
			throws Exception {

		return engineeringItemService.searchEngineeringItems(searchStr);
	}

	@GetMapping("/{physicalId}")
	public EngineeringItemInfo getEngineeringItemInfo(@PathVariable String physicalId) throws Exception {

		return engineeringItemService.getEngineeringItemInfo(physicalId);
	}
}