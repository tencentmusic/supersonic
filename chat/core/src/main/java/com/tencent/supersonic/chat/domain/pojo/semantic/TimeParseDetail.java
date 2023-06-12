package com.tencent.supersonic.chat.domain.pojo.semantic;


import java.util.List;
import lombok.Data;

@Data
public class TimeParseDetail {

    private String type;
    private List<String> time;
}
