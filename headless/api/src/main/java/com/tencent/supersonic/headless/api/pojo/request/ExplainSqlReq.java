package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.enums.QueryMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExplainSqlReq<T> {

    private QueryMethod queryTypeEnum;

    private T queryReq;
}
