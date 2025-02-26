package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class SqlEvaluation implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean isValidated;
    private String validateMsg;
}
