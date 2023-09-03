package com.tencent.supersonic.semantic.api.query.pojo;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class Param {

    @NotBlank(message = "Invald parameter name")
    private String name;

    @NotBlank(message = "Invalid parameter value")
    private String value;

    public Param() {
    }

    public Param(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"")
                .append(name).append('\"');
        sb.append(",\"value\":\"")
                .append(value).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
