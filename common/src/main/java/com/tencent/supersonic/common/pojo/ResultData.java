package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.pojo.enums.ReturnCode;
import com.tencent.supersonic.common.util.TraceIdUtil;
import lombok.Data;
import org.slf4j.MDC;

/***
 * result data
 */
@Data
public class ResultData<T> {
    private int code;
    private String msg;
    private T data;
    private long timestamp;
    private String traceId;

    public ResultData() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ResultData<T> success(T data) {
        ResultData<T> resultData = new ResultData<>();
        resultData.setCode(ReturnCode.SUCCESS.getCode());
        resultData.setMsg(ReturnCode.SUCCESS.getMessage());
        resultData.setData(data);
        resultData.setTraceId(MDC.get(TraceIdUtil.TRACE_ID));
        return resultData;
    }

    public static <T> ResultData<T> fail(int code, String message) {
        ResultData<T> resultData = new ResultData<>();
        resultData.setCode(code);
        resultData.setMsg(message);
        resultData.setTraceId(MDC.get(TraceIdUtil.TRACE_ID));
        return resultData;
    }

}
