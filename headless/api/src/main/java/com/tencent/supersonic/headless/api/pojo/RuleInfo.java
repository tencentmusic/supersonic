package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.util.List;

@Data
public class RuleInfo {

    private Mode mode;
    private List<Object> parameters;

    public enum Mode {
        /** BEFORE, some days ago RECENT, the last few days EXIST, there was some information */
        BEFORE, RECENT, EXIST
    }
}
