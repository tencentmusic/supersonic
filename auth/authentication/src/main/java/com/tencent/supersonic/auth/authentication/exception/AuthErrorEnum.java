package com.tencent.supersonic.auth.authentication.exception;

import com.tencent.supersonic.common.exception.IErrorCode;

/**
 * @program: easybi
 * @description:
 * @author: roylin
 * @create: 2021-08-19 17:07
 **/

public enum AuthErrorEnum implements IErrorCode {

    USER_NOT_FOUND(1, "用户不存在"),
    USER_REPEATE(2, "用户重复"),
    ERROR_PASSWORD(3, "密码不正确"),
    MISS_LOGIN_TYPE(4, "缺少登录类型"),
    LOGIN_TYPE_INVALIED(6, "登录类型不可用"),
    ANALYSIS_CLOUD_PASSWORD_ENCRYPT_FAILED(7, "分析云密码加密失败"),
    ANALYSIS_CLOUD_PASSWORD_LOGIN_FAILED(8, "分析云账密登录失败"),
    ANALYSIS_CLOUD_TOKEN_LOGIN_FAILED(9, "分析云TOKEN登录失败"),
    RED_SEA_USER_NOT_FOUND(10, "未找到红海用户"),
    PROJECT_NOT_FOUND(11, "未找到指定项目,请重新登陆"),
    PROJECT_OPERATOR_RELATION_NOT_FOUND(12, "未找到项目用户关系"),
    USER_PROJECT_RELATION_NOT_FOUND(13, "用户未找到归属项目,请重新登陆"),
    CAT_USER_NOT_FOUND(14, "大营销spms账号未在BI中找到"),
    EXTERNAL_USER_NOT_FOUND(15, "外部系统返回的SPMS账号在BI中未找到"),
    USER_SYNC_REPEAT(20, "分析云同步用户名在BI中已存在,但是id不同"),
    USER_SPMS_SYNC_REPEAT(21, "分析云同步SPMS账号在BI中已存在,但是id不同");


    private Integer code;
    private String message;


    AuthErrorEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
