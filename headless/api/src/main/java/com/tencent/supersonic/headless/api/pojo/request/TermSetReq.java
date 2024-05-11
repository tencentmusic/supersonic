package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.headless.api.pojo.Term;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class TermSetReq extends RecordInfo {

    @NotNull(message = "主题域ID不可为空")
    private Long domainId;

    private List<Term> terms = Lists.newArrayList();

}
