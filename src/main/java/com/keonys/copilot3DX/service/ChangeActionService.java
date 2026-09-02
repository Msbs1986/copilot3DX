package com.keonys.copilot3DX.service;


import com.keonys.copilot3DX.dto.ChangeActionDto;
import org.springframework.stereotype.Service;

@Service
public class ChangeActionService {

    public ChangeActionDto search(String query) {

        return new ChangeActionDto(
                "CA-000123",
                "Test Change Action",
                "Released"
        );
    }
}