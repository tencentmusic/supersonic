package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class MetricDefineByFieldParams extends MetricDefineParams {

    private List<FieldParam> fields = Lists.newArrayList();
}
