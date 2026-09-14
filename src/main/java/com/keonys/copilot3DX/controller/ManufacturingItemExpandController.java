package com.keonys.copilot3DX.controller;

import org.json.simple.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.keonys.copilot3DX.service.ManufacturingItemExpandService;

@RestController
@RequestMapping("/manufacturing-items")
public class ManufacturingItemExpandController {

    private final ManufacturingItemExpandService expandService;

    public ManufacturingItemExpandController(
            ManufacturingItemExpandService expandService) {

        this.expandService = expandService;
    }

    @GetMapping(
            value = "/expand-search",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public JSONObject expandManufacturingItemsBySearch(
            @RequestParam(required = false)
            String searchStr) throws Exception {

        return expandService
                .expandManufacturingItemsBySearch(searchStr);
    }
}