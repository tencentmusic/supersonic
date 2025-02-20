package com.tencent.supersonic.headless.core.translator;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.config.ParameterConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("HeadlessTranslatorConfig")
@Slf4j
public class TranslatorConfig extends ParameterConfig {

    public static final Parameter TRANSLATOR_RESULT_LIMIT =
            new Parameter("s2.query-optimizer.resultLimit", "1000", "查询最大返回数据行数",
                    "为了前端展示性能考虑，请不要设置过大", "number", "语义翻译配置");

    @Override
    public List<Parameter> getSysParameters() {
        return Lists.newArrayList(TRANSLATOR_RESULT_LIMIT);
    }

}
