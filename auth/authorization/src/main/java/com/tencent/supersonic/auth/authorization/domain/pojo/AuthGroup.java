package com.tencent.supersonic.auth.authorization.domain.pojo;

import java.util.List;
import lombok.Data;

@Data
public class AuthGroup {

    private String domainId;
    private String name;
    private Integer groupId;
    private List<AuthRule> authRules;
    /**
     * row permission expression
     */
    private List<String> dimensionFilters;
    /**
     * row permission expression description information
     */
    private String dimensionFilterDescription;

    private List<String> authorizedUsers;
    /**
     * authorization Department Id
     */
    private List<String> authorizedDepartmentIds;
}
