package com.tencent.supersonic.chat.server.pojo;

import java.util.List;
import lombok.Data;


@Data
public class IATResult {

    private String state;

    
    private String errorCode;
    
    private String errorMessage;
    
    private List<FrameResult> body;
    

    @Data
    public static class FrameResult {
        private String ansStr;
    }

}
