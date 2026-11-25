package com.keonys.copilot3DX.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.dto.WorkPlanDto;
import com.keonys.copilot3DX.model.WorkPlanInfo;
import com.keonys.copilot3DX.service.WorkPlanService;

@RestController
@RequestMapping("/workplans")
public class WorkPlanController {

	private final WorkPlanService workPlanService;

	public WorkPlanController(
			WorkPlanService workPlanService) {

		this.workPlanService = workPlanService;
	}

	@GetMapping(
			value = "/search",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public WorkPlanDto searchWorkPlans(
			@RequestParam(required = false)
			String searchStr) throws Exception {

		return workPlanService
				.searchWorkPlans(searchStr);
	}

	@GetMapping(
			value = "/info",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public WorkPlanInfo getWorkPlanInfo(
			@RequestParam
			String physicalId) throws Exception {

		return workPlanService
				.getWorkPlanInfo(physicalId);
	}

	@GetMapping(
			value = "/by-manufacturing-item",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public WorkPlanDto getManufacturingItemWorkPlans(
			@RequestParam
			String physicalId) throws Exception {

		return workPlanService
				.getAttachedWorkPlans(physicalId);
	}
}