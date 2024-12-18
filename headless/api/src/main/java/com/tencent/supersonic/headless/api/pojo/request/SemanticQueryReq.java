package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.supersonic.headless.api.pojo.Cache;
import com.tencent.supersonic.headless.api.pojo.Param;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.*;

@Data
@Slf4j
public abstract class SemanticQueryReq {

    protected boolean needAuth = true;

    protected boolean innerLayerNative = false;

    protected Long dataSetId;

    protected String dataSetName;

    protected Set<Long> modelIds = new HashSet<>();

    protected List<Param> params = new ArrayList<>();

    protected Cache cacheInfo = new Cache();

    protected SqlInfo sqlInfo = new SqlInfo();

    private Long queryId;
    public void addModelId(Long modelId) {
        modelIds.add(modelId);
    }

    public String generateCommandMd5() {
        return DigestUtils.md5Hex(this.toCustomizedString());
    }

    public abstract String toCustomizedString();

    public List<Long> getModelIds() {
        return Lists.newArrayList(modelIds);
    }

    public Set<Long> getModelIdSet() {
        return modelIds;
    }
}
