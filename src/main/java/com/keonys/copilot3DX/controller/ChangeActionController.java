package com.keonys.copilot3DX.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
     * Get a Change Action and its realized changes
     * from 3DEXPERIENCE using the Change Action physical ID.
     *
     * Example:
     * GET /change-actions/BA1C5B6D16E92A006A8EF53600009315
     */
    @GetMapping(
        value = "/{changeActionId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ChangeActionDto getChangeAction(
            @PathVariable String changeActionId) throws Exception {

        return changeActionService.getChangeAction(changeActionId);
    }
}