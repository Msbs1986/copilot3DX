package com.keonys.copilot3DX.controller;

import java.util.List;

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

	/**
	 * Search WorkPlans.
	 *
	 * Example:
	 * GET /workplans/search?searchStr=Workplan*
	 */
	@GetMapping(
			value = "/search",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public WorkPlanDto searchWorkPlans(
			@RequestParam(required = false)
			String searchStr) throws Exception {

		return workPlanService
				.searchWorkPlans(searchStr);
	}

	/**
	 * Get detailed information about one WorkPlan.
	 *
	 * Example:
	 * GET /workplans/info
	 *     ?physicalId=E991D96992210D006B06BD47000190D7
	 */
	@GetMapping(
			value = "/info",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public WorkPlanInfo getWorkPlanInfo(
			@RequestParam
			String physicalId) throws Exception {

		return workPlanService
				.getWorkPlanInfo(physicalId);
	}

	/**
	 * Get all WorkPlans attached to a Manufacturing Item.
	 *
	 * Example:
	 * GET /workplans/by-manufacturing-item
	 *     ?physicalId=0A015864055A05006AFEF2F20000CB09
	 */
	@GetMapping(
			value = "/by-manufacturing-item",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public WorkPlanDto getManufacturingItemWorkPlans(
			@RequestParam
			String physicalId) throws Exception {

		return workPlanService
				.getAttachedWorkPlans(physicalId);
	}

	/**
	 * Expand a WorkPlan and return only its Header Operation references.
	 *
	 * Only elements whose type is:
	 * DELLmiHeaderOperationReference
	 *
	 * Example:
	 * GET /workplans/header-operations
	 *     ?physicalId=E991D96992210D006B06BD47000190D7
	 */
	@GetMapping(
			value = "/header-operations",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public List<WorkPlanInfo> getWorkPlanHeaderOperations(
			@RequestParam
			String physicalId) throws Exception {

		return workPlanService
				.getWorkPlanHeaderOperations(physicalId);
	}
}