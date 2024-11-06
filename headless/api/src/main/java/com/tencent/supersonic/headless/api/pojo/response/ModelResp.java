package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.Field;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.ModelDetail;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class ModelResp extends SchemaItem {

    private Long domainId;

    private Long databaseId;

    private Long tagObjectId;

    private ModelDetail modelDetail;

    private String depends;

    private String filterSql;

    private List<String> viewers = new ArrayList<>();

    private List<String> viewOrgs = new ArrayList<>();

    private List<String> admins = new ArrayList<>();

    private List<String> adminOrgs = new ArrayList<>();

    private Integer isOpen;

    private List<DrillDownDimension> drillDownDimensions = Lists.newArrayList();

    private String alias;

    private String fullPath;

    private Map<String, Object> ext;

    public boolean openToAll() {
        return isOpen != null && isOpen == 1;
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
        if (!CollectionUtils.isEmpty(modelDetail.getFields())) {
            fieldSet.addAll(modelDetail.getFields().stream().map(Field::getFieldName)
                    .collect(Collectors.toSet()));
        }
        return fieldSet;
    }

    public IdentifyType getIdentifyType(String fieldName) {
        List<Identify> identifiers = modelDetail.getIdentifiers();
        for (Identify identify : identifiers) {
            if (Objects.equals(identify.getFieldName(), fieldName)) {
                return IdentifyType.valueOf(identify.getType());
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), domainId);
    }
}
