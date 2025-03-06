package com.tencent.supersonic.chat.server.pojo;

import lombok.Data;

import java.util.List;

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
