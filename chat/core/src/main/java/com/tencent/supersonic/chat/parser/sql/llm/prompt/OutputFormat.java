package com.tencent.supersonic.chat.parser.sql.llm.prompt;

import com.tencent.supersonic.chat.parser.plugin.function.FunctionResp;
import com.tencent.supersonic.common.util.JsonUtil;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/***
 * output format
 */
@Slf4j
public class OutputFormat {

    public static final String PATTERN = "\\{[^{}]+\\}";

    public static String schemaLinkParse(String schemaLinkOutput) {
        try {
            schemaLinkOutput = schemaLinkOutput.trim();
            String pattern = "Schema_links:(.*)";
            Pattern regexPattern = Pattern.compile(pattern, Pattern.DOTALL);
            Matcher matcher = regexPattern.matcher(schemaLinkOutput);
            if (matcher.find()) {
                schemaLinkOutput = matcher.group(1).trim();
            } else {
                schemaLinkOutput = null;
            }
        } catch (Exception e) {
            log.error("", e);
            schemaLinkOutput = null;
        }
        return schemaLinkOutput;
    }

    public static FunctionResp functionCallParse(String llmOutput) {
        try {
            String[] findResult = llmOutput.split(PATTERN);
            String result = findResult[0].trim();

            Map<String, String> resultDict = JsonUtil.toMap(result, String.class, String.class);
            log.info("result:{},resultDict:{}", result, resultDict);

            String selection = resultDict.get("选择工具");
            FunctionResp resp = new FunctionResp();
            resp.setToolSelection(selection);
            return resp;
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }
}
