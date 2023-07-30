package com.tencent.supersonic.chat.parser.function;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FunctionReq {

    private String queryText;

    private List<String> functionNames;

}
