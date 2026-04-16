package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DomainUpdateReq extends DomainReq {

    private Long id;
}
