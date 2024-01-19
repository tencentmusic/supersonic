package com.tencent.supersonic.headless.api.request;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.Cache;
import com.tencent.supersonic.headless.api.pojo.Param;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;


@Data
@Slf4j
public abstract class SemanticQueryReq {

    protected Set<Long> modelIds;
    protected List<Param> params = new ArrayList<>();

    protected Cache cacheInfo = new Cache();

    public void setModelId(Long modelId) {
        modelIds = new HashSet<>();
        modelIds.add(modelId);
    }

    public String generateCommandMd5() {
        return DigestUtils.md5Hex(this.toCustomizedString());
    }

    public abstract String toCustomizedString();

    public List<Long> getModelIds() {
        return Lists.newArrayList(modelIds);
    }

    public String getModelIdStr() {
        return String.join(",", modelIds.stream().map(Object::toString).collect(Collectors.toList()));
    }

    public Set<Long> getModelIdSet() {
        return modelIds;
    }

}
