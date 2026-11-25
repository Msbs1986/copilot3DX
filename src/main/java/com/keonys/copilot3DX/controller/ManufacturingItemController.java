package com.keonys.copilot3DX.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.dto.ManufacturingItemDto;
import com.keonys.copilot3DX.dto.WorkPlanDto;
import com.keonys.copilot3DX.model.ManufacturingItemInfo;
import com.keonys.copilot3DX.model.WorkPlanInfo;
import com.keonys.copilot3DX.service.ManufacturingItemService;
import com.keonys.copilot3DX.service.WorkPlanService;

@RestController
@RequestMapping("/manufacturing-items")
public class ManufacturingItemController {

	private final ManufacturingItemService manufacturingItemService;

	private final WorkPlanService workPlanService;

	public ManufacturingItemController(ManufacturingItemService manufacturingItemService,
			WorkPlanService workPlanService) {

		this.manufacturingItemService = manufacturingItemService;

		this.workPlanService = workPlanService;
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

	/**
	 * Retrouve les gammes attachées à un Manufacturing Item.
	 *
	 * Cet endpoint réalise automatiquement : 1. ItemSpecification/Locate 2.
	 * Filtrage des WorkPlans 3. Appel du masque Details
	 */
	@GetMapping(value = "/workplans", produces = MediaType.APPLICATION_JSON_VALUE)
	public WorkPlanDto getAttachedWorkPlans(@RequestParam String physicalId) throws Exception {

		return workPlanService.getAttachedWorkPlans(physicalId);
	}

	/**
	 * Retourne directement les informations d'un WorkPlan à partir de son
	 * physicalId.
	 */
	@GetMapping(value = "/workplans/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public WorkPlanInfo getWorkPlanInfo(@RequestParam String physicalId) throws Exception {

		return workPlanService.getWorkPlanInfo(physicalId);
	}
}