package com.tencent.supersonic.chat.domain.pojo.semantic;

import java.util.List;
import lombok.Data;

@Data
public class TimeParseResult {

    private String text;
    private List<Integer> offset;
    private TimeParseDetail detail;
}
