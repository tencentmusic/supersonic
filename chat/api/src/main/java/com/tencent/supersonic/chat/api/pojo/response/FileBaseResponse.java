package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

@Data
public class FileBaseResponse<T> {
    /**
     * 状态: OK - 正常 FORBIDDEN - 禁止 ERROR - 错误 EXCEPTION - 异常
     */
    private String state;

    /**
     * 错误代号，当state不为OK时返回
     */
    private String errorCode;

    /**
     * 错误信息，当state不为OK时返回
     */
    private String errorMessage;

    /**
     * 接口响应结果，当state为OK时返回
     */
    private T body;
}
