package com.tencent.supersonic.auth.api.authorization.pojo;

import lombok.Data;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

@Data
public class AuthRule {

    private String name;
    private String description;
    private List<String> metrics;
    private List<String> dimensions;

    @Transient
    public List<String> resourceNames() {
        ArrayList<String> res = new ArrayList<>();
        if (metrics != null) {
            res.addAll(metrics);
        }

        if (dimensions != null) {
            res.addAll(dimensions);
        }
        return res;
    }
}
