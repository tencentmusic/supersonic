package com.tencent.supersonic.chat.domain.pojo.semantic;


import lombok.Data;

@Data
public class ParserSvrResponse<T> {

    private String code;
    private String msg;
    private T data;
}
