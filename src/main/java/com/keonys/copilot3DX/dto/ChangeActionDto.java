package com.keonys.copilot3DX.dto;

public class ChangeActionDto {

    private String name;
    private String title;
    private String state;

    public ChangeActionDto() {
    }

    public ChangeActionDto(String name,
                           String title,
                           String state) {
        this.name = name;
        this.title = title;
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}