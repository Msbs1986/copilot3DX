package com.keonys.copilot3DX.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeActionSummary {

    private String identifier;
    private String relativePath;

}