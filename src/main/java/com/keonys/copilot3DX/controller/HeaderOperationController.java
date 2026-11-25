package com.keonys.copilot3DX.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.model.PrimaryResourceInfo;
import com.keonys.copilot3DX.model.ResourceInfo;
import com.keonys.copilot3DX.model.SecondaryResourceInfo;
import com.keonys.copilot3DX.service.HeaderOperationService;

@RestController
@RequestMapping("/header-operations")
public class HeaderOperationController {

	private final HeaderOperationService headerOperationService;

	public HeaderOperationController(HeaderOperationService headerOperationService) {

		this.headerOperationService = headerOperationService;
	}

	/**
	 * Get all Primary Capable Resources attached to one Header Operation.
	 */
	@GetMapping(value = "/primary-resources", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<PrimaryResourceInfo> getPrimaryResources(@RequestParam String physicalId) throws Exception {

		return headerOperationService.getPrimaryResources(physicalId);
	}

	/**
	 * Get all Secondary Capable Resources attached to one Header Operation.
	 */
	@GetMapping(value = "/secondary-resources", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<SecondaryResourceInfo> getSecondaryResources(@RequestParam String physicalId) throws Exception {

		return headerOperationService.getSecondaryResources(physicalId);
	}

	/**
	 * Get detailed information for one Resource.
	 */
	@GetMapping(value = "/resource", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResourceInfo getResourceDetails(@RequestParam String physicalId) throws Exception {

		return headerOperationService.getResourceDetails(physicalId);
	}
}