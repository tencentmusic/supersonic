package com.tencent.supersonic.auth.api.authentication.pojo;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class Organization {

    private String id;

    private String parentId;

    private String name;

    private String fullName;

    private List<Organization> subOrganizations = Lists.newArrayList();

    private boolean isRoot;

}
