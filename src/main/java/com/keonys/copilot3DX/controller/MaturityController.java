package com.keonys.copilot3DX.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.dto.MaturityDto;
import com.keonys.copilot3DX.service.MaturityService;

@RestController
@RequestMapping("/maturity")
public class MaturityController {

	private final MaturityService maturityService;

	public MaturityController(MaturityService maturityService) {

		this.maturityService = maturityService;
	}

	@PostMapping(value = "/change-state", produces = MediaType.APPLICATION_JSON_VALUE)
	public MaturityDto changeState(

			@RequestParam String objectId, @RequestParam String nextState

	) throws Exception {

		return maturityService.changeState(objectId, nextState);
	}
}