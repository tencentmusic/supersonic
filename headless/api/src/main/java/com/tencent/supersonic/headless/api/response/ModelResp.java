package com.tencent.supersonic.headless.api.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.ModelDetail;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import lombok.Data;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@ToString(callSuper = true)
public class ModelResp extends SchemaItem {

    private Long domainId;

    private Long databaseId;

    private ModelDetail modelDetail;

    private String depends;

    private String sourceType;

    private String filterSql;

    private List<String> viewers = new ArrayList<>();

    private List<String> viewOrgs = new ArrayList<>();

    private List<String> admins = new ArrayList<>();

    private List<String> adminOrgs = new ArrayList<>();

    private Integer isOpen;

    private List<DrillDownDimension> drillDownDimensions;

    private String alias;

    private String fullPath;

    public boolean openToAll() {
        return isOpen != null && isOpen == 1;
    }

    public Identify getPrimaryIdentify() {
        if (modelDetail == null) {
            return null;
        }
        if (CollectionUtils.isEmpty(modelDetail.getIdentifiers())) {
            return null;
        }
        for (Identify identify : modelDetail.getIdentifiers()) {
            if (!CollectionUtils.isEmpty(identify.getEntityNames())) {
                return identify;
            }
        }
        return null;
    }

    public List<Dim> getTimeDimension() {
        if (modelDetail == null) {
            return Lists.newArrayList();
        }
        return modelDetail.filterTimeDims();
    }

    public Set<String> getFieldList() {
        Set<String> fieldSet = new HashSet<>();
        if (modelDetail == null) {
            return fieldSet;
        }
        if (!CollectionUtils.isEmpty(modelDetail.getIdentifiers())) {
            fieldSet.addAll(modelDetail.getIdentifiers().stream()
                    .map(Identify::getFieldName).collect(Collectors.toSet()));
        }
        if (!CollectionUtils.isEmpty(modelDetail.getDimensions())) {
            fieldSet.addAll(modelDetail.getDimensions().stream()
                    .map(Dim::getFieldName).collect(Collectors.toSet()));
        }
        if (!CollectionUtils.isEmpty(modelDetail.getMeasures())) {
            fieldSet.addAll(modelDetail.getMeasures().stream()
                    .map(Measure::getFieldName).collect(Collectors.toSet()));
        }
        return fieldSet;
    }

}
