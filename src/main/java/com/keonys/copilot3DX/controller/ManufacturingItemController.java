package com.keonys.copilot3DX.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.dto.ManufacturingItemDto;
import com.keonys.copilot3DX.model.ManufacturingItemInfo;
import com.keonys.copilot3DX.service.ManufacturingItemService;

@RestController
@RequestMapping("/manufacturing-items")
public class ManufacturingItemController {

	private final ManufacturingItemService manufacturingItemService;

	public ManufacturingItemController(ManufacturingItemService manufacturingItemService) {

		this.manufacturingItemService = manufacturingItemService;
	}

	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ManufacturingItemDto searchManufacturingItems(@RequestParam(required = false) String searchStr)
			throws Exception {

		return manufacturingItemService.searchManufacturingItems(searchStr);
	}

	@GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public ManufacturingItemInfo getManufacturingItemInfo(@RequestParam String physicalId) throws Exception {

		return manufacturingItemService.getManufacturingItemInfo(physicalId);
	}
}