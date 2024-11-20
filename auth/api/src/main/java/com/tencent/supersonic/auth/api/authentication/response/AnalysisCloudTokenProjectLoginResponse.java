package com.tencent.supersonic.auth.api.authentication.response;

import com.tencent.supersonic.auth.api.authentication.pojo.AnalysisCloudProject;
import lombok.Data;

/**
 * @program: easybi
 * @description: 分析云通过token和project登录返回
 * @author: roylin
 * @create: 2021-08-25 14:17
 **/

@Data
public class AnalysisCloudTokenProjectLoginResponse {

    String rspcode;
    String rspdesc;
    AnalysisCloudProject data;
}
