package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.headless.api.pojo.Term;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TermSetResp extends RecordInfo {

    private Long domainId;

    private List<Term> terms = Lists.newArrayList();

    public TermSetResp(Long domainId) {
        this.domainId = domainId;
    }
}
