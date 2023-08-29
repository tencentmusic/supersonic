package com.tencent.supersonic.semantic.api.model.request;



import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;


@Data
public class DomainReq extends SchemaItem {

    private Long parentId = 0L;

    private Integer isOpen = 0;

    private List<String> viewers = new ArrayList<>();

    private List<String> viewOrgs = new ArrayList<>();

    private List<String> admins = new ArrayList<>();

    private List<String> adminOrgs = new ArrayList<>();
}
