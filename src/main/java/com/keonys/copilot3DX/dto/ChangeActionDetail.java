package com.keonys.copilot3DX.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeActionDetail {

    private String id;
    private String name;
    private String title;
    private String state;
    private String owner;
    private String originated;

}