package com.tencent.supersonic.headless.api.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Param {

    @NotBlank(message = "Invald parameter name")
    private String name;

    @NotNull(message = "Invalid parameter value")
    private String value;

    public Param() {}

    public Param(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
