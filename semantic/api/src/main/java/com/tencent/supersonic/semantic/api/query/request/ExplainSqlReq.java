package com.tencent.supersonic.semantic.api.query.request;

import com.tencent.supersonic.semantic.api.model.enums.QueryTypeEnum;
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

    private QueryTypeEnum queryTypeEnum;

    private T queryReq;
}
